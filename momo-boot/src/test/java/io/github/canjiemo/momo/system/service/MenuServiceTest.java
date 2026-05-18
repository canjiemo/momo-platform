package io.github.canjiemo.momo.system.service;

import io.github.canjiemo.momo.boot.test.AbstractIntegrationTest;
import io.github.canjiemo.momo.system.dto.MenuCreateRequest;
import io.github.canjiemo.momo.system.dto.MenuDTO;
import io.github.canjiemo.momo.system.dto.MenuUpdateRequest;
import io.github.canjiemo.mycommon.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 菜单 CRUD 集成测试。
 */
class MenuServiceTest extends AbstractIntegrationTest {

    @Autowired
    private MenuService service;

    // ============================================================
    // create
    // ============================================================

    @Test
    @DisplayName("create: 创建顶级菜单（parentId=null）成功，parentId 默认 0")
    void create_topLevelMenu() {
        service.create(baseCreate("测试目录-顶级", null, 0));

        Long id = findIdByName("测试目录-顶级");
        MenuDTO got = service.getById(id);
        assertEquals(0L, got.getParentId(), "parentId 为 null 时落库为 0（顶级菜单）");
    }

    @Test
    @DisplayName("create: 创建子菜单成功")
    void create_childMenu() {
        service.create(baseCreate("测试父菜单", null, 0));
        Long parentId = findIdByName("测试父菜单");

        service.create(baseCreate("测试子菜单", parentId, 1));
        Long childId = findIdByName("测试子菜单");

        assertEquals(parentId, service.getById(childId).getParentId());
    }

    @Test
    @DisplayName("create: 指定的父菜单不存在抛业务异常")
    void create_invalidParentThrows() {
        MenuCreateRequest req = baseCreate("孤儿菜单", 999_999_999L, 1);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(req));
        assertTrue(ex.getMessage().contains("父菜单不存在"));
    }

    // ============================================================
    // update
    // ============================================================

    @Test
    @DisplayName("update: 修改 menuName / path / type / status 成功")
    void update_basicFields() {
        service.create(baseCreate("测试-原名", null, 0));
        Long id = findIdByName("测试-原名");

        MenuUpdateRequest req = baseUpdate(id, "测试-新名", "/new/path", 1);
        req.setStatus(0);
        service.update(req);

        MenuDTO after = service.getById(id);
        assertEquals("测试-新名", after.getMenuName());
        assertEquals("/new/path", after.getPath());
        assertEquals(1, after.getType());
        assertEquals(0, after.getStatus());
    }

    @Test
    @DisplayName("update[回归]: path / permission / icon / sortOrder 传 null 时数据库实际写入 NULL")
    void update_nullableFieldsCanBeClearedToNull() {
        MenuCreateRequest c = baseCreate("测试-回归", null, 1);
        c.setPath("/initial/path");
        c.setPermission("init:perm");
        c.setIcon("InitIcon");
        c.setSortOrder(5);
        service.create(c);
        Long id = findIdByName("测试-回归");
        MenuDTO before = service.getById(id);
        assertEquals("/initial/path", before.getPath(), "前置：path 已写入");
        assertEquals("init:perm",     before.getPermission());
        assertEquals("InitIcon",      before.getIcon());
        assertEquals(5,               before.getSortOrder());

        MenuUpdateRequest req = baseUpdate(id, "测试-回归", null, 1);
        req.setPermission(null);
        req.setIcon(null);
        req.setSortOrder(null);
        service.update(req);

        MenuDTO after = service.getById(id);
        assertNull(after.getPath(),       "path 传 null 必须真正写入 NULL");
        assertNull(after.getPermission(), "permission 传 null 必须真正写入 NULL");
        assertNull(after.getIcon(),       "icon 传 null 必须真正写入 NULL");
        assertNull(after.getSortOrder(),  "sortOrder 传 null 必须真正写入 NULL");
    }

    @Test
    @DisplayName("update: 把 parentId 设为自己抛业务异常")
    void update_selfAsParentThrows() {
        service.create(baseCreate("测试-自闭环", null, 0));
        Long id = findIdByName("测试-自闭环");

        MenuUpdateRequest req = baseUpdate(id, "测试-自闭环", null, 0);
        req.setParentId(id);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(req));
        assertTrue(ex.getMessage().contains("自己设为父菜单"));
    }

    @Test
    @DisplayName("update: 主键不存在抛业务异常")
    void update_nonExistentIdThrows() {
        MenuUpdateRequest req = baseUpdate(999_999_999L, "不存在", null, 1);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(req));
        assertTrue(ex.getMessage().contains("菜单不存在"));
    }

    // ============================================================
    // delete
    // ============================================================

    @Test
    @DisplayName("delete: 无子菜单时删除成功")
    void delete_leafMenuSuccess() {
        service.create(baseCreate("测试-叶子", null, 1));
        Long id = findIdByName("测试-叶子");

        service.delete(String.valueOf(id));

        assertThrows(BusinessException.class, () -> service.getById(id));
    }

    @Test
    @DisplayName("delete: 有子菜单时抛业务异常")
    void delete_hasChildrenThrows() {
        service.create(baseCreate("测试-父菜单2", null, 0));
        Long parentId = findIdByName("测试-父菜单2");
        service.create(baseCreate("测试-子菜单2", parentId, 1));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.delete(String.valueOf(parentId)));
        assertTrue(ex.getMessage().contains("存在子菜单"));
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

    private Long findIdByName(String menuName) {
        List<MenuDTO> all = service.list();
        return all.stream()
                .filter(m -> menuName.equals(m.getMenuName()))
                .findFirst().orElseThrow()
                .getId();
    }

    private static MenuCreateRequest baseCreate(String name, Long parentId, int type) {
        MenuCreateRequest req = new MenuCreateRequest();
        req.setMenuName(name);
        req.setParentId(parentId);
        req.setType(type);
        req.setStatus(1);
        return req;
    }

    private static MenuUpdateRequest baseUpdate(Long id, String name, String path, int type) {
        MenuUpdateRequest req = new MenuUpdateRequest();
        req.setId(id);
        req.setMenuName(name);
        req.setPath(path);
        req.setType(type);
        req.setStatus(1);
        return req;
    }
}
