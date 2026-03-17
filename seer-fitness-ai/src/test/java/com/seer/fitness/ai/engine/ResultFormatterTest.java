package com.seer.fitness.ai.engine;

import com.seer.fitness.ai.engine.dto.AiQueryResponse;
import com.seer.fitness.ai.provider.AiProviderManager;
import com.seer.fitness.ai.provider.IAiChatProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * ResultFormatter 单元测试
 * <p>
 * 重点验证：
 * 1. 空结果集 → 固定文案，不调用 LLM
 * 2. 图表类型推断规则（line / bar / pie / none）
 * 3. LLM 生成摘要失败 → 降级到规则文案
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResultFormatterTest {

    @Mock private AiProviderManager providerManager;
    @Mock private IAiChatProvider   chatProvider;

    @InjectMocks
    private ResultFormatter formatter;

    @BeforeEach
    void setUp() {
        when(providerManager.getActiveChat()).thenReturn(chatProvider);
        when(chatProvider.chat(anyString(), anyString())).thenReturn("摘要文本");
    }

    // -----------------------------------------------------------------------
    // 空结果
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("空结果集 → 返回固定提示，不调用 LLM")
    void emptyRows_fixedMessage() {
        AiQueryResponse resp = formatter.format("查询用户数", "SELECT COUNT(*)", List.of());

        assertEquals("未查到符合条件的数据。", resp.getSummary());
        assertNull(resp.getTable());
        assertNull(resp.getChart());
        verify(chatProvider, never()).chat(anyString(), anyString());
    }

    // -----------------------------------------------------------------------
    // 表格数据构建
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("有结果 → 表格列和行数量正确")
    void hasRows_tableBuilt() {
        List<Map<String, Object>> rows = List.of(
                row("username", "alice", "count", 5L),
                row("username", "bob",   "count", 3L)
        );

        AiQueryResponse resp = formatter.format("查询用户", "SELECT ...", rows);

        assertNotNull(resp.getTable());
        assertEquals(2, resp.getTable().getColumns().size());
        assertEquals(2, resp.getTable().getRows().size());
    }

    // -----------------------------------------------------------------------
    // 图表类型推断
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("第一列含时间关键字 + 第二列为数值 → line 图")
    void chartType_line_timeColumn() {
        List<Map<String, Object>> rows = List.of(
                row("month", "2024-01", "count", 10L),
                row("month", "2024-02", "count", 20L)
        );
        AiQueryResponse resp = formatter.format("按月统计", "SELECT ...", rows);

        assertNotNull(resp.getChart());
        assertEquals("line", resp.getChart().getType());
        assertEquals("month", resp.getChart().getXField());
        assertEquals("count", resp.getChart().getYField());
    }

    @Test
    @DisplayName("第一列含分组关键字 + 第二列为数值 → bar 图")
    void chartType_bar_groupColumn() {
        List<Map<String, Object>> rows = List.of(
                row("role", "teacher", "count", 5L),
                row("role", "student", "count", 50L)
        );
        AiQueryResponse resp = formatter.format("按角色统计", "SELECT ...", rows);

        assertNotNull(resp.getChart());
        assertEquals("bar", resp.getChart().getType());
    }

    @Test
    @DisplayName("2 列数值型 + 行数 ≤ 10 → pie 图")
    void chartType_pie_twoColumnSmallSet() {
        List<Map<String, Object>> rows = List.of(
                row("dept", "体育", "ratio", 40L),
                row("dept", "数学", "ratio", 30L),
                row("dept", "语文", "ratio", 30L)
        );
        AiQueryResponse resp = formatter.format("部门占比", "SELECT ...", rows);

        assertNotNull(resp.getChart());
        assertEquals("pie", resp.getChart().getType());
    }

    @Test
    @DisplayName("第二列非数值 → none（不生成图表）")
    void chartType_none_nonNumericSecondColumn() {
        List<Map<String, Object>> rows = List.of(
                row("username", "alice", "email", "alice@example.com"),
                row("username", "bob",   "email", "bob@example.com")
        );
        AiQueryResponse resp = formatter.format("查询用户邮箱", "SELECT ...", rows);

        assertNull(resp.getChart());
    }

    @Test
    @DisplayName("单列结果 → none（不生成图表）")
    void chartType_none_singleColumn() {
        List<Map<String, Object>> rows = List.of(
                Map.of("count", 42L)
        );
        AiQueryResponse resp = formatter.format("查总数", "SELECT ...", rows);

        assertNull(resp.getChart());
    }

    @Test
    @DisplayName("time 列命名 + 行数 > 10 → 仍然是 line（行数不影响时间图）")
    void chartType_line_manyRows() {
        List<Map<String, Object>> rows = new java.util.ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            rows.add(row("date", "2024-0" + i, "count", (long) i));
        }
        AiQueryResponse resp = formatter.format("按日期统计", "SELECT ...", rows);

        assertNotNull(resp.getChart());
        assertEquals("line", resp.getChart().getType());
    }

    // -----------------------------------------------------------------------
    // LLM 摘要降级
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("LLM 生成摘要抛异常 → 降级为规则摘要（含数据条数）")
    void llmFails_fallbackSummary() {
        when(chatProvider.chat(anyString(), anyString()))
                .thenThrow(new RuntimeException("LLM timeout"));

        List<Map<String, Object>> rows = List.of(row("name", "alice", "count", 5L));
        AiQueryResponse resp = formatter.format("查询用户", "SELECT ...", rows);

        assertNotNull(resp.getSummary());
        assertTrue(resp.getSummary().contains("1"), "降级摘要应含数据条数");
    }

    // -----------------------------------------------------------------------
    // 工具方法
    // -----------------------------------------------------------------------

    /** 按顺序构造有序 Map（保证列顺序符合预期） */
    private Map<String, Object> row(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }
}
