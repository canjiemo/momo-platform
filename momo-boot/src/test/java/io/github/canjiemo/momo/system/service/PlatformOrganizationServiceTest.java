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
 * 平台组织（tenant_id=NULL）CRUD 集成测试。
 */
class PlatformOrganizationServiceTest extends AbstractIntegrationTest {

    @Autowired
    private PlatformOrganizationService service;

    @Test
    @DisplayName("create: 创建平台组织成功，tenant_id=NULL")
    void create_success() {
        service.create(baseCreate("PLAT_ORG_1", "平台组织 1", null));

        Long id = findIdByCode("PLAT_ORG_1");
        OrganizationDTO got = service.getById(id);
        assertEquals("平台组织 1", got.getOrgName());
    }

    @Test
    @DisplayName("create: orgCode 重复抛业务异常")
    void create_duplicateCodeThrows() {
        service.create(baseCreate("PLAT_ORG_DUP", "A", null));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(baseCreate("PLAT_ORG_DUP", "B", null)));
        assertTrue(ex.getMessage().contains("组织编码已存在"));
    }

    @Test
    @DisplayName("update: 修改 orgName / contactPhone / status 成功")
    void update_basicFields() {
        service.create(baseCreate("PLAT_ORG_UPD", "原名", null));
        Long id = findIdByCode("PLAT_ORG_UPD");

        OrganizationUpdateRequest req = baseUpdate(id, "PLAT_ORG_UPD", "新名");
        req.setContactPhone("13800000099");
        req.setStatus(0);
        service.update(req);

        OrganizationDTO after = service.getById(id);
        assertEquals("新名", after.getOrgName());
        assertEquals("13800000099", after.getContactPhone());
        assertEquals(0, after.getStatus());
    }

    @Test
    @DisplayName("update[回归]: leaderId / contactPhone / email / address / description 传 null 时写入 NULL")
    void update_nullableFieldsCanBeClearedToNull() {
        OrganizationCreateRequest c = baseCreate("PLAT_ORG_NULL", "回归平台", null);
        c.setContactPhone("13900000000");
        c.setEmail("orig@example.com");
        c.setDescription("初始描述");
        service.create(c);
        Long id = findIdByCode("PLAT_ORG_NULL");
        OrganizationDTO before = service.getById(id);
        assertEquals("13900000000", before.getContactPhone());
        assertEquals("orig@example.com", before.getEmail());
        assertEquals("初始描述", before.getDescription());

        OrganizationUpdateRequest req = baseUpdate(id, "PLAT_ORG_NULL", "回归平台");
        req.setContactPhone(null);
        req.setEmail(null);
        req.setDescription(null);
        service.update(req);

        OrganizationDTO after = service.getById(id);
        assertNull(after.getContactPhone(), "contactPhone 传 null 必须写入 NULL");
        assertNull(after.getEmail(),        "email 传 null 必须写入 NULL");
        assertNull(after.getDescription(),  "description 传 null 必须写入 NULL");
    }

    @Test
    @DisplayName("update: 主键不存在抛业务异常")
    void update_nonExistentIdThrows() {
        OrganizationUpdateRequest req = baseUpdate(999_999_999L, "NO_EXIST", "不存在");
        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(req));
        assertTrue(ex.getMessage().contains("平台组织不存在"));
    }

    @Test
    @DisplayName("delete: 无子组织 / 无成员时删除成功")
    void delete_leafSuccess() {
        service.create(baseCreate("PLAT_ORG_DEL", "可删", null));
        Long id = findIdByCode("PLAT_ORG_DEL");

        service.delete(new String[]{String.valueOf(id)});

        assertThrows(BusinessException.class, () -> service.getById(id));
    }

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
