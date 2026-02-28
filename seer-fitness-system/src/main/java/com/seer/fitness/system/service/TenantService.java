package com.seer.fitness.system.service;

import com.google.common.collect.Maps;
import com.seer.fitness.system.config.PasswordPolicyConfig;
import com.seer.fitness.system.dto.TenantCreateRequest;
import com.seer.fitness.system.dto.TenantDTO;
import com.seer.fitness.system.dto.TenantQueryParam;
import com.seer.fitness.system.dto.TenantUpdateRequest;
import com.seer.fitness.system.entity.SysTenant;
import com.seer.fitness.system.entity.SysUser;
import com.seer.fitness.system.enums.TenantStatus;
import io.github.mocanjie.base.mycommon.exception.BusinessException;
import io.github.mocanjie.base.mycommon.pager.Pager;
import io.github.mocanjie.base.myjpa.service.impl.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 租户管理服务（简化版 - tenant_id 模式）
 * 租户创建只需在 sys_tenant 插入记录，无需创建 Schema
 */
@Service
@Slf4j
public class TenantService extends BaseServiceImpl implements ITenantService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${myjpa.tenant.enabled:false}")
    private boolean tenantEnabled;

    @Autowired
    private PasswordPolicyConfig passwordConfig;

    @Override
    public Pager<TenantDTO> search(TenantQueryParam param, Pager pager) {
        Map<String, Object> queryMap = Maps.newHashMap();

        String sql = "SELECT id, tenant_code, tenant_name, contact_phone, contact_email, " +
                    "address, description, status, activated_at, expired_at, " +
                    "max_users, created_at, updated_at, created_by, updated_by " +
                    "FROM sys_tenant";

        List<String> conditions = new ArrayList<>();
        conditions.add("delete_flag = 0");

        if (StringUtils.hasText(param.getTenantCode())) {
            conditions.add("tenant_code LIKE :tenantCode");
            queryMap.put("tenantCode", "%" + param.getTenantCode() + "%");
        }

        if (StringUtils.hasText(param.getTenantName())) {
            conditions.add("tenant_name LIKE :tenantName");
            queryMap.put("tenantName", "%" + param.getTenantName() + "%");
        }

        if (param.getStatus() != null) {
            conditions.add("status = :status");
            queryMap.put("status", param.getStatus());
        }

        if (StringUtils.hasText(param.getContactPhone())) {
            conditions.add("contact_phone LIKE :contactPhone");
            queryMap.put("contactPhone", "%" + param.getContactPhone() + "%");
        }

        sql += " WHERE " + String.join(" AND ", conditions) + " ORDER BY created_at DESC";

        Pager<TenantDTO> result = baseDao.queryPageForSql(sql, queryMap, pager, TenantDTO.class);

        if (result.getPageData() != null) {
            result.getPageData().forEach(this::enrichTenantDTO);
        }

        return result;
    }

    @Override
    public TenantDTO getById(Long id) {
        if (id == null) throw new BusinessException("租户ID不能为空");

        SysTenant tenant = baseDao.queryById(id, SysTenant.class);
        if (tenant == null) return null;

        TenantDTO dto = convertToDTO(tenant);
        enrichTenantDTO(dto);
        return dto;
    }

    @Override
    public TenantDTO getByCode(String tenantCode) {
        if (!StringUtils.hasText(tenantCode)) throw new BusinessException("租户编码不能为空");

        String sql = "SELECT * FROM sys_tenant WHERE tenant_code = :tenantCode AND delete_flag = 0";
        Map<String, Object> params = Maps.newHashMap();
        params.put("tenantCode", tenantCode);

        SysTenant tenant = baseDao.querySingleForSql(sql, params, SysTenant.class);
        if (tenant == null) return null;

        TenantDTO dto = convertToDTO(tenant);
        enrichTenantDTO(dto);
        return dto;
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
        tenant.setStatus(TenantStatus.ACTIVE.getCode());
        tenant.setActivatedAt(LocalDateTime.now());
        tenant.setMaxUsers(request.getMaxUsers() != null ? request.getMaxUsers() : 1000);
        tenant.setDeleteFlag(0);
        tenant.setCreatedAt(LocalDateTime.now());
        tenant.setUpdatedAt(LocalDateTime.now());

        if (StringUtils.hasText(request.getExpiredAt())) {
            try {
                tenant.setExpiredAt(LocalDateTime.parse(request.getExpiredAt(), DATE_TIME_FORMATTER));
            } catch (Exception e) {
                throw new BusinessException("过期时间格式不正确，应为：yyyy-MM-dd HH:mm:ss");
            }
        }

        baseDao.insertPO(tenant, true);

        log.info("租户创建成功: tenantCode={}, id={}", request.getTenantCode(), tenant.getId());

        // 租户模式开启时，自动创建租户管理员账号
        if (tenantEnabled) {
            createTenantAdmin(tenant, request.getAdminUsername());
        }
    }

    /**
     * 为新租户创建管理员用户
     * admin_flag=1 表示租户内的管理员（非平台超管）
     */
    private void createTenantAdmin(SysTenant tenant, String adminUsername) {
        String username = StringUtils.hasText(adminUsername)
                ? adminUsername
                : tenant.getTenantCode().toLowerCase() + "_admin";

        // 检查用户名是否已存在
        String checkSql = "SELECT COUNT(*) FROM sys_user WHERE username = :username AND delete_flag = 0";
        Map<String, Object> checkParams = Maps.newHashMap();
        checkParams.put("username", username);
        Long count = baseDao.querySingleForSql(checkSql, checkParams, Long.class);
        if (count != null && count > 0) {
            log.warn("租户管理员账号已存在，跳过创建: username={}", username);
            return;
        }

        String initialPassword = passwordConfig.getInitialPassword();
        int bcryptStrength = passwordConfig.getBackend().getBcryptStrength();
        String encodedPassword = BCrypt.hashpw(initialPassword, BCrypt.gensalt(bcryptStrength));

        SysUser admin = new SysUser();
        admin.setTenantId(tenant.getId());
        admin.setUsername(username);
        admin.setPassword(encodedPassword);
        admin.setRealName(tenant.getTenantName() + " 管理员");
        admin.setStatus(1);
        admin.setAdminFlag(1);
        admin.setDeleteFlag(0);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setUpdatedAt(LocalDateTime.now());

        baseDao.insertPO(admin, true);

        log.info("租户管理员创建成功: tenantId={}, username={}", tenant.getId(), username);
    }

    @Override
    @Transactional
    public void update(TenantUpdateRequest request) {
        SysTenant tenant = baseDao.queryById(request.getId(), SysTenant.class);
        if (tenant == null) throw new BusinessException("租户不存在");

        if (StringUtils.hasText(request.getTenantName())) tenant.setTenantName(request.getTenantName());
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

        tenant.setUpdatedAt(LocalDateTime.now());
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
        tenant.setUpdatedAt(LocalDateTime.now());
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
        tenant.setUpdatedAt(LocalDateTime.now());
        baseDao.updatePO(tenant);

        log.info("禁用租户成功: id={}, tenantCode={}", id, tenant.getTenantCode());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SysTenant tenant = baseDao.queryById(id, SysTenant.class);
        if (tenant == null) throw new BusinessException("租户不存在");

        baseDao.delByIds(SysTenant.class, new String[]{String.valueOf(id)});

        log.info("删除租户成功: id={}, tenantCode={}", id, tenant.getTenantCode());
    }

    @Override
    public boolean existsByCode(String tenantCode) {
        String sql = "SELECT COUNT(*) FROM sys_tenant WHERE tenant_code = :tenantCode AND delete_flag = 0";
        Map<String, Object> params = Maps.newHashMap();
        params.put("tenantCode", tenantCode);
        Long count = baseDao.querySingleForSql(sql, params, Long.class);
        return count != null && count > 0;
    }

    private TenantDTO convertToDTO(SysTenant tenant) {
        TenantDTO dto = new TenantDTO();
        dto.setId(tenant.getId());
        dto.setTenantCode(tenant.getTenantCode());
        dto.setTenantName(tenant.getTenantName());
        dto.setContactPhone(tenant.getContactPhone());
        dto.setContactEmail(tenant.getContactEmail());
        dto.setAddress(tenant.getAddress());
        dto.setDescription(tenant.getDescription());
        dto.setStatus(tenant.getStatus());
        dto.setActivatedAt(tenant.getActivatedAt());
        dto.setExpiredAt(tenant.getExpiredAt());
        dto.setMaxUsers(tenant.getMaxUsers());
        dto.setCreatedAt(tenant.getCreatedAt());
        dto.setUpdatedAt(tenant.getUpdatedAt());
        dto.setCreatedBy(tenant.getCreatedBy());
        dto.setUpdatedBy(tenant.getUpdatedBy());
        return dto;
    }

    private void enrichTenantDTO(TenantDTO dto) {
        TenantStatus status = TenantStatus.fromCode(dto.getStatus());
        if (status != null) dto.setStatusText(status.getDescription());
    }
}
