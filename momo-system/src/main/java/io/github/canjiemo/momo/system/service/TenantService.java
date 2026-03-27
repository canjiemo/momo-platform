package io.github.canjiemo.momo.system.service;

import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import io.github.canjiemo.momo.framework.utils.PasswordUtil;
import io.github.canjiemo.momo.system.constants.ConfigKeys;
import io.github.canjiemo.momo.system.dto.TenantCreateRequest;
import io.github.canjiemo.momo.system.dto.TenantDTO;
import io.github.canjiemo.momo.system.dto.TenantQueryParam;
import io.github.canjiemo.momo.system.dto.TenantUpdateRequest;
import io.github.canjiemo.momo.system.entity.SysTenant;
import io.github.canjiemo.momo.system.entity.SysTenantRole;
import io.github.canjiemo.momo.system.entity.SysUser;
import io.github.canjiemo.momo.system.enums.TenantStatus;
import io.github.canjiemo.momo.system.utils.ConfigUtil;
import io.github.canjiemo.mycommon.exception.BusinessException;
import io.github.canjiemo.mycommon.pager.Pager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 租户管理服务（简化版 - tenant_id 模式）
 * 租户创建只需在 sys_tenant 插入记录，无需创建 Schema
 */
@Service
@Slf4j
public class TenantService extends BaseServiceImpl implements ITenantService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private PasswordUtil passwordUtil;

    @Value("${myjdbc.tenant.enabled:false}")
    private boolean tenantEnabled;

    @Override
    public Pager<TenantDTO> search(TenantQueryParam param, Pager<TenantDTO> pager) {
        return lambdaQuery(SysTenant.class, TenantDTO.class)
                .like(SysTenant::getTenantCode, param.getTenantCode())
                .like(SysTenant::getTenantName, param.getTenantName())
                .eq(SysTenant::getStatus, param.getStatus())
                .like(SysTenant::getContactPhone, param.getContactPhone())
                .orderByDesc(SysTenant::getCreateTime)
                .page(pager);
    }

    @Override
    public TenantDTO getById(Long id) {
        if (id == null) throw new BusinessException("租户ID不能为空");

        SysTenant tenant = baseDao.queryById(id, SysTenant.class);
        if (tenant == null) return null;

        return convertToDTO(tenant);
    }

    @Override
    public TenantDTO getByCode(String tenantCode) {
        if (!StringUtils.hasText(tenantCode)) throw new BusinessException("租户编码不能为空");

        SysTenant tenant = lambdaQuery(SysTenant.class)
                .eq(SysTenant::getTenantCode, tenantCode)
                .one();
        if (tenant == null) return null;

        return convertToDTO(tenant);
    }

    /**
     * 创建租户（简化版）
     * 只在 sys_tenant 插入记录，无需创建 Schema
     */
    @Override
    @Transactional
    public void create(TenantCreateRequest request) {
        if (existsByCode(request.getTenantCode())) {
            throw new BusinessException("租户编码已存在：" + request.getTenantCode());
        }

        SysTenant tenant = new SysTenant();
        tenant.setTenantCode(request.getTenantCode());
        tenant.setTenantName(request.getTenantName());
        tenant.setContactPhone(request.getContactPhone());
        tenant.setContactEmail(request.getContactEmail());
        tenant.setAddress(request.getAddress());
        tenant.setDescription(request.getDescription());
        tenant.setRealName(request.getRealName());
        tenant.setStatus(TenantStatus.ACTIVE.getCode());
        tenant.setActivatedAt(LocalDateTime.now());
        tenant.setMaxUsers(request.getMaxUsers() != null ? request.getMaxUsers() : 1000);
        tenant.setDeleteFlag(0);

        if (StringUtils.hasText(request.getExpiredAt())) {
            try {
                tenant.setExpiredAt(LocalDateTime.parse(request.getExpiredAt(), DATE_TIME_FORMATTER));
            } catch (Exception e) {
                throw new BusinessException("过期时间格式不正确，应为：yyyy-MM-dd HH:mm:ss");
            }
        }

        baseDao.insertPO(tenant, true);

        log.info("租户创建成功: tenantCode={}, id={}", request.getTenantCode(), tenant.getId());

        // 保存租户-平台角色映射
        for (Long roleId : request.getRoleIds()) {
            SysTenantRole tenantRole = new SysTenantRole();
            tenantRole.setTenantId(tenant.getId());
            tenantRole.setRoleId(roleId);
            baseDao.insertPO(tenantRole, true);
        }
        log.info("租户角色映射创建成功: tenantId={}, roleIds={}", tenant.getId(), request.getRoleIds());

        // 租户模式开启时，自动创建租户管理员账号
        if (tenantEnabled) {
            createTenantAdmin(tenant);
        }
    }

    /**
     * 为新租户创建管理员用户
     * - username 使用 tenant_name（管理员登录账号）
     * - real_name 使用 sys_tenant.real_name（学校中文名称）
     * - admin_flag=1 表示租户内管理员（非平台超管）
     * - 菜单权限由 sys_tenant_role 动态决定，无需分配具体角色
     */
    private void createTenantAdmin(SysTenant tenant) {
        String username = tenant.getTenantName();

        // 检查该租户下用户名是否已存在（tenant_id + username 联合唯一）
        boolean exists = lambdaQuery(SysUser.class)
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getTenantId, tenant.getId())
                .exists();
        if (exists) {
            throw new BusinessException("租户管理员账号已存在，请检查数据一致性: username=" + username);
        }

        String initialPassword = ConfigUtil.getString(ConfigKeys.PASSWORD_INITIAL, "Aa123456!");
        String encodedPassword = passwordUtil.encryptPassword(initialPassword);

        SysUser admin = new SysUser();
        admin.setTenantId(tenant.getId());
        admin.setUsername(username);
        admin.setPassword(encodedPassword);
        admin.setRealName(tenant.getRealName());
        admin.setStatus(1);
        admin.setAdminFlag(1);
        admin.setDeleteFlag(0);

        baseDao.insertPO(admin, true);
        log.info("租户管理员创建成功: tenantId={}, username={}", tenant.getId(), username);
    }

    /**
     * 获取租户已分配的平台角色 ID 列表
     */
    @Override
    public List<Long> getTenantRoleIds(Long tenantId) {
        return lambdaQuery(SysTenantRole.class)
                .eq(SysTenantRole::getTenantId, tenantId)
                .list()
                .stream()
                .map(SysTenantRole::getRoleId)
                .toList();
    }

    /**
     * 为租户分配平台角色（全量替换）
     */
    @Override
    @Transactional
    public void assignRoles(Long tenantId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new BusinessException("必须为租户分配至少一个平台角色");
        }

        SysTenant tenant = baseDao.queryById(tenantId, SysTenant.class);
        if (tenant == null) throw new BusinessException("租户不存在");

        // 删除旧的映射（SysTenantRole 无 deleteFlag，myjpa 执行物理删除）
        List<SysTenantRole> existing = lambdaQuery(SysTenantRole.class)
                .eq(SysTenantRole::getTenantId, tenantId)
                .list();
        for (SysTenantRole old : existing) {
            baseDao.delPO(old);
        }

        // 插入新的映射
        for (Long roleId : roleIds) {
            SysTenantRole tenantRole = new SysTenantRole();
            tenantRole.setTenantId(tenantId);
            tenantRole.setRoleId(roleId);
            baseDao.insertPO(tenantRole, true);
        }
        log.info("租户角色分配成功: tenantId={}, roleIds={}", tenantId, roleIds);
    }

    @Override
    @Transactional
    public void update(TenantUpdateRequest request) {
        SysTenant tenant = baseDao.queryById(request.getId(), SysTenant.class);
        if (tenant == null) throw new BusinessException("租户不存在");

        // tenant_name 创建后不可修改
        if (StringUtils.hasText(request.getRealName())) tenant.setRealName(request.getRealName());
        if (StringUtils.hasText(request.getContactPhone())) tenant.setContactPhone(request.getContactPhone());
        if (StringUtils.hasText(request.getContactEmail())) tenant.setContactEmail(request.getContactEmail());
        if (StringUtils.hasText(request.getAddress())) tenant.setAddress(request.getAddress());
        if (StringUtils.hasText(request.getDescription())) tenant.setDescription(request.getDescription());
        if (request.getMaxUsers() != null) tenant.setMaxUsers(request.getMaxUsers());

        if (StringUtils.hasText(request.getExpiredAt())) {
            try {
                tenant.setExpiredAt(LocalDateTime.parse(request.getExpiredAt(), DATE_TIME_FORMATTER));
            } catch (Exception e) {
                throw new BusinessException("过期时间格式不正确，应为：yyyy-MM-dd HH:mm:ss");
            }
        }

        baseDao.updatePO(tenant);

        log.info("更新租户成功: id={}", request.getId());
    }

    @Override
    @Transactional
    public void enable(Long id) {
        SysTenant tenant = baseDao.queryById(id, SysTenant.class);
        if (tenant == null) throw new BusinessException("租户不存在");
        if (TenantStatus.ACTIVE.getCode().equals(tenant.getStatus())) throw new BusinessException("租户已是启用状态");

        tenant.setStatus(TenantStatus.ACTIVE.getCode());
        if (tenant.getActivatedAt() == null) tenant.setActivatedAt(LocalDateTime.now());
        baseDao.updatePO(tenant);

        log.info("启用租户成功: id={}, tenantCode={}", id, tenant.getTenantCode());
    }

    @Override
    @Transactional
    public void disable(Long id) {
        SysTenant tenant = baseDao.queryById(id, SysTenant.class);
        if (tenant == null) throw new BusinessException("租户不存在");
        if (TenantStatus.DISABLED.getCode().equals(tenant.getStatus())) throw new BusinessException("租户已是禁用状态");

        tenant.setStatus(TenantStatus.DISABLED.getCode());
        baseDao.updatePO(tenant);

        log.info("禁用租户成功: id={}, tenantCode={}", id, tenant.getTenantCode());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SysTenant tenant = baseDao.queryById(id, SysTenant.class);
        if (tenant == null) throw new BusinessException("租户不存在");

        baseDao.delByIds(SysTenant.class, String.valueOf(id));

        log.info("删除租户成功: id={}, tenantCode={}", id, tenant.getTenantCode());
    }

    @Override
    public boolean existsByCode(String tenantCode) {
        return lambdaQuery(SysTenant.class)
                .eq(SysTenant::getTenantCode, tenantCode)
                .exists();
    }

    private TenantDTO convertToDTO(SysTenant tenant) {
        TenantDTO dto = new TenantDTO();
        BeanUtils.copyProperties(tenant, dto);
        return dto;
    }
}
