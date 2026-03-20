package io.github.canjiemo.momo.ai.engine;

import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import io.github.canjiemo.momo.ai.catalog.entity.AiTableCatalog;
import io.github.canjiemo.mycommon.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SqlValidator extends BaseServiceImpl {

    /**
     * 校验规则：
     * 1. 必须是 SELECT 语句
     * 2. 只能查询数据目录中启用的表
     * 预留扩展点：后续可注入 SqlValidatorPlugin 列表
     */
    public void validate(String sql) {
        // 拒绝多语句 SQL（防止注入：SELECT 1; DROP TABLE ... 模式）
        if (sql.contains(";")) {
            String stripped = sql.stripTrailing().replaceAll(";$", "");
            if (stripped.contains(";")) {
                throw new BusinessException("不支持多语句 SQL");
            }
        }

        Statement stmt;
        try {
            stmt = CCJSqlParserUtil.parse(sql);
        } catch (Exception e) {
            throw new BusinessException("SQL 解析失败，请重新描述您的问题");
        }

        // 1. 只允许 SELECT
        if (!(stmt instanceof Select)) {
            throw new BusinessException("仅支持查询操作，不允许修改数据");
        }

        // 2. 白名单校验：只能查询开放的表
        Set<String> allowedTables = lambdaQuery(AiTableCatalog.class)
                .eq(AiTableCatalog::getIsEnabled, 1)
                .list()
                .stream()
                .map(t -> t.getTableName().toLowerCase())
                .collect(Collectors.toSet());

        TablesNamesFinder finder = new TablesNamesFinder();
        List<String> usedTables = finder.getTableList(stmt);
        for (String table : usedTables) {
            if (!allowedTables.contains(table.toLowerCase())) {
                throw new BusinessException("查询包含未开放的数据表: " + table);
            }
        }
    }
}
