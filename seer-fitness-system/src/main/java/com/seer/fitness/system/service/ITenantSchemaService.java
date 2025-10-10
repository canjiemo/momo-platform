package com.seer.fitness.system.service;

/**
 * 租户Schema管理服务接口
 * 负责租户Schema的创建、初始化和清理
 * <p>
 * 阶段3核心功能：
 * - 自动创建租户Schema
 * - 执行DDL脚本初始化表结构
 * - 插入基础数据（角色、菜单等）
 * - 创建租户管理员账号
 * - 记录初始化步骤日志
 * - 失败时自动回滚
 *
 * @author seer-fitness
 */
public interface ITenantSchemaService {

    /**
     * 创建Schema并初始化表结构和数据
     * 完整流程：
     * 1. 创建Schema (CREATE SCHEMA)
     * 2. 切换到新Schema (SET search_path)
     * 3. 执行DDL脚本（创建表）
     * 4. 执行默认数据脚本（角色、菜单）
     * 5. 创建管理员账号
     * 6. 记录每个步骤的日志
     * 7. 失败时自动回滚（DROP SCHEMA CASCADE）
     *
     * @param tenantId       租户ID
     * @param schemaName     Schema名称（例如：school_001）
     * @param adminUsername  管理员用户名
     * @param adminRealName  管理员真实姓名
     * @param adminPassword  管理员密码（明文，方法内部会加密）
     * @throws Exception 创建失败时抛出异常
     */
    void createSchemaAndInitTables(Long tenantId, String schemaName,
                                   String adminUsername, String adminRealName, String adminPassword) throws Exception;

    /**
     * 删除Schema（慎用！会删除所有数据）
     * 用于回滚或删除租户时使用
     *
     * @param schemaName Schema名称
     * @throws Exception 删除失败时抛出异常
     */
    void dropSchema(String schemaName) throws Exception;

    /**
     * 检查Schema是否存在
     *
     * @param schemaName Schema名称
     * @return true表示存在，false表示不存在
     */
    boolean schemaExists(String schemaName);

    /**
     * 验证Schema完整性
     * 检查Schema中的表结构是否完整
     *
     * @param schemaName Schema名称
     * @return true表示完整，false表示不完整
     */
    boolean validateSchema(String schemaName);
}
