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
 * 角色 CRUD 集成测试。
 * <p>
 * 关键覆盖点：
 * <ol>
 *   <li>create / update / delete / search / getById 全链路</li>
 *   <li>{@link #update_nullableDescriptionCanBeClearedToNull()} —— updatePO(role, false) 回归</li>
 *   <li>角色名 / 角色编码唯一性约束</li>
 * </ol>
 */
class RoleServiceTest extends AbstractIntegrationTest {

    @Autowired
    private RoleService service;

    // ============================================================
    // create
    // ============================================================

    @Test
    @DisplayName("create: 创建角色成功，可按 search 查回")
    void create_success() {
        service.create(baseCreate("TEST_R1", "测试角色 R1"));

        RoleQueryParam param = new RoleQueryParam();
        param.setRoleCode("TEST_R1");
        param.setPageNum(1);
        param.setPageSize(10);
        Pager<RoleDTO> result = service.search(param, PagerHandler.createPager(param));

        assertEquals(1, result.getPageData().size());
        assertEquals("测试角色 R1", result.getPageData().get(0).getRoleName());
    }

    @Test
    @DisplayName("create: 角色名重复抛业务异常")
    void create_duplicateNameThrows() {
        service.create(baseCreate("DUP_NAME_A", "重名角色"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(baseCreate("DUP_NAME_B", "重名角色")));
        assertTrue(ex.getMessage().contains("角色名"));
    }

    @Test
    @DisplayName("create: 角色编码重复抛业务异常")
    void create_duplicateCodeThrows() {
        service.create(baseCreate("DUP_CODE", "角色 A"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(baseCreate("DUP_CODE", "角色 B")));
        assertTrue(ex.getMessage().contains("角色编码"));
    }

    // ============================================================
    // update
    // ============================================================

    @Test
    @DisplayName("update: 修改名称 / 状态成功")
    void update_basicFields() {
        service.create(baseCreate("UPD_BASIC", "原名"));
        Long id = findIdByCode("UPD_BASIC");

        RoleUpdateRequest req = baseUpdate(id, "UPD_BASIC", "新名称", "新描述");
        req.setStatus(0);
        service.update(req);

        RoleDTO after = service.getById(String.valueOf(id));
        assertEquals("新名称", after.getRoleName());
        assertEquals("新描述", after.getDescription());
        assertEquals(0, after.getStatus());
    }

    @Test
    @DisplayName("update[回归]: description 传 null 时数据库实际写入 NULL（updatePO(role, false) 修复）")
    void update_nullableDescriptionCanBeClearedToNull() {
        RoleCreateRequest create = baseCreate("UPD_NULL", "可空回归");
        create.setDescription("初始描述");
        service.create(create);
        Long id = findIdByCode("UPD_NULL");
        assertEquals("初始描述", service.getById(String.valueOf(id)).getDescription(), "前置：description 已写入");

        RoleUpdateRequest req = baseUpdate(id, "UPD_NULL", "可空回归", null);
        service.update(req);

        RoleDTO after = service.getById(String.valueOf(id));
        assertNull(after.getDescription(), "description 传 null 必须真正写入 NULL");
    }

    @Test
    @DisplayName("update: 主键不存在抛业务异常")
    void update_nonExistentIdThrows() {
        RoleUpdateRequest req = baseUpdate(999_999_999L, "NO_EXIST", "不存在", null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(req));
        assertTrue(ex.getMessage().contains("角色不存在"));
    }

    @Test
    @DisplayName("update: 把 roleCode 改成同表内已存在的值抛业务异常")
    void update_duplicateCodeThrows() {
        service.create(baseCreate("UPD_CODE_A", "A"));
        service.create(baseCreate("UPD_CODE_B", "B"));
        Long aId = findIdByCode("UPD_CODE_A");

        RoleUpdateRequest req = baseUpdate(aId, "UPD_CODE_B", "A", "原描述");
        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(req));
        assertTrue(ex.getMessage().contains("角色编码"));
    }

    // ============================================================
    // delete
    // ============================================================

    @Test
    @DisplayName("delete: 无关联用户时删除成功")
    void delete_success() {
        service.create(baseCreate("DEL_OK", "可删"));
        Long id = findIdByCode("DEL_OK");

        service.delete(String.valueOf(id));

        assertThrows(BusinessException.class, () -> service.getById(String.valueOf(id)));
    }

    @Test
    @DisplayName("delete: id 数组为空抛业务异常")
    void delete_emptyIdsThrows() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(new String[0]));
        assertTrue(ex.getMessage().contains("不能为空"));
    }

    // ============================================================
    // helpers
    // ============================================================

    private Long findIdByCode(String code) {
        RoleQueryParam q = new RoleQueryParam();
        q.setRoleCode(code);
        q.setPageNum(1);
        q.setPageSize(1);
        return service.search(q, PagerHandler.createPager(q))
                .getPageData().get(0).getId();
    }

    private static RoleCreateRequest baseCreate(String code, String name) {
        RoleCreateRequest req = new RoleCreateRequest();
        req.setRoleCode(code);
        req.setRoleName(name);
        req.setDescription("默认描述");
        req.setRoleType(2);     // 2=租户模板角色
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
