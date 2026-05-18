package io.github.canjiemo.momo.system.service;

import io.github.canjiemo.momo.boot.test.AbstractIntegrationTest;
import io.github.canjiemo.momo.system.dto.SysConfigCreateRequest;
import io.github.canjiemo.momo.system.dto.SysConfigDTO;
import io.github.canjiemo.momo.system.dto.SysConfigQueryParam;
import io.github.canjiemo.momo.system.dto.SysConfigUpdateRequest;
import io.github.canjiemo.mycommon.exception.BusinessException;
import io.github.canjiemo.mycommon.pager.Pager;
import io.github.canjiemo.mycommon.pager.PagerHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 系统配置 CRUD 集成测试。
 * <p>
 * 注意：SysConfigService.update 用 {@code if (... != null)} 保护每个 setter，
 * 属于"局部更新"语义，故 null 字段保持原值 —— 这里不写 update null 回归，
 * 而是验证"局部更新不会误清现有字段"。
 */
class SysConfigServiceTest extends AbstractIntegrationTest {

    @Autowired
    private SysConfigService service;

    @Test
    @DisplayName("create: 创建配置成功，可按 key 查回")
    void create_success() {
        SysConfigCreateRequest req = baseCreate("test.cfg.k1", "测试键1", "value1", "测试");
        service.create(req);

        SysConfigDTO dto = service.getByKey("test.cfg.k1");
        assertEquals("value1", dto.getConfigValue());
        assertEquals("测试键1", dto.getConfigName());
        assertEquals(2, dto.getConfigType(), "service.create 写死 configType=2（用户配置）");
    }

    @Test
    @DisplayName("create: configKey 重复抛业务异常")
    void create_duplicateKeyThrows() {
        service.create(baseCreate("test.cfg.dup", "名1", "v1", null));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(baseCreate("test.cfg.dup", "名2", "v2", null)));
        assertTrue(ex.getMessage().contains("配置键已存在"));
    }

    @Test
    @DisplayName("update: 修改 value / name / remark 成功")
    void update_basicFields() {
        service.create(baseCreate("test.cfg.upd", "原名", "原值", "原备注"));
        Long id = service.getByKey("test.cfg.upd").getId();

        SysConfigUpdateRequest req = new SysConfigUpdateRequest();
        req.setId(id);
        req.setConfigValue("新值");
        req.setConfigName("新名");
        req.setRemark("新备注");
        service.update(req);

        SysConfigDTO after = service.getByKey("test.cfg.upd");
        assertEquals("新值", after.getConfigValue());
        assertEquals("新名", after.getConfigName());
        assertEquals("新备注", after.getRemark());
    }

    @Test
    @DisplayName("update[语义]: null 字段不更新（局部更新语义，保持原值）")
    void update_nullFieldsPreserveExisting() {
        service.create(baseCreate("test.cfg.partial", "原名", "原值", "原备注"));
        Long id = service.getByKey("test.cfg.partial").getId();

        SysConfigUpdateRequest req = new SysConfigUpdateRequest();
        req.setId(id);
        req.setConfigValue("仅改值");
        // configName / remark 留 null
        service.update(req);

        SysConfigDTO after = service.getByKey("test.cfg.partial");
        assertEquals("仅改值", after.getConfigValue());
        assertEquals("原名", after.getConfigName(), "configName 传 null 时保持原值（局部更新）");
        assertEquals("原备注", after.getRemark(), "remark 传 null 时保持原值");
    }

    @Test
    @DisplayName("update: 主键不存在抛业务异常")
    void update_nonExistentIdThrows() {
        SysConfigUpdateRequest req = new SysConfigUpdateRequest();
        req.setId(999_999_999L);
        req.setConfigValue("x");
        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(req));
        assertTrue(ex.getMessage().contains("配置项不存在"));
    }

    @Test
    @DisplayName("delete: 用户配置（configType=2）可以删除")
    void delete_userConfigSuccess() {
        service.create(baseCreate("test.cfg.del", "可删", "v", null));
        Long id = service.getByKey("test.cfg.del").getId();

        service.delete(id);

        assertThrows(BusinessException.class, () -> service.getByKey("test.cfg.del"));
    }

    @Test
    @DisplayName("delete: 主键不存在抛业务异常")
    void delete_nonExistentIdThrows() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(999_999_999L));
        assertTrue(ex.getMessage().contains("配置项不存在"));
    }

    @Test
    @DisplayName("getValue: 命中返回值，未命中返回 null")
    void getValue() {
        service.create(baseCreate("test.cfg.val", "取值", "the_value", null));

        assertEquals("the_value", service.getValue("test.cfg.val"));
        assertNull(service.getValue("test.cfg.not.exist"));
    }

    @Test
    @DisplayName("search: 按 configKey 模糊查询")
    void search_byKeyLike() {
        service.create(baseCreate("test.search.a", "A", "va", null));
        service.create(baseCreate("test.search.b", "B", "vb", null));

        SysConfigQueryParam param = new SysConfigQueryParam();
        param.setConfigKey("test.search");
        param.setPageNum(1);
        param.setPageSize(10);
        Pager<SysConfigDTO> result = service.search(param, PagerHandler.createPager(param));

        assertEquals(2, result.getPageData().size());
        assertTrue(result.getPageData().stream().allMatch(c -> c.getConfigKey().startsWith("test.search")));
    }

    private static SysConfigCreateRequest baseCreate(String key, String name, String value, String remark) {
        SysConfigCreateRequest req = new SysConfigCreateRequest();
        req.setConfigKey(key);
        req.setConfigName(name);
        req.setConfigValue(value);
        req.setRemark(remark);
        return req;
    }
}
