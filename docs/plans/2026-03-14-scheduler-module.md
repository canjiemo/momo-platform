# 定时任务模块 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 基于 Spring ThreadPoolTaskScheduler 实现动态定时任务模块，支持后台管理 Cron 配置、启停、手动触发和执行历史查询。

**Architecture:** 任务定义存储于 sys_job 表，执行历史存储于 sys_job_log 表。JobScheduleManager 在启动时加载所有启用任务并注册到 ThreadPoolTaskScheduler，管理员操作后动态刷新调度。业务方只需实现 JobHandler 接口并注册为 Spring Bean，无需改动调度框架。

**Tech Stack:** Spring Boot 3.5.6、ThreadPoolTaskScheduler、myjdbc（BaseServiceImpl / lambdaQuery）、MyMVC（MyBaseController / MyResponseResult）

---

## 包路径约定

所有新文件均在 `seer-fitness-system` 模块下：

```
com.seer.fitness.system
├── entity/          SysJob.java, SysJobLog.java
├── dto/             JobDTO, JobQueryParam, JobCreateRequest, JobUpdateRequest,
│                    JobLogDTO, JobLogQueryParam
├── scheduler/       JobHandler.java (接口), JobScheduleManager.java
│                    handler/CourseReminderHandler.java (示例)
├── service/         SysJobService.java, SysJobLogService.java
└── controller/      SysJobController.java
```

---

## Task 1: Entity 实体类

**Files:**
- Create: `seer-fitness-system/src/main/java/com/seer/fitness/system/entity/SysJob.java`
- Create: `seer-fitness-system/src/main/java/com/seer/fitness/system/entity/SysJobLog.java`

**Step 1: 创建 SysJob.java**

```java
package com.seer.fitness.system.entity;

import io.github.canjiemo.base.myjdbc.MyTableEntity;
import io.github.canjiemo.base.myjdbc.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@MyTable("sys_job")
public class SysJob implements MyTableEntity {
    private Long id;
    private String jobName;
    private String jobGroup;
    private String handlerName;
    private String cronExpression;
    private String jobParams;
    /** 0=停用 1=启用 */
    private Integer status;
    private String remark;
    private Integer deleteFlag;
    private Long createdBy;
    private LocalDateTime createTime;
    private Long updatedBy;
    private LocalDateTime updateTime;
}
```

**Step 2: 创建 SysJobLog.java**

```java
package com.seer.fitness.system.entity;

import io.github.canjiemo.base.myjdbc.MyTableEntity;
import io.github.canjiemo.base.myjdbc.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@MyTable("sys_job_log")
public class SysJobLog implements MyTableEntity {
    private Long id;
    private Long jobId;
    private String jobName;
    private String handlerName;
    /** 0=定时触发 1=手动触发 */
    private Integer triggerType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    /** 0=失败 1=成功 */
    private Integer status;
    private String errorMsg;
    private Long operatorId;
}
```

**Step 3: Commit**

```bash
git add seer-fitness-system/src/main/java/com/seer/fitness/system/entity/SysJob.java \
        seer-fitness-system/src/main/java/com/seer/fitness/system/entity/SysJobLog.java
git commit -m "feat(scheduler): add SysJob and SysJobLog entities"
```

---

## Task 2: DTO 类

**Files:**
- Create: `seer-fitness-system/src/main/java/com/seer/fitness/system/dto/JobDTO.java`
- Create: `seer-fitness-system/src/main/java/com/seer/fitness/system/dto/JobQueryParam.java`
- Create: `seer-fitness-system/src/main/java/com/seer/fitness/system/dto/JobCreateRequest.java`
- Create: `seer-fitness-system/src/main/java/com/seer/fitness/system/dto/JobUpdateRequest.java`
- Create: `seer-fitness-system/src/main/java/com/seer/fitness/system/dto/JobLogDTO.java`
- Create: `seer-fitness-system/src/main/java/com/seer/fitness/system/dto/JobLogQueryParam.java`

**Step 1: JobDTO.java**

```java
package com.seer.fitness.system.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class JobDTO {
    private Long id;
    private String jobName;
    private String jobGroup;
    private String handlerName;
    private String cronExpression;
    private String jobParams;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

**Step 2: JobQueryParam.java**

```java
package com.seer.fitness.system.dto;

import io.github.canjiemo.mycommon.pager.PagerParam;
import lombok.Data;

@Data
public class JobQueryParam extends PagerParam {
    private String jobName;
    private String jobGroup;
    private String handlerName;
    private Integer status;
}
```

**Step 3: JobCreateRequest.java**

```java
package com.seer.fitness.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JobCreateRequest {
    @NotBlank(message = "任务名称不能为空")
    private String jobName;

    private String jobGroup;

    @NotBlank(message = "处理器名称不能为空")
    private String handlerName;

    @NotBlank(message = "Cron表达式不能为空")
    private String cronExpression;

    private String jobParams;

    @NotNull(message = "状态不能为空")
    private Integer status;

    private String remark;
}
```

**Step 4: JobUpdateRequest.java**

```java
package com.seer.fitness.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JobUpdateRequest {
    @NotNull(message = "任务ID不能为空")
    private Long id;

    @NotBlank(message = "任务名称不能为空")
    private String jobName;

    private String jobGroup;

    @NotBlank(message = "处理器名称不能为空")
    private String handlerName;

    @NotBlank(message = "Cron表达式不能为空")
    private String cronExpression;

    private String jobParams;

    @NotNull(message = "状态不能为空")
    private Integer status;

    private String remark;
}
```

**Step 5: JobLogDTO.java**

```java
package com.seer.fitness.system.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class JobLogDTO {
    private Long id;
    private Long jobId;
    private String jobName;
    private String handlerName;
    private Integer triggerType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    private Integer status;
    private String errorMsg;
    private Long operatorId;
}
```

**Step 6: JobLogQueryParam.java**

```java
package com.seer.fitness.system.dto;

import io.github.canjiemo.mycommon.pager.PagerParam;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class JobLogQueryParam extends PagerParam {
    private Long jobId;
    private Integer triggerType;
    private Integer status;
    private LocalDateTime startTimeBegin;
    private LocalDateTime startTimeEnd;
}
```

**Step 7: Commit**

```bash
git add seer-fitness-system/src/main/java/com/seer/fitness/system/dto/Job*.java \
        seer-fitness-system/src/main/java/com/seer/fitness/system/dto/JobLog*.java
git commit -m "feat(scheduler): add Job and JobLog DTO classes"
```

---

## Task 3: JobHandler 接口 + 示例处理器

**Files:**
- Create: `seer-fitness-system/src/main/java/com/seer/fitness/system/scheduler/JobHandler.java`
- Create: `seer-fitness-system/src/main/java/com/seer/fitness/system/scheduler/handler/CourseReminderHandler.java`

**Step 1: 创建 JobHandler 接口**

```java
package com.seer.fitness.system.scheduler;

/**
 * 定时任务处理器接口
 * 实现此接口并注册为 Spring Bean，Bean 名即为 sys_job.handler_name
 */
public interface JobHandler {
    /**
     * 任务执行入口
     * @param params sys_job.job_params 传入的参数（JSON 字符串，可为 null）
     */
    void execute(String params) throws Exception;
}
```

**Step 2: 创建示例处理器 CourseReminderHandler**

```java
package com.seer.fitness.system.scheduler.handler;

import com.seer.fitness.system.scheduler.JobHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 示例：课程提醒处理器
 * handler_name = courseReminderHandler
 */
@Slf4j
@Component("courseReminderHandler")
public class CourseReminderHandler implements JobHandler {

    @Override
    public void execute(String params) throws Exception {
        log.info("执行课程提醒任务, params={}", params);
        // TODO: 实现课程提醒业务逻辑
    }
}
```

**Step 3: Commit**

```bash
git add seer-fitness-system/src/main/java/com/seer/fitness/system/scheduler/
git commit -m "feat(scheduler): add JobHandler interface and CourseReminderHandler example"
```

---

## Task 4: JobScheduleManager（动态调度核心）

**Files:**
- Create: `seer-fitness-system/src/main/java/com/seer/fitness/system/scheduler/JobScheduleManager.java`

**Step 1: 创建 JobScheduleManager**

```java
package com.seer.fitness.system.scheduler;

import com.seer.fitness.system.entity.SysJob;
import com.seer.fitness.system.entity.SysJobLog;
import io.github.canjiemo.base.myjdbc.dao.BaseDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
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
    private BaseDao baseDao;

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
        // 校验 Cron
        if (!CronExpression.isValidExpression(job.getCronExpression())) {
            throw new IllegalArgumentException("无效的Cron表达式: " + job.getCronExpression());
        }
        // 获取处理器 Bean
        JobHandler handler = getHandler(job.getHandlerName());

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
     * 更新任务（取消旧的，注册新的）
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
        JobHandler handler = getHandler(handlerName);
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
```

**Step 2: Commit**

```bash
git add seer-fitness-system/src/main/java/com/seer/fitness/system/scheduler/JobScheduleManager.java
git commit -m "feat(scheduler): add JobScheduleManager for dynamic task scheduling"
```

---

## Task 5: ThreadPoolTaskScheduler 配置

**Files:**
- Create: `seer-fitness-system/src/main/java/com/seer/fitness/system/config/SchedulerConfig.java`
- Modify: `seer-fitness-boot/src/main/resources/application.yml`

**Step 1: 创建 SchedulerConfig.java**

```java
package com.seer.fitness.system.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerConfig {

    @Value("${scheduler.pool-size:5}")
    private int poolSize;

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix("job-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        return scheduler;
    }
}
```

**Step 2: 在 application.yml 末尾追加配置**

```yaml
# 定时任务线程池配置
scheduler:
  pool-size: 5
```

**Step 3: Commit**

```bash
git add seer-fitness-system/src/main/java/com/seer/fitness/system/config/SchedulerConfig.java \
        seer-fitness-boot/src/main/resources/application.yml
git commit -m "feat(scheduler): add ThreadPoolTaskScheduler bean and config"
```

---

## Task 6: SysJobService

**Files:**
- Create: `seer-fitness-system/src/main/java/com/seer/fitness/system/service/SysJobService.java`

**Step 1: 创建 SysJobService.java**

```java
package com.seer.fitness.system.service;

import com.google.common.collect.Maps;
import com.seer.fitness.system.dto.*;
import com.seer.fitness.system.entity.SysJob;
import com.seer.fitness.system.scheduler.JobScheduleManager;
import com.seer.fitness.system.utils.SecurityContextUtil;
import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import io.github.canjiemo.mycommon.exception.BusinessException;
import io.github.canjiemo.mycommon.pager.Pager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SysJobService extends BaseServiceImpl {

    @Autowired
    private JobScheduleManager scheduleManager;

    public Pager<JobDTO> search(JobQueryParam param, Pager pager) {
        StringBuilder sql = new StringBuilder(
            "SELECT id, job_name, job_group, handler_name, cron_expression, job_params, " +
            "status, remark, create_time, update_time FROM sys_job WHERE 1=1");
        Map<String, Object> params = Maps.newHashMap();

        if (param.getJobName() != null && !param.getJobName().isBlank()) {
            sql.append(" AND job_name LIKE :jobName");
            params.put("jobName", "%" + param.getJobName() + "%");
        }
        if (param.getJobGroup() != null && !param.getJobGroup().isBlank()) {
            sql.append(" AND job_group = :jobGroup");
            params.put("jobGroup", param.getJobGroup());
        }
        if (param.getHandlerName() != null && !param.getHandlerName().isBlank()) {
            sql.append(" AND handler_name = :handlerName");
            params.put("handlerName", param.getHandlerName());
        }
        if (param.getStatus() != null) {
            sql.append(" AND status = :status");
            params.put("status", param.getStatus());
        }
        sql.append(" ORDER BY create_time DESC");
        return baseDao.queryPageForSql(sql.toString(), params, pager, JobDTO.class);
    }

    public JobDTO getById(Long id) {
        SysJob job = baseDao.queryById(id, SysJob.class);
        if (job == null) throw new BusinessException("任务不存在");
        return convertToDTO(job);
    }

    @Transactional
    public void create(JobCreateRequest request) {
        validateCron(request.getCronExpression());
        if (isJobNameExists(request.getJobName(), null)) {
            throw new BusinessException("任务名称已存在");
        }

        SysJob job = new SysJob();
        job.setJobName(request.getJobName());
        job.setJobGroup(request.getJobGroup() != null ? request.getJobGroup() : "DEFAULT");
        job.setHandlerName(request.getHandlerName());
        job.setCronExpression(request.getCronExpression());
        job.setJobParams(request.getJobParams());
        job.setStatus(request.getStatus());
        job.setRemark(request.getRemark());
        job.setDeleteFlag(0);
        job.setCreateTime(LocalDateTime.now());
        job.setUpdateTime(LocalDateTime.now());
        job.setCreatedBy(SecurityContextUtil.getCurrentUser().getUserId());
        job.setUpdatedBy(SecurityContextUtil.getCurrentUser().getUserId());

        baseDao.insertPO(job, true);

        if (job.getStatus() == 1) {
            scheduleManager.register(job);
        }
    }

    @Transactional
    public void update(JobUpdateRequest request) {
        validateCron(request.getCronExpression());
        SysJob job = baseDao.queryById(request.getId(), SysJob.class);
        if (job == null) throw new BusinessException("任务不存在");
        if (isJobNameExists(request.getJobName(), request.getId())) {
            throw new BusinessException("任务名称已存在");
        }

        job.setJobName(request.getJobName());
        job.setJobGroup(request.getJobGroup() != null ? request.getJobGroup() : "DEFAULT");
        job.setHandlerName(request.getHandlerName());
        job.setCronExpression(request.getCronExpression());
        job.setJobParams(request.getJobParams());
        job.setStatus(request.getStatus());
        job.setRemark(request.getRemark());
        job.setUpdateTime(LocalDateTime.now());
        job.setUpdatedBy(SecurityContextUtil.getCurrentUser().getUserId());

        baseDao.updatePO(job);
        scheduleManager.refresh(job);
    }

    @Transactional
    public void delete(Long id) {
        SysJob job = baseDao.queryById(id, SysJob.class);
        if (job == null) throw new BusinessException("任务不存在");
        scheduleManager.cancel(id);
        baseDao.delByIds(SysJob.class, String.valueOf(id));
    }

    @Transactional
    public void enable(Long id) {
        SysJob job = baseDao.queryById(id, SysJob.class);
        if (job == null) throw new BusinessException("任务不存在");
        job.setStatus(1);
        job.setUpdateTime(LocalDateTime.now());
        job.setUpdatedBy(SecurityContextUtil.getCurrentUser().getUserId());
        baseDao.updatePO(job);
        scheduleManager.refresh(job);
    }

    @Transactional
    public void disable(Long id) {
        SysJob job = baseDao.queryById(id, SysJob.class);
        if (job == null) throw new BusinessException("任务不存在");
        job.setStatus(0);
        job.setUpdateTime(LocalDateTime.now());
        job.setUpdatedBy(SecurityContextUtil.getCurrentUser().getUserId());
        baseDao.updatePO(job);
        scheduleManager.cancel(id);
    }

    public void trigger(Long id) {
        SysJob job = baseDao.queryById(id, SysJob.class);
        if (job == null) throw new BusinessException("任务不存在");
        Long operatorId = SecurityContextUtil.getCurrentUser().getUserId();
        scheduleManager.triggerOnce(id, job.getJobName(), job.getHandlerName(), job.getJobParams(), operatorId);
    }

    private void validateCron(String cron) {
        if (!CronExpression.isValidExpression(cron)) {
            throw new BusinessException("无效的Cron表达式: " + cron);
        }
    }

    private boolean isJobNameExists(String jobName, Long excludeId) {
        var q = lambdaQuery(SysJob.class).eq(SysJob::getJobName, jobName);
        if (excludeId != null) q.ne(SysJob::getId, excludeId);
        return q.exists();
    }

    private JobDTO convertToDTO(SysJob job) {
        JobDTO dto = new JobDTO();
        dto.setId(job.getId());
        dto.setJobName(job.getJobName());
        dto.setJobGroup(job.getJobGroup());
        dto.setHandlerName(job.getHandlerName());
        dto.setCronExpression(job.getCronExpression());
        dto.setJobParams(job.getJobParams());
        dto.setStatus(job.getStatus());
        dto.setRemark(job.getRemark());
        dto.setCreateTime(job.getCreateTime());
        dto.setUpdateTime(job.getUpdateTime());
        return dto;
    }
}
```

**Step 2: Commit**

```bash
git add seer-fitness-system/src/main/java/com/seer/fitness/system/service/SysJobService.java
git commit -m "feat(scheduler): add SysJobService with CRUD, enable/disable, trigger"
```

---

## Task 7: SysJobLogService

**Files:**
- Create: `seer-fitness-system/src/main/java/com/seer/fitness/system/service/SysJobLogService.java`

**Step 1: 创建 SysJobLogService.java**

```java
package com.seer.fitness.system.service;

import com.google.common.collect.Maps;
import com.seer.fitness.system.dto.JobLogDTO;
import com.seer.fitness.system.dto.JobLogQueryParam;
import com.seer.fitness.system.entity.SysJobLog;
import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import io.github.canjiemo.mycommon.exception.BusinessException;
import io.github.canjiemo.mycommon.pager.Pager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SysJobLogService extends BaseServiceImpl {

    public Pager<JobLogDTO> search(JobLogQueryParam param, Pager pager) {
        StringBuilder sql = new StringBuilder(
            "SELECT id, job_id, job_name, handler_name, trigger_type, start_time, end_time, " +
            "duration_ms, status, error_msg, operator_id FROM sys_job_log WHERE 1=1");
        Map<String, Object> params = Maps.newHashMap();

        if (param.getJobId() != null) {
            sql.append(" AND job_id = :jobId");
            params.put("jobId", param.getJobId());
        }
        if (param.getTriggerType() != null) {
            sql.append(" AND trigger_type = :triggerType");
            params.put("triggerType", param.getTriggerType());
        }
        if (param.getStatus() != null) {
            sql.append(" AND status = :status");
            params.put("status", param.getStatus());
        }
        if (param.getStartTimeBegin() != null) {
            sql.append(" AND start_time >= :startTimeBegin");
            params.put("startTimeBegin", param.getStartTimeBegin());
        }
        if (param.getStartTimeEnd() != null) {
            sql.append(" AND start_time <= :startTimeEnd");
            params.put("startTimeEnd", param.getStartTimeEnd());
        }
        sql.append(" ORDER BY start_time DESC");
        return baseDao.queryPageForSql(sql.toString(), params, pager, JobLogDTO.class);
    }

    public JobLogDTO getById(Long id) {
        String sql = "SELECT id, job_id, job_name, handler_name, trigger_type, start_time, end_time, " +
                     "duration_ms, status, error_msg, operator_id FROM sys_job_log WHERE id = :id";
        Map<String, Object> params = Maps.newHashMap();
        params.put("id", id);
        JobLogDTO dto = baseDao.querySingleForSql(sql, params, JobLogDTO.class);
        if (dto == null) throw new BusinessException("日志记录不存在");
        return dto;
    }
}
```

**Step 2: Commit**

```bash
git add seer-fitness-system/src/main/java/com/seer/fitness/system/service/SysJobLogService.java
git commit -m "feat(scheduler): add SysJobLogService"
```

---

## Task 8: SysJobController

**Files:**
- Create: `seer-fitness-system/src/main/java/com/seer/fitness/system/controller/SysJobController.java`

**Step 1: 创建 SysJobController.java**

```java
package com.seer.fitness.system.controller;

import com.seer.fitness.system.annotation.OperationLog;
import com.seer.fitness.system.dto.*;
import com.seer.fitness.system.enums.OperationType;
import com.seer.fitness.system.security.RequireAuth;
import com.seer.fitness.system.service.SysJobLogService;
import com.seer.fitness.system.service.SysJobService;
import io.github.canjiemo.base.mymvc.controller.MyBaseController;
import io.github.canjiemo.base.mymvc.data.MyResponseResult;
import io.github.canjiemo.mycommon.pager.Pager;
import io.github.canjiemo.mycommon.pager.PagerHandler;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/platform/job")
public class SysJobController extends MyBaseController {

    @Autowired
    private SysJobService jobService;

    @Autowired
    private SysJobLogService jobLogService;

    @PostMapping("/search")
    @RequireAuth(permissions = {"job:view"})
    @OperationLog(type = OperationType.QUERY, module = "job", description = "分页查询定时任务")
    public MyResponseResult<Pager<JobDTO>> search(@RequestBody JobQueryParam param) {
        return super.doJsonPagerOut(jobService.search(param, PagerHandler.createPager(param)));
    }

    @GetMapping("/{id}")
    @RequireAuth(permissions = {"job:view"})
    public MyResponseResult<JobDTO> getById(@PathVariable Long id) {
        return super.doJsonOut(jobService.getById(id));
    }

    @PostMapping("/create")
    @RequireAuth(permissions = {"job:create"})
    @OperationLog(type = OperationType.CREATE, module = "job", description = "创建定时任务")
    public MyResponseResult create(@Valid @RequestBody JobCreateRequest request) {
        jobService.create(request);
        return super.doJsonDefaultMsg();
    }

    @PostMapping("/update")
    @RequireAuth(permissions = {"job:update"})
    @OperationLog(type = OperationType.UPDATE, module = "job", description = "更新定时任务")
    public MyResponseResult update(@Valid @RequestBody JobUpdateRequest request) {
        jobService.update(request);
        return super.doJsonDefaultMsg();
    }

    @PostMapping("/delete/{id}")
    @RequireAuth(permissions = {"job:delete"})
    @OperationLog(type = OperationType.DELETE, module = "job", description = "删除定时任务")
    public MyResponseResult delete(@PathVariable Long id) {
        jobService.delete(id);
        return super.doJsonDefaultMsg();
    }

    @PostMapping("/enable/{id}")
    @RequireAuth(permissions = {"job:update"})
    @OperationLog(type = OperationType.UPDATE, module = "job", description = "启用定时任务")
    public MyResponseResult enable(@PathVariable Long id) {
        jobService.enable(id);
        return super.doJsonDefaultMsg();
    }

    @PostMapping("/disable/{id}")
    @RequireAuth(permissions = {"job:update"})
    @OperationLog(type = OperationType.UPDATE, module = "job", description = "停用定时任务")
    public MyResponseResult disable(@PathVariable Long id) {
        jobService.disable(id);
        return super.doJsonDefaultMsg();
    }

    @PostMapping("/trigger/{id}")
    @RequireAuth(permissions = {"job:trigger"})
    @OperationLog(type = OperationType.UPDATE, module = "job", description = "手动触发定时任务")
    public MyResponseResult trigger(@PathVariable Long id) {
        jobService.trigger(id);
        return super.doJsonDefaultMsg();
    }

    // ---------- 执行历史 ----------

    @PostMapping("/log/search")
    @RequireAuth(permissions = {"job:view"})
    @OperationLog(type = OperationType.QUERY, module = "job", description = "查询定时任务执行历史")
    public MyResponseResult<Pager<JobLogDTO>> logSearch(@RequestBody JobLogQueryParam param) {
        return super.doJsonPagerOut(jobLogService.search(param, PagerHandler.createPager(param)));
    }

    @GetMapping("/log/{id}")
    @RequireAuth(permissions = {"job:view"})
    public MyResponseResult<JobLogDTO> logDetail(@PathVariable Long id) {
        return super.doJsonOut(jobLogService.getById(id));
    }
}
```

**Step 2: Commit**

```bash
git add seer-fitness-system/src/main/java/com/seer/fitness/system/controller/SysJobController.java
git commit -m "feat(scheduler): add SysJobController with full CRUD and log endpoints"
```

---

## Task 9: 菜单初始化数据（SQL）

**Files:**
- Modify: `seer-fitness-boot/src/main/resources/db/pgsql/002_init_data.sql`

> 在 002_init_data.sql 末尾追加以下 SQL，为定时任务菜单和权限录入初始数据。
> 同时在本地数据库执行：`docker cp ... && docker exec pgsql psql ...`

**Step 1: 查看现有菜单数据中最大 sort_order 和父菜单结构**

执行：`docker exec pgsql psql -U admin -d fitness-demo -c "SELECT id, menu_name, parent_id, sort_order FROM sys_menu ORDER BY sort_order;"`

**Step 2: 追加菜单 SQL 到 002_init_data.sql**

根据上一步查到的实际父菜单ID，在 002_init_data.sql 末尾追加（parent_id 取平台管理目录的实际 ID）：

```sql
-- ====================================================================================================
-- 定时任务菜单初始化（平台管理员菜单，tenant_id=NULL）
-- ====================================================================================================
INSERT INTO sys_menu (menu_name, parent_id, path, permission, type, icon, sort_order, status, delete_flag, create_time, update_time)
VALUES
  -- 定时任务目录
  ('定时任务', <平台管理父菜单ID>, '/platform/job', NULL, 1, 'clock', 90, 1, 0, NOW(), NOW()),
  -- 定时任务列表页
  ('任务列表', <上面插入的目录ID>, '/platform/job/list', 'job:view', 2, NULL, 1, 1, 0, NOW(), NOW()),
  -- 权限按钮
  ('创建任务', <上面插入的目录ID>, NULL, 'job:create', 3, NULL, 2, 1, 0, NOW(), NOW()),
  ('编辑任务', <上面插入的目录ID>, NULL, 'job:update', 3, NULL, 3, 1, 0, NOW(), NOW()),
  ('删除任务', <上面插入的目录ID>, NULL, 'job:delete', 3, NULL, 4, 1, 0, NOW(), NOW()),
  ('手动触发', <上面插入的目录ID>, NULL, 'job:trigger', 3, NULL, 5, 1, 0, NOW(), NOW()),
  ('执行历史', <上面插入的目录ID>, NULL, 'job:view', 3, NULL, 6, 1, 0, NOW(), NOW());
```

> ⚠️ 注意：parent_id 需要根据实际数据库中的菜单 ID 填写，不能硬编码。先查询再插入。

**Step 3: 在本地数据库执行菜单 SQL**

```bash
# 将 SQL 语句直接执行到本地数据库
docker exec pgsql psql -U admin -d fitness-demo -c "<上面的INSERT语句>"
```

**Step 4: Commit**

```bash
git add seer-fitness-boot/src/main/resources/db/pgsql/002_init_data.sql
git commit -m "feat(scheduler): add job menu init data"
```

---

## Task 10: 编译验证

**Step 1: 编译整个项目**

```bash
cd /Users/canjiemo/project/seer-fitness-edu
mvn clean package -DskipTests
```

预期：`BUILD SUCCESS`

**Step 2: 处理编译错误**

如有编译错误，根据错误信息修正对应文件（常见问题：import 路径、方法名拼写）。

**Step 3: 最终 Commit**

```bash
git add .
git commit -m "feat(scheduler): complete scheduler module implementation"
```

---

## 验收清单

- [ ] `mvn clean package -DskipTests` 通过
- [ ] 应用启动日志出现 "定时任务模块初始化完成"
- [ ] `POST /platform/job/create` 能正常创建任务
- [ ] `POST /platform/job/trigger/{id}` 手动触发后 sys_job_log 有记录
- [ ] `POST /platform/job/log/search` 能查询执行历史
- [ ] 修改 Cron 后任务按新表达式执行

---

## 新增处理器指南

后续新增业务定时任务只需两步：

```java
// 1. 实现 JobHandler，注册为 Bean
@Slf4j
@Component("myNewHandler")   // Bean 名即 handler_name
public class MyNewHandler implements JobHandler {
    @Override
    public void execute(String params) throws Exception {
        // 业务逻辑
    }
}
```

```sql
-- 2. 在 sys_job 插入一条记录（或通过后台页面创建）
INSERT INTO sys_job (job_name, handler_name, cron_expression, status, delete_flag, create_time, update_time)
VALUES ('我的新任务', 'myNewHandler', '0 0 9 * * ?', 1, 0, NOW(), NOW());
```
