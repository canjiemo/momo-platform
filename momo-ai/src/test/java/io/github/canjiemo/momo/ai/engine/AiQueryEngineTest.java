package io.github.canjiemo.momo.ai.engine;

import io.github.canjiemo.momo.ai.conversation.entity.AiConversation;
import io.github.canjiemo.momo.ai.engine.dto.AiQueryRequest;
import io.github.canjiemo.momo.ai.engine.dto.AiQueryResponse;
import io.github.canjiemo.mycommon.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AiQueryEngine 路由逻辑单元测试
 * <p>
 * 验证三条执行路径：
 * 1. Schema 检索无结果（NotRelatedToDataException）→ 返回引导提示，不调用 LLM
 * 2. LLM 返回 NOT_A_QUERY → 返回友好提示，不执行 SQL
 * 3. 正常流程 → 依次调用 Schema/LLM/Validator/Executor/Formatter
 */
@ExtendWith(MockitoExtension.class)
class AiQueryEngineTest {

    @Mock private SchemaContextBuilder schemaContextBuilder;
    @Mock private SqlGenerator        sqlGenerator;
    @Mock private SqlValidator        sqlValidator;
    @Mock private SqlExecutor         sqlExecutor;
    @Mock private ResultFormatter     resultFormatter;

    @InjectMocks
    private AiQueryEngine engine;

    private AiQueryRequest request;

    @BeforeEach
    void setUp() {
        request = new AiQueryRequest();
        request.setSessionId("sess-001");
        request.setQuestion("查询本月新增用户数");
    }

    // -----------------------------------------------------------------------
    // 路径 1：向量库为空 → 返回同步提示，不打 LLM
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("向量库为空 -> 返回管理员提示，不调用 LLM")
    void schema_vectorEmpty_returnsAdminHint() {
        when(schemaContextBuilder.build(anyString()))
                .thenThrow(new NotRelatedToDataException(true));

        AiQueryResponse resp = engine.query(request, Collections.emptyList());

        assertNotNull(resp);
        assertTrue(resp.getSummary().contains("管理员"));
        assertNull(resp.getGeneratedSql());
        verifyNoInteractions(sqlGenerator, sqlValidator, sqlExecutor, resultFormatter);
    }

    @Test
    @DisplayName("问题与数据无关（低相似度）-> 返回查询引导提示")
    void schema_notRelated_returnsQueryHint() {
        when(schemaContextBuilder.build(anyString()))
                .thenThrow(new NotRelatedToDataException(false));

        AiQueryResponse resp = engine.query(request, Collections.emptyList());

        assertNotNull(resp);
        assertTrue(resp.getSummary().contains("查询"));
        verifyNoInteractions(sqlGenerator, sqlValidator, sqlExecutor, resultFormatter);
    }

    // -----------------------------------------------------------------------
    // 路径 2：LLM 判断不是数据查询 → NOT_A_QUERY
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("LLM 返回 NOT_A_QUERY → 跳过 SQL 执行，返回助手引导提示")
    void sqlGenerator_notAQuery_skipsExecution() {
        when(schemaContextBuilder.build(anyString())).thenReturn("schema context");
        when(sqlGenerator.generate(anyString(), anyString(), anyList()))
                .thenReturn("NOT_A_QUERY");

        AiQueryResponse resp = engine.query(request, Collections.emptyList());

        assertNotNull(resp);
        assertTrue(resp.getSummary().contains("数据查询助手"));
        verifyNoInteractions(sqlValidator, sqlExecutor, resultFormatter);
    }

    // -----------------------------------------------------------------------
    // 路径 3：正常流程
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("正常流程 → 依次调用全部 5 个组件，返回 formatter 的结果")
    void normalFlow_allComponentsCalled() {
        String schema = "表名: sys_user ...";
        String sql    = "SELECT COUNT(*) FROM sys_user";

        AiQueryResponse expected = new AiQueryResponse();
        expected.setSessionId("sess-001");
        expected.setSummary("本月共新增 42 名用户。");
        expected.setGeneratedSql(sql);

        when(schemaContextBuilder.build(anyString())).thenReturn(schema);
        when(sqlGenerator.generate(eq(schema), anyString(), anyList())).thenReturn(sql);
        doNothing().when(sqlValidator).validate(sql);
        when(sqlExecutor.execute(sql)).thenReturn(List.of(Map.of("count", 42)));
        when(resultFormatter.format(anyString(), eq(sql), anyList())).thenReturn(expected);

        AiQueryResponse resp = engine.query(request, Collections.emptyList());

        assertSame(expected, resp);
        assertEquals("sess-001", resp.getSessionId());
        verify(sqlValidator).validate(sql);
        verify(sqlExecutor).execute(sql);
        verify(resultFormatter).format(anyString(), eq(sql), anyList());
    }

    @Test
    @DisplayName("携带对话历史 → history 被传递给 sqlGenerator")
    void normalFlow_historyPassedToGenerator() {
        when(schemaContextBuilder.build(anyString())).thenReturn("schema");
        when(sqlGenerator.generate(anyString(), anyString(), anyList())).thenReturn("NOT_A_QUERY");

        List<AiConversation> history = List.of(); // 空历史也行，只验证传参
        engine.query(request, history);

        verify(sqlGenerator).generate(anyString(), anyString(), eq(history));
    }

    @Test
    @DisplayName("SqlValidator 抛异常 → BusinessException 向上传播")
    void validatorThrows_propagates() {
        when(schemaContextBuilder.build(anyString())).thenReturn("schema");
        when(sqlGenerator.generate(anyString(), anyString(), anyList()))
                .thenReturn("DROP TABLE sys_user");
        doThrow(new BusinessException("仅支持查询操作"))
                .when(sqlValidator).validate(anyString());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> engine.query(request, Collections.emptyList()));
        assertTrue(ex.getMessage().contains("查询"));
        verifyNoInteractions(sqlExecutor, resultFormatter);
    }

    // -----------------------------------------------------------------------
    // 参数校验
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("question 为 null → 抛出 BusinessException")
    void nullQuestion_throws() {
        request.setQuestion(null);
        assertThrows(BusinessException.class, () -> engine.query(request, Collections.emptyList()));
        verifyNoInteractions(schemaContextBuilder);
    }

    @Test
    @DisplayName("question 超过 500 字符 → 抛出 BusinessException")
    void tooLongQuestion_throws() {
        request.setQuestion("x".repeat(501));
        assertThrows(BusinessException.class, () -> engine.query(request, Collections.emptyList()));
        verifyNoInteractions(schemaContextBuilder);
    }
}
