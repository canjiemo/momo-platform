package com.seer.fitness.system.service;

import com.seer.fitness.system.dto.UserCreateRequest;
import com.seer.fitness.system.dto.UserDTO;
import com.seer.fitness.system.dto.UserQueryParam;
import com.seer.fitness.system.dto.UserUpdateRequest;
import com.seer.fitness.system.entity.SysUser;
import io.github.canjiemo.mycommon.pager.Pager;

import java.util.List;

/**
 * 用户管理服务接口
 * 提供用户的增删改查、权限管理、密码管理等功能
 *
 * @author seer-fitness
 */
public interface IUserService {

    /**
     * 分页查询用户
     * 支持复杂查询条件、分页、排序
     *
     * @param param 查询参数
     * @param pager 分页信息
     * @return 分页结果
     */
    Pager<UserDTO> search(UserQueryParam param, Pager<UserDTO> pager);

    /**
     * 根据ID获取用户详情
     * 包含用户基本信息和角色信息
     *
     * @param id 用户ID
     * @return 用户详情
     */
    UserDTO getById(Long id);

    /**
     * 创建用户
     * 包括密码加密、角色分配等操作
     *
     * @param request 创建请求参数
     */
    void create(UserCreateRequest request);

    /**
     * 更新用户信息
     *
     * @param request 更新请求参数
     */
    void update(UserUpdateRequest request);

    /**
     * 批量删除用户（逻辑删除）
     *
     * @param ids 用户ID数组
     */
    void delete(String[] ids);

    /**
     * 个人修改密码
     * 需要验证当前密码
     *
     * @param currentUserId 当前用户ID
     * @param currentPassword 当前密码
     * @param newPassword 新密码
     */
    void changePassword(Long currentUserId, String currentPassword, String newPassword);

    /**
     * 管理员初始化密码
     * 将用户密码重置为系统配置的初始密码
     *
     * @param userId 用户ID
     */
    void initPassword(Long userId);

    /**
     * 管理员重置密码
     * 将用户密码重置为指定密码
     *
     * @param userId 用户ID
     * @param newPassword 新密码
     */
    void resetPassword(Long userId, String newPassword);

    /**
     * 获取当前用户完整信息（包含角色和菜单权限）
     * 用于个人信息接口
     *
     * @return 用户完整信息
     */
    UserDTO getCurrentUserProfile();

    /**
     * 查询平台用户列表（tenant_id=null）
     * 仅供平台管理员使用
     *
     * @return 平台用户列表
     */
    List<UserDTO> listPlatformUsers();

    SysUser findByUsernameAndTenantId(String username, Long tenantId);
}