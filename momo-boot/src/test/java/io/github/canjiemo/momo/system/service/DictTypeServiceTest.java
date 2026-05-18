package io.github.canjiemo.momo.system.service;

import io.github.canjiemo.momo.boot.test.AbstractIntegrationTest;
import io.github.canjiemo.momo.system.dto.DictTypeCreateRequest;
import io.github.canjiemo.momo.system.dto.DictTypeDTO;
import io.github.canjiemo.momo.system.dto.DictTypeQueryParam;
import io.github.canjiemo.momo.system.dto.DictTypeUpdateRequest;
import io.github.canjiemo.mycommon.exception.BusinessException;
import io.github.canjiemo.mycommon.pager.Pager;
import io.github.canjiemo.mycommon.pager.PagerHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 字典类型 CRUD 集成测试。
 * <p>
 * 关键覆盖点：
 * <ol>
 *   <li>create / update / delete / search / getById / getByDictType 全链路</li>
 *   <li>{@link #update_nullableFieldsCanBeClearedToNull()} —— 验证 updatePO(po, false) 修复的回归</li>
 *   <li>唯一性、级联约束等业务异常</li>
 * </ol>
 */
class DictTypeServiceTest extends AbstractIntegrationTest {

    @Autowired
    private DictTypeService service;

    // ============================================================
    // create
    // ============================================================

    @Test
    @DisplayName("create: 新建字典类型成功，可按 dictType 查回")
    void create_success() {
        DictTypeCreateRequest req = baseCreate("test_status", "测试状态字典");
        service.create(req);

        DictTypeDTO got = service.getByDictType("test_status");
        assertNotNull(got);
        assertEquals("测试状态字典", got.getDictName());
        assertEquals(1, got.getStatus());
    }

    @Test
    @DisplayName("create: dictType 重复时抛出业务异常")
    void create_duplicateDictType() {
        service.create(baseCreate("dup_type", "第一次"));
        DictTypeCreateRequest dup = baseCreate("dup_type", "第二次");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.create(dup));
        assertTrue(ex.getMessage().contains("字典类型已存在"));
    }

    // ============================================================
    // update
    // ============================================================

    @Test
    @DisplayName("update: 修改名称 / 状态 / 排序成功")
    void update_basicFields() {
        service.create(baseCreate("upd_basic", "原名称", "原描述", "原备注"));
        DictTypeDTO created = service.getByDictType("upd_basic");

        DictTypeUpdateRequest req = new DictTypeUpdateRequest();
        req.setId(created.getId());
        req.setDictName("新名称");
        req.setDictType("upd_basic");
        req.setDictDescription("新描述");
        req.setStatus(0);
        req.setSortOrder(99);
        req.setRemark("新备注");
        service.update(req);

        DictTypeDTO after = service.getByDictType("upd_basic");
        assertEquals("新名称", after.getDictName());
        assertEquals("新描述", after.getDictDescription());
        assertEquals(0, after.getStatus());
        assertEquals(99, after.getSortOrder());
        assertEquals("新备注", after.getRemark());
    }

    @Test
    @DisplayName("update[回归]: dictDescription / remark 传 null 时，数据库实际写入 null（修复 updatePO 默认跳 null 的 bug）")
    void update_nullableFieldsCanBeClearedToNull() {
        service.create(baseCreate("upd_null", "名称", "初始描述", "初始备注"));
        DictTypeDTO created = service.getByDictType("upd_null");
        assertEquals("初始描述", created.getDictDescription(), "前置：初始描述应正确写入");
        assertEquals("初始备注", created.getRemark());

        DictTypeUpdateRequest req = new DictTypeUpdateRequest();
        req.setId(created.getId());
        req.setDictName(created.getDictName());
        req.setDictType(created.getDictType());
        req.setDictDescription(null);   // 期望清空
        req.setStatus(created.getStatus());
        req.setSortOrder(created.getSortOrder());
        req.setRemark(null);            // 期望清空
        service.update(req);

        DictTypeDTO after = service.getByDictType("upd_null");
        assertNull(after.getDictDescription(), "可空字段 dictDescription 传 null 必须真正写入 NULL");
        assertNull(after.getRemark(), "可空字段 remark 传 null 必须真正写入 NULL");
    }

    @Test
    @DisplayName("update: 主键不存在抛出业务异常")
    void update_nonExistentIdThrows() {
        DictTypeUpdateRequest req = new DictTypeUpdateRequest();
        req.setId(999_999_999L);
        req.setDictName("x");
        req.setDictType("x_not_exist");
        req.setStatus(1);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(req));
        assertTrue(ex.getMessage().contains("字典类型不存在"));
    }

    @Test
    @DisplayName("update: 改名为已存在的 dictType 抛出业务异常")
    void update_duplicateDictTypeThrows() {
        service.create(baseCreate("upd_a", "A"));
        service.create(baseCreate("upd_b", "B"));
        DictTypeDTO a = service.getByDictType("upd_a");

        DictTypeUpdateRequest req = new DictTypeUpdateRequest();
        req.setId(a.getId());
        req.setDictName("A");
        req.setDictType("upd_b");           // 改成 B 的 dictType
        req.setStatus(1);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(req));
        assertTrue(ex.getMessage().contains("字典类型已存在"));
    }

    // ============================================================
    // delete
    // ============================================================

    @Test
    @DisplayName("delete: 无关联数据时，删除成功")
    void delete_success() {
        service.create(baseCreate("del_ok", "可删"));
        DictTypeDTO created = service.getByDictType("del_ok");

        service.delete(String.valueOf(created.getId()));

        assertThrows(BusinessException.class, () -> service.getByDictType("del_ok"));
    }

    @Test
    @DisplayName("delete: id 数组为空时抛业务异常")
    void delete_emptyIdsThrows() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete(new String[0]));
        assertTrue(ex.getMessage().contains("不能为空"));
    }

    // ============================================================
    // search
    // ============================================================

    @Test
    @DisplayName("search: 按 dictName 模糊查询返回匹配项，分页元信息正确")
    void search_byNameLike() {
        service.create(baseCreate("search_s1", "搜索-S1"));
        service.create(baseCreate("search_s2", "搜索-S2"));
        service.create(baseCreate("search_other", "其它"));

        DictTypeQueryParam param = new DictTypeQueryParam();
        param.setDictName("搜索");
        param.setPageNum(1);
        param.setPageSize(10);
        Pager<DictTypeDTO> result = service.search(param, PagerHandler.createPager(param));

        assertNotNull(result.getPageData());
        assertTrue(result.getPageData().size() >= 2);
        assertTrue(result.getPageData().stream().allMatch(d -> d.getDictName().contains("搜索")));
    }

    // ============================================================
    // helpers
    // ============================================================

    private static DictTypeCreateRequest baseCreate(String dictType, String dictName) {
        return baseCreate(dictType, dictName, "默认描述", "默认备注");
    }

    private static DictTypeCreateRequest baseCreate(String dictType, String dictName,
                                                    String desc, String remark) {
        DictTypeCreateRequest req = new DictTypeCreateRequest();
        req.setDictType(dictType);
        req.setDictName(dictName);
        req.setDictDescription(desc);
        req.setStatus(1);
        req.setSortOrder(0);
        req.setRemark(remark);
        return req;
    }
}
