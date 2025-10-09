package com.seer.fitness.system.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志DTO
 * 用于数据传输和前端展示
 *
 * @author seer-fitness
 */
@Data
public class OperationLogDTO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 操作用户ID
     */
    private Long userId;

    /**
     * 操作用户名
     */
    private String username;

    /**
     * 操作用户真实姓名
     */
    private String realName;

    /**
     * 操作类型：CREATE/UPDATE/DELETE/QUERY/LOGIN/LOGOUT
     */
    private String operationType;

    /**
     * 操作类型描述
     */
    private String operationTypeDesc;

    /**
     * 操作模块：user/role/menu/organization等
     */
    private String moduleName;

    /**
     * 业务数据ID
     */
    private String businessId;

    /**
     * 业务数据名称
     */
    private String businessName;

    /**
     * 操作描述
     */
    private String operationDesc;

    /**
     * 请求方式：GET/POST/PUT/DELETE
     */
    private String requestMethod;

    /**
     * 请求URL
     */
    private String requestUrl;

    /**
     * 请求参数（JSON格式）
     */
    private String requestParams;

    /**
     * 响应数据（JSON格式，可选）
     */
    private String responseData;

    /**
     * 操作IP地址
     */
    private String ipAddress;

    /**
     * 用户代理信息
     */
    private String userAgent;

    /**
     * 操作结果：1成功 0失败
     */
    private Integer operationResult;

    /**
     * 操作结果描述
     */
    private String operationResultDesc;

    /**
     * 错误信息（操作失败时记录）
     */
    private String errorMessage;

    /**
     * 执行耗时（毫秒）
     */
    private Integer executionTime;

    /**
     * 操作时间
     */
    private LocalDateTime createdAt;
}