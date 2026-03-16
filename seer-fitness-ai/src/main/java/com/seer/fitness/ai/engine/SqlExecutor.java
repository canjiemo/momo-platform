package com.seer.fitness.ai.engine;

import io.github.canjiemo.base.myjdbc.dao.IBaseDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SqlExecutor {

    private static final int MAX_ROWS = 1000;

    @Autowired
    private IBaseDao baseDao;

    /**
     * 通过 myjdbc 执行 SQL，自动注入租户隔离 + delete_flag=0
     * 使用 Map.class 接收动态结果，无需 DTO
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> execute(String sql) {
        // 追加行数限制，防止超大结果集
        String limitedSql = sql.stripTrailing().replaceAll(";$", "")
                + " LIMIT " + MAX_ROWS;
        List<Map<String, Object>> results = (List<Map<String, Object>>)
                (List<?>) baseDao.queryListForSql(limitedSql, Map.of(), Map.class);
        log.info("SQL 执行完成，返回 {} 行", results.size());
        return results;
    }
}
