package com.seer.fitness.system.service;

import com.google.common.collect.Maps;
import com.seer.fitness.system.entity.SysOrganization;
import com.seer.fitness.system.entity.SysUser;
import com.seer.fitness.system.entity.SysUserRole;
import com.seer.fitness.system.config.PasswordPolicyConfig;
import com.seer.fitness.system.dto.*;
import com.seer.fitness.system.utils.PasswordUtil;
import com.seer.fitness.system.util.SecurityContextUtil;
import io.github.mocanjie.base.mycommon.exception.BusinessException;
import io.github.mocanjie.base.mycommon.pager.Pager;
import io.github.mocanjie.base.myjpa.service.impl.BaseServiceImpl;
import io.github.mocanjie.base.myjpa.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户管理服务实现
 * 提供用户的增删改查、权限管理、密码管理等功能
 *
 * @author seer-fitness
 */
@Service
@Slf4j
public class UserService extends BaseServiceImpl implements IUserService {

    @Autowired
    private PasswordUtil passwordUtil;

    @Autowired
    private PasswordPolicyConfig passwordConfig;

    @Autowired
    private MenuService menuService;

    /**
     * 分页查询用户
     * 支持复杂查询条件、分页、排序
     */
    @Override
    public Pager<UserDTO> search(UserQueryParam param, Pager pager) {
        Map<String, Object> queryMap = Maps.newHashMap();

        String sql = "SELECT u.id, u.username, u.real_name, u.status, u.admin_flag, " +
                    "u.user_type, u.org_id, u.create_time, u.update_time " +
                    "FROM sys_user u";

        List<String> conditions = new ArrayList<>();

        // 如果按角色查询，需要关联用户角色表
        if (param.getRoleId() != null) {
            sql += " INNER JOIN sys_user_role ur ON u.id = ur.user_id";
            conditions.add("ur.role_id = :roleId");
            queryMap.put("roleId", param.getRoleId());
        }

        if (StringUtils.hasText(param.getUsername())) {
            conditions.add("u.username LIKE :username");
            queryMap.put("username", "%" + param.getUsername() + "%");
        }

        if (StringUtils.hasText(param.getRealName())) {
            conditions.add("u.real_name LIKE :realName");
            queryMap.put("realName", "%" + param.getRealName() + "%");
        }

        if (param.getStatus() != null) {
            conditions.add("u.status = :status");
            queryMap.put("status", param.getStatus());
        }

        if (param.getAdminFlag() != null) {
            conditions.add("u.admin_flag = :adminFlag");
            queryMap.put("adminFlag", param.getAdminFlag());
        }

        // 平台管理员可按 tenantId 过滤特定租户数据
        boolean isPlatformAdmin = SecurityContextUtil.isPlatformAdmin();
        if (isPlatformAdmin && param.getTenantId() != null) {
            conditions.add("u.tenant_id = :tenantId");
            queryMap.put("tenantId", param.getTenantId());
        }

        if (!conditions.isEmpty()) {
            sql += " WHERE " + String.join(" AND ", conditions);
        }

        sql += " ORDER BY u.create_time DESC";

        log.info("用户分页查询SQL: {}", sql);

        final String finalSql = sql;
        Pager<UserDTO> result = (isPlatformAdmin && param.getTenantId() != null)
                ? TenantContext.withoutTenant(() -> baseDao.queryPageForSql(finalSql, queryMap, pager, UserDTO.class))
                : baseDao.queryPageForSql(finalSql, queryMap, pager, UserDTO.class);

        // 为每个用户查询角色信息
        if (result.getPageData() != null && !result.getPageData().isEmpty()) {
            for (UserDTO userDTO : result.getPageData()) {
                // 查询用户角色
                String rolesSql = "SELECT r.id, r.role_name, r.description " +
                                 "FROM sys_role r " +
                                 "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
                                 "WHERE ur.user_id = :userId";
                Map<String, Object> roleParams = Maps.newHashMap();
                roleParams.put("userId", userDTO.getId());

                List<RoleDTO> roles = baseDao.queryListForSql(rolesSql, roleParams, RoleDTO.class);
                userDTO.setRoles(roles);
            }
        }

        return result;
    }

    /**
     * 根据ID获取用户详情
     * 包含用户基本信息和角色信息
     */
    @Override
    public UserDTO getById(Long id) {
        if (id == null) {
            throw new BusinessException("用户ID不能为空");
        }

        // 查询用户基本信息
        SysUser user = baseDao.queryById(id, SysUser.class);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 转换为DTO
        UserDTO userDTO = convertToDTO(user);

        // 查询用户角色
        String rolesSql = "SELECT r.id, r.role_name, r.description " +
                         "FROM sys_role r " +
                         "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
                         "WHERE ur.user_id = :userId";
        Map<String, Object> roleParams = Maps.newHashMap();
        roleParams.put("userId", id);

        List<RoleDTO> roles = baseDao.queryListForSql(rolesSql, roleParams, RoleDTO.class);
        userDTO.setRoles(roles);

        return userDTO;
    }

    /**
     * 创建用户
     * 包括密码加密、角色分配等操作
     */
    @Override
    @Transactional(readOnly = false)
    public void create(UserCreateRequest request) {
        // 检查用户名是否已存在
        if (isUsernameExists(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }

        // 加密密码
        String encryptedPassword = passwordUtil.encryptPassword(request.getPassword());

        // 验证组织ID是否存在（如果提供了）
        if (request.getOrgId() != null) {
            SysOrganization org = baseDao.queryById(request.getOrgId(), SysOrganization.class);
            if (org == null) {
                throw new BusinessException("指定的组织不存在");
            }
        }

        // 创建用户
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(encryptedPassword);
        user.setRealName(request.getRealName());
        user.setStatus(request.getStatus());
        user.setOrgId(request.getOrgId());  // 设置组织ID（可为空）
        user.setUserType(request.getUserType() != null ? request.getUserType() : 0);  // 默认为运维人员
        user.setDeleteFlag(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        baseDao.insertPO(user, true);

        // 分配角色
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            assignRoles(user.getId(), request.getRoleIds());
        }

        log.info("创建用户成功: username={}, id={}", request.getUsername(), user.getId());
    }

    /**
     * 更新用户
     */
    @Transactional(readOnly = false)
    public void update(UserUpdateRequest request) {
        // 查询现有用户
        SysUser user = baseDao.queryById(request.getId(), SysUser.class);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 管理员账号只允许修改姓名
        if (user.getAdminFlag() != null && user.getAdminFlag() == 1) {
            user.setRealName(request.getRealName());
            user.setUpdateTime(LocalDateTime.now());
            baseDao.updatePO(user);
            log.info("更新管理员用户姓名成功: id={}, realName={}", request.getId(), request.getRealName());
            return;
        }

        // 验证组织ID是否存在（如果提供了）
        if (request.getOrgId() != null) {
            SysOrganization org = baseDao.queryById(request.getOrgId(), SysOrganization.class);
            if (org == null) {
                throw new BusinessException("指定的组织不存在");
            }
        }

        // 更新用户信息
        user.setRealName(request.getRealName());
        user.setStatus(request.getStatus());
        user.setOrgId(request.getOrgId());  // 更新组织ID（可为空）
        if (request.getUserType() != null) {
            user.setUserType(request.getUserType());  // 更新用户类型
        }
        user.setUpdateTime(LocalDateTime.now());

        baseDao.updatePO(user);

        // 重新分配角色
        if (request.getRoleIds() != null) {
            // 先删除现有角色
            removeUserRoles(request.getId());
            // 再分配新角色
            if (!request.getRoleIds().isEmpty()) {
                assignRoles(request.getId(), request.getRoleIds());
            }
        }

        log.info("更新用户成功: id={}, realName={}", request.getId(), request.getRealName());
    }

    /**
     * 批量删除用户（逻辑删除）
     */
    @Override
    @Transactional(readOnly = false)
    public void delete(String[] ids) {
        if (ids == null || ids.length == 0) {
            throw new BusinessException("删除的用户ID不能为空");
        }

        for (String id : ids) {
            if (!StringUtils.hasText(id)) {
                throw new BusinessException("用户ID不能为空");
            }

            Long userId = Long.valueOf(id);

            // 检查是否为超级管理员
            SysUser user = baseDao.queryById(userId, SysUser.class);
            if (user == null) {
                throw new BusinessException("用户不存在：ID=" + userId);
            }

            if (user.getAdminFlag() != null && user.getAdminFlag() == 1) {
                throw new BusinessException("不能删除超级管理员账号");
            }

            // 删除用户角色关联
            removeUserRoles(userId);
        }

        // 逻辑删除用户
        baseDao.delByIds(SysUser.class, ids);

        log.info("删除用户成功: ids={}", java.util.Arrays.toString(ids));
    }

    /**
     * 管理员重置密码
     * 将用户密码重置为指定密码
     */
    @Override
    @Transactional(readOnly = false)
    public void resetPassword(Long userId, String newPassword) {
        SysUser user = baseDao.queryById(userId, SysUser.class);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 检查新密码是否与旧密码相同
        if (passwordUtil.verifyPassword(newPassword, user.getPassword())) {
            throw new BusinessException("新密码不能与当前密码相同");
        }

        String encryptedPassword = passwordUtil.encryptPassword(newPassword);
        user.setPassword(encryptedPassword);
        user.setUpdateTime(LocalDateTime.now());

        baseDao.updatePO(user);

        log.info("重置密码成功: userId={}", userId);
    }

    /**
     * 个人修改密码
     * 需要验证当前密码
     */
    @Override
    @Transactional(readOnly = false)
    public void changePassword(Long currentUserId, String currentPassword, String newPassword) {
        SysUser user = baseDao.queryById(currentUserId, SysUser.class);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 验证当前密码
        if (!passwordUtil.verifyPassword(currentPassword, user.getPassword())) {
            throw new BusinessException("当前密码错误");
        }

        // 检查新密码是否与当前密码相同
        if (passwordUtil.verifyPassword(newPassword, user.getPassword())) {
            throw new BusinessException("新密码不能与当前密码相同");
        }

        // 加密并更新新密码
        String encryptedPassword = passwordUtil.encryptPassword(newPassword);
        user.setPassword(encryptedPassword);
        user.setUpdateTime(LocalDateTime.now());

        baseDao.updatePO(user);
        log.info("个人修改密码成功: userId={}", currentUserId);
    }

    /**
     * 管理员初始化密码
     * 将用户密码重置为系统配置的初始密码
     */
    @Override
    @Transactional(readOnly = false)
    public void initPassword(Long userId) {
        SysUser user = baseDao.queryById(userId, SysUser.class);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (user.getAdminFlag() != null && user.getAdminFlag() == 1) {
            throw new BusinessException("不能对管理员账号进行密码初始化");
        }

        // 获取配置的初始密码
        String initialPassword = passwordConfig.getInitialPassword();

        // 检查初始密码是否与当前密码相同
        if (passwordUtil.verifyPassword(initialPassword, user.getPassword())) {
            throw new BusinessException("用户密码已是初始密码，无需重复设置");
        }

        // 加密并设置初始密码
        String encryptedPassword = passwordUtil.encryptPassword(initialPassword);
        user.setPassword(encryptedPassword);
        user.setUpdateTime(LocalDateTime.now());

        baseDao.updatePO(user);
        log.info("管理员初始化密码成功: userId={}, initialPassword={}", userId, initialPassword);
    }

    /**
     * 根据用户名和租户ID查询用户（用于登录）
     * tenantId=NULL 表示平台管理员
     */
    public SysUser findByUsernameAndTenantId(String username, Long tenantId) {
        String sql;
        Map<String, Object> params = Maps.newHashMap();
        params.put("username", username);
        if (tenantId == null) {
            sql = "SELECT * FROM sys_user WHERE username = :username AND tenant_id IS NULL";
        } else {
            sql = "SELECT * FROM sys_user WHERE username = :username AND tenant_id = :tenantId";
            params.put("tenantId", tenantId);
        }
        return baseDao.querySingleForSql(sql, params, SysUser.class);
    }

    /**
     * 根据用户名查询用户
     */
    public SysUser findByUsername(String username) {
        String sql = "SELECT * FROM sys_user WHERE username = :username";
        Map<String, Object> params = Maps.newHashMap();
        params.put("username", username);
        return baseDao.querySingleForSql(sql, params, SysUser.class);
    }

    /**
     * 检查用户名是否已存在
     */
    private boolean isUsernameExists(String username) {
        return lambdaQuery(SysUser.class).eq(SysUser::getUsername, username).exists();
    }

    /**
     * 分配角色
     */
    private void assignRoles(Long userId, List<String> roleIds) {
        for (String roleId : roleIds) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(Long.valueOf(roleId));
            userRole.setCreateTime(LocalDateTime.now());

            baseDao.insertPO(userRole, true);
        }
    }

    /**
     * 移除用户角色关联
     */
    private void removeUserRoles(Long userId) {
        String sql = "SELECT * FROM sys_user_role WHERE user_id = :userId";
        Map<String, Object> params = Maps.newHashMap();
        params.put("userId", userId);

        List<SysUserRole> userRoles = baseDao.queryListForSql(sql, params, SysUserRole.class);
        for (SysUserRole userRole : userRoles) {
            baseDao.delPO(userRole);
        }
    }

    /**
     * 转换为DTO
     */
    private UserDTO convertToDTO(SysUser user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setTenantId(user.getTenantId());
        dto.setUsername(user.getUsername());
        dto.setRealName(user.getRealName());
        dto.setStatus(user.getStatus());
        dto.setAdminFlag(user.getAdminFlag());
        dto.setUserType(user.getUserType());
        dto.setOrgId(user.getOrgId());
        dto.setCreateTime(user.getCreateTime());
        dto.setUpdateTime(user.getUpdateTime());
        return dto;
    }

    /**
     * 获取当前用户完整信息（包含角色和菜单权限）
     * 用于个人信息接口
     */
    public UserDTO getCurrentUserProfile() {
        String currentUserId = SecurityContextUtil.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException("用户未登录");
        }

        // 转换ID类型
        Long userId;
        try {
            userId = Long.parseLong(currentUserId);
        } catch (NumberFormatException e) {
            throw new BusinessException("用户ID格式错误");
        }

        // 获取用户基本信息和角色
        UserDTO userDTO = getById(userId);

        // 获取用户菜单权限（扁平列表）
        List<MenuDTO> userMenus = menuService.getUserMenus(currentUserId);
        userDTO.setMenus(userMenus);

        // 获取用户权限字符串列表
        List<String> permissions = menuService.getUserPermissions(userId);
        userDTO.setPermissions(permissions);

        return userDTO;
    }

    /**
     * 查询平台用户列表（tenant_id=null）
     * 仅供平台管理员使用，显式指定 tenant_id IS NULL 确保只返回平台用户
     */
    @Override
    public List<UserDTO> listPlatformUsers() {
        String sql = "SELECT id, username, real_name, status, admin_flag, user_type, " +
                    "create_time, update_time FROM sys_user WHERE tenant_id IS NULL ORDER BY id";
        return baseDao.queryListForSql(sql, Maps.newHashMap(), UserDTO.class);
    }
}