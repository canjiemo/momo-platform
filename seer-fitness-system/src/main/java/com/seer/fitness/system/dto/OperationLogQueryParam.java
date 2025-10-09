package com.seer.fitness.system.dto;

import io.github.mocanjie.base.mycommon.pager.PagerParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 操作日志查询参数
 * 继承分页参数，支持复杂查询条件
 *
 * @author seer-fitness
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OperationLogQueryParam extends PagerParam {

    /**
     * 操作用户ID
     */
    private Long userId;

    /**
     * 操作用户名（模糊查询）
     */
    private String username;

    /**
     * 操作用户真实姓名（模糊查询）
     */
    private String realName;

    /**
     * 操作类型：CREATE/UPDATE/DELETE/QUERY/LOGIN/LOGOUT
     */
    private String operationType;

    /**
     * 操作模块：user/role/menu/organization等
     */
    private String moduleName;

    /**
     * 业务数据ID
     */
    private String businessId;

    /**
     * 业务数据名称（模糊查询）
     */
    private String businessName;

    /**
     * 操作描述（模糊查询）
     */
    private String operationDesc;

    /**
     * 请求方式：GET/POST/PUT/DELETE
     */
    private String requestMethod;

    /**
     * 请求URL（模糊查询）
     */
    private String requestUrl;

    /**
     * 操作IP地址
     */
    private String ipAddress;

    /**
     * 操作结果：1成功 0失败
     */
    private Integer operationResult;

    /**
     * 开始时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 最小执行耗时（毫秒）
     */
    private Integer minExecutionTime;

    /**
     * 最大执行耗时（毫秒）
     */
    private Integer maxExecutionTime;
}