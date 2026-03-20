package io.github.canjiemo.momo.ai.engine;

import io.github.canjiemo.base.myjdbc.dao.IBaseDao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SqlExecutor 单元测试
 * <p>
 * 重点验证 LIMIT 注入规则：
 * - SQL 中无 LIMIT → 自动追加 LIMIT 1000
 * - SQL 中已有 LIMIT → 保持原样，不再追加
 * - 末尾分号 → 被清理后再执行
 */
@ExtendWith(MockitoExtension.class)
class SqlExecutorTest {

    @Mock
    private IBaseDao baseDao;

    @InjectMocks
    private SqlExecutor sqlExecutor;

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> mockEmptyResult() {
        return List.of();
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("SQL 无 LIMIT → 自动追加 LIMIT 1000")
    void noLimit_appendsLimit1000() {
        when(baseDao.queryListForSql(anyString(), anyMap(), eq(Map.class)))
                .thenReturn((List) mockEmptyResult());

        sqlExecutor.execute("SELECT * FROM sys_user");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(baseDao).queryListForSql(sqlCaptor.capture(), anyMap(), eq(Map.class));

        String executedSql = sqlCaptor.getValue();
        assertTrue(executedSql.toUpperCase().endsWith("LIMIT 1000"),
                "应追加 LIMIT 1000，实际: " + executedSql);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("SQL 已有 LIMIT → 不再追加，保留原 LIMIT")
    void hasLimit_notModified() {
        when(baseDao.queryListForSql(anyString(), anyMap(), eq(Map.class)))
                .thenReturn((List) mockEmptyResult());

        sqlExecutor.execute("SELECT * FROM sys_user LIMIT 50");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(baseDao).queryListForSql(sqlCaptor.capture(), anyMap(), eq(Map.class));

        String executedSql = sqlCaptor.getValue();
        // 只应有一个 LIMIT
        long limitCount = java.util.Arrays.stream(executedSql.toUpperCase().split("LIMIT"))
                .count() - 1;
        assertEquals(1L, limitCount, "不应重复追加 LIMIT，实际: " + executedSql);
        assertTrue(executedSql.contains("50"), "原 LIMIT 50 应被保留");
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("SQL 末尾带分号 → 分号被清理后执行")
    void trailingSemicolon_stripped() {
        when(baseDao.queryListForSql(anyString(), anyMap(), eq(Map.class)))
                .thenReturn((List) mockEmptyResult());

        sqlExecutor.execute("SELECT COUNT(*) FROM sys_user;");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(baseDao).queryListForSql(sqlCaptor.capture(), anyMap(), eq(Map.class));

        assertFalse(sqlCaptor.getValue().contains(";"),
                "分号应被清理，实际: " + sqlCaptor.getValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("执行成功 → 返回 baseDao 的结果列表")
    void execute_returnsRows() {
        List<Map<String, Object>> expected = List.of(Map.of("count", 42L));
        when(baseDao.queryListForSql(anyString(), anyMap(), eq(Map.class)))
                .thenReturn((List) expected);

        List<Map<String, Object>> result = sqlExecutor.execute("SELECT COUNT(*) FROM sys_user");

        assertEquals(1, result.size());
        assertEquals(42L, result.get(0).get("count"));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("LIMIT 大小写不敏感匹配 → 已有 'limit' 小写时也不重复追加")
    void limitCaseInsensitive_notDuplicated() {
        when(baseDao.queryListForSql(anyString(), anyMap(), eq(Map.class)))
                .thenReturn((List) mockEmptyResult());

        sqlExecutor.execute("SELECT * FROM sys_user limit 20");

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(baseDao).queryListForSql(sqlCaptor.capture(), anyMap(), eq(Map.class));

        // 不应再追加 LIMIT 1000
        assertFalse(sqlCaptor.getValue().toUpperCase().endsWith("LIMIT 1000"),
                "已有 limit 时不应再追加 LIMIT 1000，实际: " + sqlCaptor.getValue());
    }
}
