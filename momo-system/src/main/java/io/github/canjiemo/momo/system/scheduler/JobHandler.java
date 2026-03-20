package io.github.canjiemo.momo.system.scheduler;

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
