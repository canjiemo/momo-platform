package io.github.canjiemo.momo.system.service;

import io.github.canjiemo.momo.boot.test.AbstractIntegrationTest;
import io.github.canjiemo.momo.system.dto.OrganizationCreateRequest;
import io.github.canjiemo.momo.system.dto.OrganizationDTO;
import io.github.canjiemo.momo.system.dto.OrganizationUpdateRequest;
import io.github.canjiemo.mycommon.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 组织架构 CRUD 集成测试。
 */
class OrganizationServiceTest extends AbstractIntegrationTest {

    @Autowired
    private OrganizationService service;

    // ============================================================
    // create
    // ============================================================

    @Test
    @DisplayName("create: 创建顶级组织成功，parentId 落库为 0")
    void create_topLevelOrg() {
        service.create(baseCreate("TEST_ORG_TOP", "测试顶级组织", null));

        Long id = findIdByCode("TEST_ORG_TOP");
        assertEquals(0L, service.getById(id).getParentId());
    }

    @Test
    @DisplayName("create: 创建子组织成功")
    void create_childOrg() {
        service.create(baseCreate("TEST_ORG_P", "父组织", null));
        Long parentId = findIdByCode("TEST_ORG_P");

        service.create(baseCreate("TEST_ORG_C", "子组织", parentId));

        Long childId = findIdByCode("TEST_ORG_C");
        assertEquals(parentId, service.getById(childId).getParentId());
    }

    @Test
    @DisplayName("create: orgCode 重复抛业务异常")
    void create_duplicateCodeThrows() {
        service.create(baseCreate("TEST_DUP_CODE", "A", null));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(baseCreate("TEST_DUP_CODE", "B", null)));
        assertTrue(ex.getMessage().contains("组织编码已存在"));
    }

    @Test
    @DisplayName("create: 指定的父组织不存在抛业务异常")
    void create_invalidParentThrows() {
        OrganizationCreateRequest req = baseCreate("TEST_ORG_ORPHAN", "孤儿组织", 999_999_999L);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req));
        assertTrue(ex.getMessage().contains("父组织不存在"));
    }

    // ============================================================
    // update
    // ============================================================

    @Test
    @DisplayName("update: 修改 orgName / contactPhone / email / status 成功")
    void update_basicFields() {
        service.create(baseCreate("TEST_ORG_UPD", "原名", null));
        Long id = findIdByCode("TEST_ORG_UPD");

        OrganizationUpdateRequest req = baseUpdate(id, "TEST_ORG_UPD", "新名");
        req.setContactPhone("13800000099");
        req.setEmail("new@example.com");
        req.setStatus(0);
        service.update(req);

        OrganizationDTO after = service.getById(id);
        assertEquals("新名", after.getOrgName());
        assertEquals("13800000099", after.getContactPhone());
        assertEquals("new@example.com", after.getEmail());
        assertEquals(0, after.getStatus());
    }

    @Test
    @DisplayName("update[回归]: leaderId / contactPhone / email / address / description 传 null 时写入 NULL")
    void update_nullableFieldsCanBeClearedToNull() {
        OrganizationCreateRequest c = baseCreate("TEST_ORG_NULL", "回归组织", null);
        c.setContactPhone("13900000000");
        c.setEmail("orig@example.com");
        c.setAddress("初始地址");
        c.setDescription("初始描述");
        service.create(c);
        Long id = findIdByCode("TEST_ORG_NULL");
        OrganizationDTO before = service.getById(id);
        assertEquals("13900000000", before.getContactPhone(), "前置：contactPhone 已写入");
        assertEquals("orig@example.com", before.getEmail());
        assertEquals("初始地址", before.getAddress());
        assertEquals("初始描述", before.getDescription());

        OrganizationUpdateRequest req = baseUpdate(id, "TEST_ORG_NULL", "回归组织");
        req.setContactPhone(null);
        req.setEmail(null);
        req.setAddress(null);
        req.setDescription(null);
        req.setLeaderId(null);
        service.update(req);

        OrganizationDTO after = service.getById(id);
        assertNull(after.getContactPhone(), "contactPhone 传 null 必须写入 NULL");
        assertNull(after.getEmail(),        "email 传 null 必须写入 NULL");
        assertNull(after.getAddress(),      "address 传 null 必须写入 NULL");
        assertNull(after.getDescription(),  "description 传 null 必须写入 NULL");
    }

    @Test
    @DisplayName("update: 把 parentId 设为自己抛业务异常")
    void update_selfAsParentThrows() {
        service.create(baseCreate("TEST_ORG_SELF", "自闭环", null));
        Long id = findIdByCode("TEST_ORG_SELF");

        OrganizationUpdateRequest req = baseUpdate(id, "TEST_ORG_SELF", "自闭环");
        req.setParentId(id);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(req));
        assertTrue(ex.getMessage().contains("自己设为父组织"));
    }

    @Test
    @DisplayName("update: 主键不存在抛业务异常")
    void update_nonExistentIdThrows() {
        OrganizationUpdateRequest req = baseUpdate(999_999_999L, "NO_EXIST", "不存在");
        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(req));
        assertTrue(ex.getMessage().contains("组织不存在"));
    }

    // ============================================================
    // delete
    // ============================================================

    @Test
    @DisplayName("delete: 无子组织时删除成功")
    void delete_leafOrgSuccess() {
        service.create(baseCreate("TEST_ORG_DEL", "可删", null));
        Long id = findIdByCode("TEST_ORG_DEL");

        service.delete(new String[]{String.valueOf(id)});

        assertThrows(BusinessException.class, () -> service.getById(id));
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
        List<OrganizationDTO> all = service.list();
        return all.stream()
                .filter(o -> code.equals(o.getOrgCode()))
                .findFirst().orElseThrow()
                .getId();
    }

    private static OrganizationCreateRequest baseCreate(String code, String name, Long parentId) {
        OrganizationCreateRequest req = new OrganizationCreateRequest();
        req.setOrgCode(code);
        req.setOrgName(name);
        req.setParentId(parentId);
        req.setStatus(1);
        return req;
    }

    private static OrganizationUpdateRequest baseUpdate(Long id, String code, String name) {
        OrganizationUpdateRequest req = new OrganizationUpdateRequest();
        req.setId(id);
        req.setOrgCode(code);
        req.setOrgName(name);
        req.setStatus(1);
        return req;
    }
}
