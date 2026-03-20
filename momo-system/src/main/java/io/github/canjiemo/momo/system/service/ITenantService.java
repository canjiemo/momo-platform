package io.github.canjiemo.momo.system.service;

import io.github.canjiemo.momo.system.dto.TenantCreateRequest;
import io.github.canjiemo.momo.system.dto.TenantDTO;
import io.github.canjiemo.momo.system.dto.TenantQueryParam;
import io.github.canjiemo.momo.system.dto.TenantUpdateRequest;
import io.github.canjiemo.mycommon.pager.Pager;

import java.util.List;

/**
 * 租户管理服务接口
 * 提供租户的增删改查、启用/禁用等基础功能
 *
 * 注意：阶段2只实现基本CRUD，Schema创建功能在阶段3实现
 *
 * @author canjiemo@gmail.com
 */
public interface ITenantService {

    /**
     * 分页查询租户
     * 支持复杂查询条件、分页、排序
     *
     * @param param 查询参数
     * @param pager 分页信息
     * @return 分页结果
     */
    Pager<TenantDTO> search(TenantQueryParam param, Pager<TenantDTO> pager);

    /**
     * 根据ID获取租户详情
     *
     * @param id 租户ID
     * @return 租户详情，不存在则返回null
     */
    TenantDTO getById(Long id);

    /**
     * 根据租户编码获取租户信息
     *
     * @param tenantCode 租户编码
     * @return 租户信息，不存在则返回null
     */
    TenantDTO getByCode(String tenantCode);

    /**
     * 创建租户
     * 只在 sys_tenant 插入记录，无需创建 Schema
     *
     * @param request 创建请求参数
     */
    void create(TenantCreateRequest request);

    /**
     * 更新租户信息
     *
     * @param request 更新请求参数
     */
    void update(TenantUpdateRequest request);

    /**
     * 启用租户
     *
     * @param id 租户ID
     */
    void enable(Long id);

    /**
     * 禁用租户
     *
     * @param id 租户ID
     */
    void disable(Long id);

    /**
     * 删除租户（逻辑删除）
     * 注意：删除租户不会删除其Schema，需要手动处理
     *
     * @param id 租户ID
     */
    void delete(Long id);

    /**
     * 检查租户编码是否已存在
     *
     * @param tenantCode 租户编码
     * @return true表示已存在
     */
    boolean existsByCode(String tenantCode);

    /**
     * 获取租户已分配的平台角色 ID 列表
     */
    List<Long> getTenantRoleIds(Long tenantId);

    /**
     * 为租户分配平台角色（全量替换）
     */
    void assignRoles(Long tenantId, List<Long> roleIds);
}
