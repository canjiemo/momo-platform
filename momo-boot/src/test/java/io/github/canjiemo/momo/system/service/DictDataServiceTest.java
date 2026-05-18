package io.github.canjiemo.momo.system.service;

import io.github.canjiemo.momo.boot.test.AbstractIntegrationTest;
import io.github.canjiemo.momo.system.dto.DictDataCreateRequest;
import io.github.canjiemo.momo.system.dto.DictDataDTO;
import io.github.canjiemo.momo.system.dto.DictDataQueryParam;
import io.github.canjiemo.momo.system.dto.DictDataUpdateRequest;
import io.github.canjiemo.momo.system.dto.DictTypeCreateRequest;
import io.github.canjiemo.mycommon.exception.BusinessException;
import io.github.canjiemo.mycommon.pager.Pager;
import io.github.canjiemo.mycommon.pager.PagerHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 字典数据 CRUD 集成测试。
 * <p>
 * 关键覆盖点：
 * <ol>
 *   <li>create / update / delete / search 全链路</li>
 *   <li>{@link #update_nullableFieldsCanBeClearedToNull()} —— 验证 updatePO(po, false) 修复</li>
 *   <li>默认项切换、按 dictType 查标签、批量排序</li>
 * </ol>
 */
class DictDataServiceTest extends AbstractIntegrationTest {

    private static final String DICT_TYPE = "test_dict_data";

    @Autowired
    private DictTypeService typeService;

    @Autowired
    private DictDataService service;

    @BeforeEach
    void setupParentDictType() {
        DictTypeCreateRequest req = new DictTypeCreateRequest();
        req.setDictType(DICT_TYPE);
        req.setDictName("数据测试");
        req.setStatus(1);
        req.setSortOrder(0);
        typeService.create(req);
    }

    // ============================================================
    // create
    // ============================================================

    @Test
    @DisplayName("create: 新建字典数据成功，可在该 dictType 下查到")
    void create_success() {
        service.create(baseCreate("启用", "1"));

        List<DictDataDTO> list = service.getByDictType(DICT_TYPE);
        assertEquals(1, list.size());
        assertEquals("启用", list.get(0).getDictLabel());
    }

    @Test
    @DisplayName("create: 同 dictType 下 dictValue 重复抛业务异常")
    void create_duplicateValueThrows() {
        service.create(baseCreate("启用", "1"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(baseCreate("再次启用", "1")));
        assertTrue(ex.getMessage().contains("字典值已存在"));
    }

    @Test
    @DisplayName("create: 新建 isDefault=1 时，自动清掉同 dictType 下其它默认项")
    void create_isDefaultClearsOthers() {
        DictDataCreateRequest first = baseCreate("旧默认", "1");
        first.setIsDefault(1);
        service.create(first);

        DictDataCreateRequest second = baseCreate("新默认", "2");
        second.setIsDefault(1);
        service.create(second);

        List<DictDataDTO> list = service.getByDictType(DICT_TYPE);
        long defaults = list.stream().filter(d -> Boolean.TRUE.equals(d.getIsDefault())).count();
        assertEquals(1, defaults, "同 dictType 下应当只有一个默认项");
        assertEquals("新默认",
                list.stream().filter(d -> Boolean.TRUE.equals(d.getIsDefault()))
                        .findFirst().orElseThrow().getDictLabel());
    }

    // ============================================================
    // update
    // ============================================================

    @Test
    @DisplayName("update: 修改 label / 状态 / 排序成功")
    void update_basicFields() {
        service.create(baseCreate("原标签", "1", "原描述", "原备注"));
        DictDataDTO created = service.getByDictType(DICT_TYPE).get(0);

        DictDataUpdateRequest req = toUpdate(created);
        req.setDictLabel("新标签");
        req.setSortOrder(50);
        req.setStatus(0);
        service.update(req);

        DictDataDTO after = service.getById(String.valueOf(created.getId()));
        assertEquals("新标签", after.getDictLabel());
        assertEquals(50, after.getSortOrder());
        assertEquals(0, after.getStatus());
    }

    @Test
    @DisplayName("update[回归]: dictDescription / cssClass / listClass / remark 传 null 时，数据库实际写入 null")
    void update_nullableFieldsCanBeClearedToNull() {
        DictDataCreateRequest create = baseCreate("回归", "regression", "初始描述", "初始备注");
        create.setCssClass("text-red");
        create.setListClass("primary");
        service.create(create);
        DictDataDTO created = service.getByDictType(DICT_TYPE).get(0);
        assertEquals("初始描述", created.getDictDescription(), "前置：初始描述已写入");
        assertEquals("text-red", created.getCssClass());
        assertEquals("primary", created.getListClass());
        assertEquals("初始备注", created.getRemark());

        DictDataUpdateRequest req = toUpdate(created);
        req.setDictDescription(null);   // 期望清空
        req.setCssClass(null);
        req.setListClass(null);
        req.setRemark(null);
        service.update(req);

        DictDataDTO after = service.getById(String.valueOf(created.getId()));
        assertNull(after.getDictDescription(), "dictDescription 传 null 必须真正写入 NULL");
        assertNull(after.getCssClass(),        "cssClass 传 null 必须真正写入 NULL");
        assertNull(after.getListClass(),       "listClass 传 null 必须真正写入 NULL");
        assertNull(after.getRemark(),          "remark 传 null 必须真正写入 NULL");
    }

    @Test
    @DisplayName("update: 把 dictValue 改成同类型下已存在的值时抛业务异常")
    void update_duplicateValueThrows() {
        service.create(baseCreate("A", "1"));
        service.create(baseCreate("B", "2"));
        DictDataDTO a = service.getByDictType(DICT_TYPE).stream()
                .filter(d -> "1".equals(d.getDictValue())).findFirst().orElseThrow();

        DictDataUpdateRequest req = toUpdate(a);
        req.setDictValue("2");          // 改成已存在的 value

        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(req));
        assertTrue(ex.getMessage().contains("字典值已存在"));
    }

    // ============================================================
    // delete
    // ============================================================

    @Test
    @DisplayName("delete: 删除成功后 getByDictType 返回空列表")
    void delete_success() {
        service.create(baseCreate("X", "x"));
        DictDataDTO created = service.getByDictType(DICT_TYPE).get(0);

        service.delete(String.valueOf(created.getId()));

        List<DictDataDTO> after = service.getByDictType(DICT_TYPE);
        assertTrue(after.isEmpty(), "删除后列表应为空");
    }

    @Test
    @DisplayName("delete: 删除不存在的 id 抛业务异常")
    void delete_nonExistentIdThrows() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.delete("999999999"));
        assertTrue(ex.getMessage().contains("字典数据不存在"));
    }

    // ============================================================
    // batchUpdateSortOrder
    // ============================================================

    @Test
    @DisplayName("batchUpdateSortOrder: 批量更新排序")
    void batchUpdateSortOrder_success() {
        service.create(baseCreate("A", "1"));
        service.create(baseCreate("B", "2"));
        List<DictDataDTO> list = service.getByDictType(DICT_TYPE);

        service.batchUpdateSortOrder(
                List.of(String.valueOf(list.get(0).getId()), String.valueOf(list.get(1).getId())),
                List.of(100, 200));

        DictDataDTO first  = service.getById(String.valueOf(list.get(0).getId()));
        DictDataDTO second = service.getById(String.valueOf(list.get(1).getId()));
        assertEquals(100, first.getSortOrder());
        assertEquals(200, second.getSortOrder());
    }

    // ============================================================
    // getDictLabel / getDesc
    // ============================================================

    @Test
    @DisplayName("getDictLabel: 命中返回 label，未命中返回 null")
    void getDictLabel() {
        service.create(baseCreate("启用", "1"));

        assertEquals("启用", service.getDictLabel(DICT_TYPE, "1"));
        assertNull(service.getDictLabel(DICT_TYPE, "99_not_exist"));
        assertNull(service.getDictLabel(null, "1"));
        assertNull(service.getDictLabel(DICT_TYPE, null));
    }

    @Test
    @DisplayName("getDesc: 支持 Integer 等非字符串 dictValue 入参")
    void getDesc_acceptsNonStringValue() {
        service.create(baseCreate("启用", "1"));

        assertEquals("启用", service.getDesc(DICT_TYPE, 1));
        assertEquals("启用", service.getDesc(DICT_TYPE, "1"));
        assertNull(service.getDesc(DICT_TYPE, null));
    }

    // ============================================================
    // search
    // ============================================================

    @Test
    @DisplayName("search: 按 dictLabel 模糊 + dictType 精确联合查询")
    void search_combined() {
        service.create(baseCreate("启用A", "1"));
        service.create(baseCreate("禁用",  "2"));
        service.create(baseCreate("启用B", "3"));

        DictDataQueryParam param = new DictDataQueryParam();
        param.setDictType(DICT_TYPE);
        param.setDictLabel("启用");
        param.setPageNum(1);
        param.setPageSize(10);
        Pager<DictDataDTO> result = service.search(param, PagerHandler.createPager(param));

        assertEquals(2, result.getPageData().size());
        assertTrue(result.getPageData().stream().allMatch(d -> d.getDictLabel().contains("启用")));
    }

    // ============================================================
    // helpers
    // ============================================================

    private static DictDataCreateRequest baseCreate(String label, String value) {
        return baseCreate(label, value, null, null);
    }

    private static DictDataCreateRequest baseCreate(String label, String value, String desc, String remark) {
        DictDataCreateRequest req = new DictDataCreateRequest();
        req.setDictType(DICT_TYPE);
        req.setDictLabel(label);
        req.setDictValue(value);
        req.setDictDescription(desc);
        req.setIsDefault(0);
        req.setStatus(1);
        req.setSortOrder(0);
        req.setRemark(remark);
        return req;
    }

    private static DictDataUpdateRequest toUpdate(DictDataDTO src) {
        DictDataUpdateRequest req = new DictDataUpdateRequest();
        req.setId(src.getId());
        req.setDictType(src.getDictType());
        req.setDictLabel(src.getDictLabel());
        req.setDictValue(src.getDictValue());
        req.setDictDescription(src.getDictDescription());
        req.setCssClass(src.getCssClass());
        req.setListClass(src.getListClass());
        // DictDataDTO.isDefault 是 Boolean，但 Request/Entity 用 Integer，这里手动桥接
        req.setIsDefault(Boolean.TRUE.equals(src.getIsDefault()) ? 1 : 0);
        req.setStatus(src.getStatus());
        req.setSortOrder(src.getSortOrder());
        req.setRemark(src.getRemark());
        return req;
    }
}
