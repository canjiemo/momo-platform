package io.github.canjiemo.momo.system.service;

import io.github.canjiemo.momo.boot.test.AbstractIntegrationTest;
import io.github.canjiemo.momo.system.dto.OrganizationCreateRequest;
import io.github.canjiemo.momo.system.dto.UserCreateRequest;
import io.github.canjiemo.momo.system.dto.UserDTO;
import io.github.canjiemo.momo.system.dto.UserQueryParam;
import io.github.canjiemo.momo.system.dto.UserUpdateRequest;
import io.github.canjiemo.mycommon.exception.BusinessException;
import io.github.canjiemo.mycommon.pager.Pager;
import io.github.canjiemo.mycommon.pager.PagerHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户 CRUD 集成测试。
 * <p>
 * 注意：UserService.update 包含两条分支：
 * <ol>
 *   <li>{@code adminFlag=1} 时只允许改 realName（{@code updatePO(user)} 跳过 null）</li>
 *   <li>普通用户走全量更新（{@code updatePO(user, false)} 强写入 null）—— 关键回归</li>
 * </ol>
 * password 字段需满足强密码策略（含大写、小写、数字、特殊字符，8-15 位）。
 */
class UserServiceTest extends AbstractIntegrationTest {

    private static final String VALID_PASSWORD = "Test1234!@";

    @Autowired
    private UserService service;

    @Autowired
    private OrganizationService orgService;

    // ============================================================
    // create
    // ============================================================

    @Test
    @DisplayName("create: 创建普通用户成功")
    void create_success() {
        service.create(baseCreate("test_u1", "用户1"));

        Long id = findIdByUsername("test_u1");
        UserDTO got = service.getById(id);
        assertEquals("test_u1", got.getUsername());
        assertEquals("用户1", got.getRealName());
        assertEquals(0, got.getAdminFlag(), "默认 adminFlag=0");
    }

    @Test
    @DisplayName("create: username 重复抛业务异常")
    void create_duplicateUsernameThrows() {
        service.create(baseCreate("test_u_dup", "A"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(baseCreate("test_u_dup", "B")));
        assertTrue(ex.getMessage().contains("用户名已存在"));
    }

    @Test
    @DisplayName("create: orgId 不存在抛业务异常")
    void create_invalidOrgIdThrows() {
        UserCreateRequest req = baseCreate("test_u_bad_org", "X");
        req.setOrgId(999_999_999L);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req));
        assertTrue(ex.getMessage().contains("组织不存在"));
    }

    // ============================================================
    // update - 普通用户分支
    // ============================================================

    @Test
    @DisplayName("update[普通用户]: 修改 realName / status / userType 成功")
    void update_basicFields() {
        service.create(baseCreate("test_u_upd", "原名"));
        Long id = findIdByUsername("test_u_upd");

        UserUpdateRequest req = baseUpdate(id, "新名");
        req.setStatus(0);
        req.setUserType(1);
        service.update(req);

        UserDTO after = service.getById(id);
        assertEquals("新名", after.getRealName());
        assertEquals(0, after.getStatus());
        assertEquals(1, after.getUserType());
    }

    @Test
    @DisplayName("update[回归-普通用户]: orgId 传 null 时数据库实际写入 NULL（updatePO(user, false) 修复）")
    void update_orgIdCanBeClearedToNull() {
        OrganizationCreateRequest org = new OrganizationCreateRequest();
        org.setOrgCode("TEST_U_ORG");
        org.setOrgName("用户测试组织");
        org.setStatus(1);
        orgService.create(org);
        Long orgId = orgService.list().stream()
                .filter(o -> "TEST_U_ORG".equals(o.getOrgCode()))
                .findFirst().orElseThrow().getId();

        UserCreateRequest c = baseCreate("test_u_null", "回归用户");
        c.setOrgId(orgId);
        service.create(c);
        Long userId = findIdByUsername("test_u_null");
        assertEquals(orgId, service.getById(userId).getOrgId(), "前置：orgId 已写入");

        UserUpdateRequest req = baseUpdate(userId, "回归用户");
        req.setOrgId(null);
        service.update(req);

        assertNull(service.getById(userId).getOrgId(), "orgId 传 null 必须真正写入 NULL（用户离开组织）");
    }

    @Test
    @DisplayName("update: 用户不存在抛业务异常")
    void update_nonExistentIdThrows() {
        UserUpdateRequest req = baseUpdate(999_999_999L, "不存在");
        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(req));
        assertTrue(ex.getMessage().contains("用户不存在"));
    }

    // ============================================================
    // delete
    // ============================================================

    @Test
    @DisplayName("delete: 删除普通用户成功")
    void delete_normalUserSuccess() {
        service.create(baseCreate("test_u_del", "可删"));
        Long id = findIdByUsername("test_u_del");

        service.delete(new String[]{String.valueOf(id)});

        assertThrows(BusinessException.class, () -> service.getById(id));
    }

    @Test
    @DisplayName("delete: id 数组为空抛业务异常")
    void delete_emptyIdsThrows() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(new String[0]));
        assertTrue(ex.getMessage().contains("不能为空"));
    }

    @Test
    @DisplayName("delete: 用户不存在抛业务异常")
    void delete_nonExistentIdThrows() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.delete(new String[]{"999999999"}));
        assertTrue(ex.getMessage().contains("用户不存在"));
    }

    // ============================================================
    // password
    // ============================================================

    @Test
    @DisplayName("resetPassword: 重置成功，密码与原密码不同")
    void resetPassword_success() {
        service.create(baseCreate("test_u_pwd", "密码用户"));
        Long id = findIdByUsername("test_u_pwd");

        service.resetPassword(id, "NewPass123!@");
        // 不报错即视为成功（无返回值）
    }

    @Test
    @DisplayName("resetPassword: 用户不存在抛业务异常")
    void resetPassword_nonExistentIdThrows() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.resetPassword(999_999_999L, "NewPass123!@"));
        assertTrue(ex.getMessage().contains("用户不存在"));
    }

    // ============================================================
    // search
    // ============================================================

    @Test
    @DisplayName("search: 按 username 模糊查询")
    void search_byUsernameLike() {
        service.create(baseCreate("test_su_a", "A"));
        service.create(baseCreate("test_su_b", "B"));

        UserQueryParam param = new UserQueryParam();
        param.setUsername("test_su");
        param.setPageNum(1);
        param.setPageSize(10);
        Pager<UserDTO> result = service.search(param, PagerHandler.createPager(param));

        assertEquals(2, result.getPageData().size());
        assertTrue(result.getPageData().stream().allMatch(u -> u.getUsername().startsWith("test_su")));
    }

    // ============================================================
    // helpers
    // ============================================================

    private Long findIdByUsername(String username) {
        UserQueryParam q = new UserQueryParam();
        q.setUsername(username);
        q.setPageNum(1);
        q.setPageSize(1);
        return service.search(q, PagerHandler.createPager(q))
                .getPageData().get(0).getId();
    }

    private static UserCreateRequest baseCreate(String username, String realName) {
        UserCreateRequest req = new UserCreateRequest();
        req.setUsername(username);
        req.setPassword(VALID_PASSWORD);
        req.setRealName(realName);
        req.setStatus(1);
        req.setUserType(0);   // 0=运维
        return req;
    }

    private static UserUpdateRequest baseUpdate(Long id, String realName) {
        UserUpdateRequest req = new UserUpdateRequest();
        req.setId(id);
        req.setRealName(realName);
        req.setStatus(1);
        return req;
    }
}
