package io.github.canjiemo.momo.system.service;

import io.github.canjiemo.momo.boot.test.AbstractIntegrationTest;
import io.github.canjiemo.momo.system.dto.JobCreateRequest;
import io.github.canjiemo.momo.system.dto.JobDTO;
import io.github.canjiemo.momo.system.dto.JobQueryParam;
import io.github.canjiemo.momo.system.dto.JobUpdateRequest;
import io.github.canjiemo.mycommon.exception.BusinessException;
import io.github.canjiemo.mycommon.pager.Pager;
import io.github.canjiemo.mycommon.pager.PagerHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 定时任务 CRUD 集成测试。
 * <p>
 * 注意：测试一律使用 status=0（禁用），避免触发 {@link io.github.canjiemo.momo.system.scheduler.JobScheduleManager#register} 真实调度；
 * status=1 启用会被 register 加入 ThreadPoolTaskScheduler，事务回滚不会清调度器，
 * 多个测试相互污染。CRUD 行为靠 status=0 路径验证已足够。
 */
class SysJobServiceTest extends AbstractIntegrationTest {

    private static final String HANDLER = "courseReminderHandler";

    @Autowired
    private SysJobService service;

    // ============================================================
    // create
    // ============================================================

    @Test
    @DisplayName("create: 创建任务成功，可按 search 查回")
    void create_success() {
        service.create(baseCreate("test_job_1", "0 0 * * * ?"));

        JobQueryParam q = new JobQueryParam();
        q.setJobName("test_job_1");
        q.setPageNum(1);
        q.setPageSize(10);
        Pager<JobDTO> result = service.search(q, PagerHandler.createPager(q));
        assertEquals(1, result.getPageData().size());
        assertEquals("0 0 * * * ?", result.getPageData().get(0).getCronExpression());
    }

    @Test
    @DisplayName("create: 无效 Cron 表达式抛业务异常")
    void create_invalidCronThrows() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(baseCreate("test_bad_cron", "not a cron")));
        assertTrue(ex.getMessage().contains("Cron"));
    }

    @Test
    @DisplayName("create: jobName 重复抛业务异常")
    void create_duplicateNameThrows() {
        service.create(baseCreate("test_dup_job", "0 0 * * * ?"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(baseCreate("test_dup_job", "0 1 * * * ?")));
        assertTrue(ex.getMessage().contains("任务名称已存在"));
    }

    // ============================================================
    // update
    // ============================================================

    @Test
    @DisplayName("update: 修改 cron / handler / status 成功")
    void update_basicFields() {
        service.create(baseCreate("test_upd_basic", "0 0 * * * ?"));
        Long id = findIdByName("test_upd_basic");

        JobUpdateRequest req = baseUpdate(id, "test_upd_basic", "0 30 * * * ?", "p=1", "备注");
        service.update(req);

        JobDTO after = service.getById(id);
        assertEquals("0 30 * * * ?", after.getCronExpression());
        assertEquals("p=1", after.getJobParams());
        assertEquals("备注", after.getRemark());
    }

    @Test
    @DisplayName("update[回归]: jobParams / remark 传 null 时数据库实际写入 NULL（updatePO(job, false) 修复）")
    void update_nullableParamsAndRemarkCanBeClearedToNull() {
        JobCreateRequest c = baseCreate("test_upd_null", "0 0 * * * ?");
        c.setJobParams("p=initial");
        c.setRemark("初始备注");
        service.create(c);
        Long id = findIdByName("test_upd_null");
        JobDTO before = service.getById(id);
        assertEquals("p=initial", before.getJobParams(), "前置：jobParams 已写入");
        assertEquals("初始备注", before.getRemark());

        JobUpdateRequest req = baseUpdate(id, "test_upd_null", "0 0 * * * ?", null, null);
        service.update(req);

        JobDTO after = service.getById(id);
        assertNull(after.getJobParams(), "jobParams 传 null 必须真正写入 NULL");
        assertNull(after.getRemark(), "remark 传 null 必须真正写入 NULL");
    }

    @Test
    @DisplayName("update: 任务不存在抛业务异常")
    void update_nonExistentIdThrows() {
        JobUpdateRequest req = baseUpdate(999_999_999L, "no_exist", "0 0 * * * ?", null, null);
        BusinessException ex = assertThrows(BusinessException.class, () -> service.update(req));
        assertTrue(ex.getMessage().contains("任务不存在"));
    }

    // ============================================================
    // delete / enable / disable
    // ============================================================

    @Test
    @DisplayName("delete: 删除成功")
    void delete_success() {
        service.create(baseCreate("test_del_job", "0 0 * * * ?"));
        Long id = findIdByName("test_del_job");

        service.delete(id);

        assertThrows(BusinessException.class, () -> service.getById(id));
    }

    @Test
    @DisplayName("disable: 禁用任务后 status 变为 0")
    void disable_setsStatusZero() {
        service.create(baseCreate("test_disable", "0 0 * * * ?"));  // status=0 by default
        Long id = findIdByName("test_disable");

        service.disable(id);

        assertEquals(0, service.getById(id).getStatus());
    }

    // ============================================================
    // helpers
    // ============================================================

    private Long findIdByName(String jobName) {
        JobQueryParam q = new JobQueryParam();
        q.setJobName(jobName);
        q.setPageNum(1);
        q.setPageSize(1);
        return service.search(q, PagerHandler.createPager(q))
                .getPageData().get(0).getId();
    }

    private static JobCreateRequest baseCreate(String name, String cron) {
        JobCreateRequest req = new JobCreateRequest();
        req.setJobName(name);
        req.setHandlerName(HANDLER);
        req.setCronExpression(cron);
        req.setStatus(0);              // 禁用：不触发 scheduler.register
        return req;
    }

    private static JobUpdateRequest baseUpdate(Long id, String name, String cron, String params, String remark) {
        JobUpdateRequest req = new JobUpdateRequest();
        req.setId(id);
        req.setJobName(name);
        req.setHandlerName(HANDLER);
        req.setCronExpression(cron);
        req.setJobParams(params);
        req.setRemark(remark);
        req.setStatus(0);              // 禁用：refresh 只 cancel 不重新 register
        return req;
    }
}
