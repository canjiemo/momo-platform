package io.github.canjiemo.momo.file.service;

import io.github.canjiemo.momo.boot.test.AbstractIntegrationTest;
import io.github.canjiemo.momo.file.dto.SysFileConfigCreateRequest;
import io.github.canjiemo.momo.file.dto.SysFileConfigDTO;
import io.github.canjiemo.momo.file.dto.SysFileConfigUpdateRequest;
import io.github.canjiemo.momo.file.entity.SysFileConfig;
import io.github.canjiemo.mycommon.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件存储配置 CRUD 集成测试。
 * <p>
 * SysFileConfigService.update 用 {@code if (... != null)} 局部更新，故验证
 * "局部更新不会误清现有字段"语义。
 * <p>
 * 注意：V2 init data 预置了 local + minio 两条配置，其中 local 已激活。测试中查询/激活
 * 切换都会影响这两条数据，但 {@code @Transactional} 回滚保证不污染。
 */
class SysFileConfigServiceTest extends AbstractIntegrationTest {

    @Autowired
    private SysFileConfigService service;

    // ============================================================
    // create
    // ============================================================

    @Test
    @DisplayName("create: 新建配置默认 isActive=0")
    void create_success() {
        SysFileConfigCreateRequest req = baseCreate("测试配置", "local", "{\"basePath\":\"./test\"}", "test remark");
        service.create(req);

        SysFileConfigDTO got = findByName("测试配置");
        assertNotNull(got);
        assertEquals("local", got.getStorageType());
        assertEquals(0, got.getIsActive(), "新建默认 isActive=0");
        assertEquals("test remark", got.getRemark());
    }

    // ============================================================
    // update
    // ============================================================

    @Test
    @DisplayName("update: 修改 configName / config / remark 成功")
    void update_basicFields() {
        service.create(baseCreate("UpdCfg", "local", "{\"basePath\":\"./old\"}", "旧"));
        Long id = findByName("UpdCfg").getId();

        SysFileConfigUpdateRequest req = new SysFileConfigUpdateRequest();
        req.setId(id);
        req.setConfigName("UpdCfgNew");
        req.setConfig("{\"basePath\":\"./new\"}");
        req.setRemark("新");
        service.update(req);

        SysFileConfigDTO after = service.list().stream()
                .filter(c -> id.equals(c.getId())).findFirst().orElseThrow();
        assertEquals("UpdCfgNew", after.getConfigName());
        // sys_file_config.config 是 JSONB 列，PG 会自动规范化空格，不做严格字符串比较
        assertTrue(after.getConfig().contains("./new"), "config 应包含新值，实际值: " + after.getConfig());
        assertEquals("新", after.getRemark());
    }

    @Test
    @DisplayName("update[语义]: null 字段不更新（局部更新语义，保持原值）")
    void update_nullFieldsPreserveExisting() {
        service.create(baseCreate("PartialCfg", "local", "{\"original\":true}", "原备注"));
        Long id = findByName("PartialCfg").getId();

        SysFileConfigUpdateRequest req = new SysFileConfigUpdateRequest();
        req.setId(id);
        req.setConfigName("仅改名");
        // config / remark 留 null
        service.update(req);

        SysFileConfigDTO after = service.list().stream()
                .filter(c -> id.equals(c.getId())).findFirst().orElseThrow();
        assertEquals("仅改名", after.getConfigName());
        // sys_file_config.config 是 JSONB 列，PG 会自动规范化空格 ——
        // 这里关注语义"原内容保留"，用 contains 而非严格字符串比较
        assertTrue(after.getConfig().contains("original") && after.getConfig().contains("true"),
                "config 传 null 保持原值，实际: " + after.getConfig());
        assertEquals("原备注", after.getRemark(), "remark 传 null 保持原值");
    }

    @Test
    @DisplayName("update: 主键不存在抛业务异常")
    void update_nonExistentIdThrows() {
        SysFileConfigUpdateRequest req = new SysFileConfigUpdateRequest();
        req.setId(999_999_999L);
        req.setConfigName("x");
        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(req));
        assertTrue(ex.getMessage().contains("配置不存在"));
    }

    // ============================================================
    // activate
    // ============================================================

    @Test
    @DisplayName("activate: 激活新配置时旧激活配置自动转为未激活（同时只能有一条 isActive=1）")
    void activate_exclusivity() {
        service.create(baseCreate("ActA", "local", "{\"k\":\"a\"}", null));
        service.create(baseCreate("ActB", "local", "{\"k\":\"b\"}", null));
        Long idA = findByName("ActA").getId();
        Long idB = findByName("ActB").getId();

        service.activate(idA);
        service.activate(idB);

        SysFileConfig active = service.getActiveConfig();
        assertNotNull(active);
        assertEquals(idB, active.getId(), "最后激活的应是 idB");
        long activeCount = service.list().stream()
                .filter(c -> Integer.valueOf(1).equals(c.getIsActive())).count();
        assertEquals(1, activeCount, "同时只能有一条 isActive=1（含 V2 预置数据 + 测试创建的）");
    }

    @Test
    @DisplayName("activate: 配置不存在抛业务异常")
    void activate_nonExistentThrows() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.activate(999_999_999L));
        assertTrue(ex.getMessage().contains("配置不存在"));
    }

    // ============================================================
    // delete
    // ============================================================

    @Test
    @DisplayName("delete: 未激活的配置可删除")
    void delete_inactiveConfigSuccess() {
        service.create(baseCreate("DelCfg", "local", "{\"k\":\"v\"}", null));
        Long id = findByName("DelCfg").getId();

        service.delete(id);

        boolean stillExists = service.list().stream().anyMatch(c -> id.equals(c.getId()));
        assertFalse(stillExists);
    }

    @Test
    @DisplayName("delete: 激活的配置不允许删除")
    void delete_activeConfigThrows() {
        service.create(baseCreate("ActiveDel", "local", "{\"k\":\"v\"}", null));
        Long id = findByName("ActiveDel").getId();
        service.activate(id);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(id));
        assertTrue(ex.getMessage().contains("激活的配置不允许删除"));
    }

    @Test
    @DisplayName("delete: 主键不存在抛业务异常")
    void delete_nonExistentIdThrows() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(999_999_999L));
        assertTrue(ex.getMessage().contains("配置不存在"));
    }

    // ============================================================
    // helpers
    // ============================================================

    private SysFileConfigDTO findByName(String name) {
        List<SysFileConfigDTO> all = service.list();
        return all.stream()
                .filter(c -> name.equals(c.getConfigName()))
                .findFirst().orElseThrow(() -> new AssertionError("找不到配置: " + name));
    }

    private static SysFileConfigCreateRequest baseCreate(String name, String type, String config, String remark) {
        SysFileConfigCreateRequest req = new SysFileConfigCreateRequest();
        req.setConfigName(name);
        req.setStorageType(type);
        req.setConfig(config);
        req.setRemark(remark);
        return req;
    }
}
