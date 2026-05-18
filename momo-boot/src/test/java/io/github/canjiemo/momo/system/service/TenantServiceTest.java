package io.github.canjiemo.momo.system.service;

import io.github.canjiemo.momo.boot.test.AbstractIntegrationTest;
import io.github.canjiemo.momo.system.dto.TenantCreateRequest;
import io.github.canjiemo.momo.system.dto.TenantDTO;
import io.github.canjiemo.momo.system.dto.TenantQueryParam;
import io.github.canjiemo.momo.system.dto.TenantUpdateRequest;
import io.github.canjiemo.mycommon.exception.BusinessException;
import io.github.canjiemo.mycommon.pager.Pager;
import io.github.canjiemo.mycommon.pager.PagerHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 租户 CRUD 集成测试。
 * <p>
 * 注意：TenantService.update 全部使用 {@code if (StringUtils.hasText(...))} / {@code if (... != null)}
 * 保护每个 setter，属于"局部更新"语义，故不写 update null 回归，而是验证
 * "null/空字符串字段保持原值"。
 */
class TenantServiceTest extends AbstractIntegrationTest {

    @Autowired
    private TenantService service;

    // ============================================================
    // create
    // ============================================================

    @Test
    @DisplayName("create: 创建租户成功，状态默认 ACTIVE(1)，activatedAt 自动设置")
    void create_success() {
        service.create(baseCreate("test_t1", "TestTenant1"));

        TenantDTO got = service.getByCode("test_t1");
        assertNotNull(got);
        assertEquals("TestTenant1", got.getTenantName());
        assertEquals(1, got.getStatus(), "新建默认 ACTIVE 状态码 = 1");
        assertNotNull(got.getActivatedAt(), "新建应自动填充 activatedAt");
        assertEquals(1000, got.getMaxUsers(), "未指定时默认 1000");
    }

    @Test
    @DisplayName("create: tenantCode 重复抛业务异常")
    void create_duplicateCodeThrows() {
        service.create(baseCreate("test_dup_t", "Dup1"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(baseCreate("test_dup_t", "Dup2")));
        assertTrue(ex.getMessage().contains("租户编码已存在"));
    }

    @Test
    @DisplayName("create: expiredAt 格式不正确抛业务异常")
    void create_invalidExpiredAtThrows() {
        TenantCreateRequest req = baseCreate("test_t_bad_exp", "BadExp");
        req.setExpiredAt("2026-12-31");  // 缺时分秒
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req));
        assertTrue(ex.getMessage().contains("过期时间格式"));
    }

    // ============================================================
    // update
    // ============================================================

    @Test
    @DisplayName("update: 修改 contactPhone / email / address 等成功")
    void update_basicFields() {
        service.create(baseCreate("test_upd_t", "UpdT"));
        Long id = service.getByCode("test_upd_t").getId();

        TenantUpdateRequest req = new TenantUpdateRequest();
        req.setId(id);
        req.setContactPhone("13800000001");
        req.setContactEmail("new@example.com");
        req.setAddress("新地址");
        req.setMaxUsers(500);
        service.update(req);

        TenantDTO after = service.getById(id);
        assertEquals("13800000001", after.getContactPhone());
        assertEquals("new@example.com", after.getContactEmail());
        assertEquals("新地址", after.getAddress());
        assertEquals(500, after.getMaxUsers());
    }

    @Test
    @DisplayName("update[语义]: null 字段不更新（局部更新语义，保持原值）")
    void update_nullFieldsPreserveExisting() {
        TenantCreateRequest c = baseCreate("test_partial_t", "PartialT");
        c.setContactPhone("13900000000");
        c.setContactEmail("orig@example.com");
        service.create(c);
        Long id = service.getByCode("test_partial_t").getId();

        TenantUpdateRequest req = new TenantUpdateRequest();
        req.setId(id);
        req.setContactPhone("13900000099");
        // contactEmail / address / description / maxUsers 留 null
        service.update(req);

        TenantDTO after = service.getById(id);
        assertEquals("13900000099", after.getContactPhone());
        assertEquals("orig@example.com", after.getContactEmail(), "传 null 时 contactEmail 保持原值");
    }

    @Test
    @DisplayName("update: 主键不存在抛业务异常")
    void update_nonExistentIdThrows() {
        TenantUpdateRequest req = new TenantUpdateRequest();
        req.setId(999_999_999L);
        req.setRealName("x");
        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(req));
        assertTrue(ex.getMessage().contains("租户不存在"));
    }

    // ============================================================
    // enable / disable
    // ============================================================

    @Test
    @DisplayName("disable: 启用态 → 禁用态，再 disable 抛异常")
    void disable_toggleStatus() {
        service.create(baseCreate("test_disable_t", "DisT"));
        Long id = service.getByCode("test_disable_t").getId();
        assertEquals(1, service.getById(id).getStatus());

        service.disable(id);
        assertEquals(2, service.getById(id).getStatus(), "DISABLED 状态码 = 2");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.disable(id));
        assertTrue(ex.getMessage().contains("已是禁用状态"));
    }

    @Test
    @DisplayName("enable: 禁用态 → 启用态")
    void enable_fromDisabled() {
        service.create(baseCreate("test_enable_t", "EnaT"));
        Long id = service.getByCode("test_enable_t").getId();
        service.disable(id);
        assertEquals(2, service.getById(id).getStatus());

        service.enable(id);
        assertEquals(1, service.getById(id).getStatus());
    }

    // ============================================================
    // delete / search
    // ============================================================

    @Test
    @DisplayName("delete: 删除成功，getById 返回 null")
    void delete_success() {
        service.create(baseCreate("test_del_t", "DelT"));
        Long id = service.getByCode("test_del_t").getId();

        service.delete(id);

        assertNull(service.getById(id), "删除后 getById 返回 null（逻辑删除）");
    }

    @Test
    @DisplayName("search: 按 tenantCode 模糊查询")
    void search_byCodeLike() {
        service.create(baseCreate("test_sr_a", "A"));
        service.create(baseCreate("test_sr_b", "B"));

        TenantQueryParam param = new TenantQueryParam();
        param.setTenantCode("test_sr");
        param.setPageNum(1);
        param.setPageSize(10);
        Pager<TenantDTO> result = service.search(param, PagerHandler.createPager(param));

        assertEquals(2, result.getPageData().size());
        assertTrue(result.getPageData().stream().allMatch(t -> t.getTenantCode().startsWith("test_sr")));
    }

    private static TenantCreateRequest baseCreate(String code, String adminLogin) {
        TenantCreateRequest req = new TenantCreateRequest();
        req.setTenantCode(code);
        req.setTenantName(adminLogin);  // 实际是管理员账号字段名
        req.setRealName("测试学校 " + code);
        req.setRoleIds(List.of(1L));    // 任意 roleId，sys_tenant_role 表无 FK
        return req;
    }
}
