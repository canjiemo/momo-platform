package com.seer.fitness.system.scheduler;

import com.seer.fitness.system.entity.SysJob;
import com.seer.fitness.system.entity.SysJobLog;
import io.github.canjiemo.base.myjdbc.dao.IBaseDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
public class JobScheduleManager {

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    private IBaseDao baseDao;

    @Autowired
    private ApplicationContext applicationContext;

    /** 已注册任务的 Future，key=jobId */
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    /** 应用启动时加载所有启用任务 */
    @PostConstruct
    public void init() {
        String sql = "SELECT * FROM sys_job WHERE status = 1 AND delete_flag = 0";
        List<SysJob> jobs = baseDao.queryListForSql(sql, new HashMap<>(), SysJob.class);
        for (SysJob job : jobs) {
            try {
                register(job);
            } catch (Exception e) {
                log.error("启动时注册任务失败, jobId={}, jobName={}", job.getId(), job.getJobName(), e);
            }
        }
        log.info("定时任务模块初始化完成，加载任务数量: {}", jobs.size());
    }

    /**
     * 注册任务到调度器
     */
    public void register(SysJob job) {
        if (!CronExpression.isValidExpression(job.getCronExpression())) {
            throw new IllegalArgumentException("无效的Cron表达式: " + job.getCronExpression());
        }
        getHandler(job.getHandlerName()); // 提前校验 handler 存在

        ScheduledFuture<?> future = taskScheduler.schedule(
            () -> executeJob(job.getId(), job.getJobName(), job.getHandlerName(), job.getJobParams(), 0, null),
            new CronTrigger(job.getCronExpression())
        );
        scheduledTasks.put(job.getId(), future);
        log.info("注册定时任务: id={}, name={}, cron={}", job.getId(), job.getJobName(), job.getCronExpression());
    }

    /**
     * 取消任务调度
     */
    public void cancel(Long jobId) {
        ScheduledFuture<?> future = scheduledTasks.remove(jobId);
        if (future != null) {
            future.cancel(false);
            log.info("取消定时任务: jobId={}", jobId);
        }
    }

    /**
     * 更新任务（取消旧的，按需重新注册）
     */
    public void refresh(SysJob job) {
        cancel(job.getId());
        if (job.getStatus() != null && job.getStatus() == 1) {
            register(job);
        }
    }

    /**
     * 手动触发一次
     */
    public void triggerOnce(Long jobId, String jobName, String handlerName, String jobParams, Long operatorId) {
        getHandler(handlerName); // 校验 handler 存在
        taskScheduler.execute(() -> executeJob(jobId, jobName, handlerName, jobParams, 1, operatorId));
    }

    /**
     * 执行任务并记录日志
     */
    private void executeJob(Long jobId, String jobName, String handlerName, String params,
                            int triggerType, Long operatorId) {
        SysJobLog jobLog = new SysJobLog();
        jobLog.setJobId(jobId);
        jobLog.setJobName(jobName);
        jobLog.setHandlerName(handlerName);
        jobLog.setTriggerType(triggerType);
        jobLog.setOperatorId(operatorId);
        jobLog.setStartTime(LocalDateTime.now());

        long start = System.currentTimeMillis();
        try {
            JobHandler handler = getHandler(handlerName);
            handler.execute(params);
            jobLog.setStatus(1);
            log.info("定时任务执行成功: jobId={}, jobName={}", jobId, jobName);
        } catch (Exception e) {
            jobLog.setStatus(0);
            jobLog.setErrorMsg(e.getMessage() != null ? e.getMessage() : e.getClass().getName());
            log.error("定时任务执行失败: jobId={}, jobName={}", jobId, jobName, e);
        } finally {
            jobLog.setEndTime(LocalDateTime.now());
            jobLog.setDurationMs(System.currentTimeMillis() - start);
            try {
                baseDao.insertPO(jobLog, true);
            } catch (Exception ex) {
                log.error("写入任务执行日志失败", ex);
            }
        }
    }

    private JobHandler getHandler(String handlerName) {
        try {
            return applicationContext.getBean(handlerName, JobHandler.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("找不到处理器Bean: " + handlerName);
        }
    }
}
