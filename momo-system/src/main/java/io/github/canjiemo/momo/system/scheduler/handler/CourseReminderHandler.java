package io.github.canjiemo.momo.system.scheduler.handler;

import io.github.canjiemo.momo.system.scheduler.JobHandler;
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
