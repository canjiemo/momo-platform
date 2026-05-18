package io.github.canjiemo.momo.system.service;

import io.github.canjiemo.momo.boot.test.AbstractIntegrationTest;
import io.github.canjiemo.momo.system.dto.RoleCreateRequest;
import io.github.canjiemo.momo.system.dto.RoleDTO;
import io.github.canjiemo.momo.system.dto.RoleQueryParam;
import io.github.canjiemo.momo.system.dto.RoleUpdateRequest;
import io.github.canjiemo.mycommon.exception.BusinessException;
import io.github.canjiemo.mycommon.pager.Pager;
import io.github.canjiemo.mycommon.pager.PagerHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 平台角色（tenant_id = NULL）CRUD 集成测试。
 */
class PlatformRoleServiceTest extends AbstractIntegrationTest {

    @Autowired
    private PlatformRoleService service;

    @Test
    @DisplayName("create: 创建平台角色后 list 可见，tenant_id=NULL")
    void create_success() {
        service.create(baseCreate("PLAT_R1", "平台角色 R1"));

        boolean found = service.list().stream().anyMatch(r -> "PLAT_R1".equals(r.getRoleCode()));
        assertTrue(found, "新建的平台角色应在 list() 中可见");
    }

    @Test
    @DisplayName("create: 角色名重复抛业务异常")
    void create_duplicateNameThrows() {
        service.create(baseCreate("PLAT_DUP_N_A", "重名平台角色"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(baseCreate("PLAT_DUP_N_B", "重名平台角色")));
        assertTrue(ex.getMessage().contains("角色名"));
    }

    @Test
    @DisplayName("update: 修改名称 / 描述 / 状态成功")
    void update_basicFields() {
        service.create(baseCreate("PLAT_UPD_BASIC", "原名"));
        Long id = findIdByCode("PLAT_UPD_BASIC");

        RoleUpdateRequest req = baseUpdate(id, "PLAT_UPD_BASIC", "新名", "新描述");
        req.setStatus(0);
        service.update(req);

        RoleDTO after = service.getById(id);
        assertEquals("新名", after.getRoleName());
        assertEquals("新描述", after.getDescription());
        assertEquals(0, after.getStatus());
    }

    @Test
    @DisplayName("update[回归]: description 传 null 时数据库实际写入 NULL（updatePO(role, false) 修复）")
    void update_nullableDescriptionCanBeClearedToNull() {
        RoleCreateRequest c = baseCreate("PLAT_UPD_NULL", "可空回归");
        c.setDescription("初始描述");
        service.create(c);
        Long id = findIdByCode("PLAT_UPD_NULL");
        assertEquals("初始描述", service.getById(id).getDescription(), "前置：description 已写入");

        service.update(baseUpdate(id, "PLAT_UPD_NULL", "可空回归", null));

        RoleDTO after = service.getById(id);
        assertNull(after.getDescription(), "description 传 null 必须真正写入 NULL");
    }

    @Test
    @DisplayName("update: 主键不存在抛业务异常")
    void update_nonExistentIdThrows() {
        RoleUpdateRequest req = baseUpdate(999_999_999L, "NO_EXIST", "不存在", null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(req));
        assertTrue(ex.getMessage().contains("平台角色不存在"));
    }

    @Test
    @DisplayName("delete: 无关联租户时删除成功")
    void delete_success() {
        service.create(baseCreate("PLAT_DEL_OK", "可删"));
        Long id = findIdByCode("PLAT_DEL_OK");

        service.delete(id);

        assertThrows(BusinessException.class, () -> service.getById(id));
    }

    @Test
    @DisplayName("search: 按 roleName 模糊查询返回平台角色（tenant_id=NULL）")
    void search_byNameLike() {
        service.create(baseCreate("PLAT_SEARCH_X", "平台搜索-X"));
        service.create(baseCreate("PLAT_SEARCH_Y", "平台搜索-Y"));

        RoleQueryParam param = new RoleQueryParam();
        param.setRoleName("平台搜索");
        param.setPageNum(1);
        param.setPageSize(10);
        Pager<RoleDTO> result = service.search(param, PagerHandler.createPager(param));

        assertEquals(2, result.getPageData().size());
        assertTrue(result.getPageData().stream().allMatch(r -> r.getRoleName().contains("平台搜索")));
    }

    private Long findIdByCode(String code) {
        return service.list().stream()
                .filter(r -> code.equals(r.getRoleCode()))
                .findFirst().orElseThrow()
                .getId();
    }

    private static RoleCreateRequest baseCreate(String code, String name) {
        RoleCreateRequest req = new RoleCreateRequest();
        req.setRoleCode(code);
        req.setRoleName(name);
        req.setDescription("默认描述");
        req.setRoleType(1);   // 1=平台角色
        req.setStatus(1);
        return req;
    }

    private static RoleUpdateRequest baseUpdate(Long id, String code, String name, String desc) {
        RoleUpdateRequest req = new RoleUpdateRequest();
        req.setId(id);
        req.setRoleCode(code);
        req.setRoleName(name);
        req.setDescription(desc);
        req.setStatus(1);
        return req;
    }
}
