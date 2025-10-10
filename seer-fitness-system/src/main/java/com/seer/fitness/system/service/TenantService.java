package com.seer.fitness.system.service;

import com.google.common.collect.Maps;
import com.seer.fitness.system.dto.TenantCreateRequest;
import com.seer.fitness.system.dto.TenantDTO;
import com.seer.fitness.system.dto.TenantQueryParam;
import com.seer.fitness.system.dto.TenantUpdateRequest;
import com.seer.fitness.system.entity.SysTenant;
import com.seer.fitness.system.enums.TenantStatus;
import io.github.mocanjie.base.mycommon.exception.BusinessException;
import io.github.mocanjie.base.mycommon.pager.Pager;
import io.github.mocanjie.base.myjpa.service.impl.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 租户管理服务实现
 * 阶段2：实现基本CRUD操作
 * 阶段3：将添加Schema自动创建功能
 *
 * @author seer-fitness
 */
@Service
@Slf4j
public class TenantService extends BaseServiceImpl implements ITenantService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private ITenantSchemaService tenantSchemaService;

    /**
     * 分页查询租户
     */
    @Override
    public Pager<TenantDTO> search(TenantQueryParam param, Pager pager) {
        Map<String, Object> queryMap = Maps.newHashMap();

        String sql = "SELECT id, tenant_code, tenant_name, schema_name, " +
                    "admin_username, admin_real_name, contact_phone, contact_email, " +
                    "address, description, status, activated_at, expired_at, " +
                    "max_users, max_storage_gb, created_at, updated_at, created_by, updated_by " +
                    "FROM public.sys_tenant";

        List<String> conditions = new ArrayList<>();

        if (StringUtils.hasText(param.getTenantCode())) {
            conditions.add("tenant_code LIKE :tenantCode");
            queryMap.put("tenantCode", "%" + param.getTenantCode() + "%");
        }

        if (StringUtils.hasText(param.getTenantName())) {
            conditions.add("tenant_name LIKE :tenantName");
            queryMap.put("tenantName", "%" + param.getTenantName() + "%");
        }

        if (StringUtils.hasText(param.getSchemaName())) {
            conditions.add("schema_name = :schemaName");
            queryMap.put("schemaName", param.getSchemaName());
        }

        if (param.getStatus() != null) {
            conditions.add("status = :status");
            queryMap.put("status", param.getStatus());
        }

        if (StringUtils.hasText(param.getAdminUsername())) {
            conditions.add("admin_username LIKE :adminUsername");
            queryMap.put("adminUsername", "%" + param.getAdminUsername() + "%");
        }

        if (StringUtils.hasText(param.getContactPhone())) {
            conditions.add("contact_phone LIKE :contactPhone");
            queryMap.put("contactPhone", "%" + param.getContactPhone() + "%");
        }

        if (!conditions.isEmpty()) {
            sql += " WHERE " + String.join(" AND ", conditions);
        }

        // 排序
        String sortField = StringUtils.hasText(param.getSortField()) ? param.getSortField() : "created_at";
        String sortOrder = "asc".equalsIgnoreCase(param.getSortOrder()) ? "ASC" : "DESC";
        sql += " ORDER BY " + sortField + " " + sortOrder;

        log.info("租户分页查询SQL: {}", sql);

        Pager<TenantDTO> result = baseDao.queryPageForSqlWithDeleteCondition(sql, queryMap, pager, TenantDTO.class);

        // 添加状态文本
        if (result.getPageData() != null && !result.getPageData().isEmpty()) {
            for (TenantDTO dto : result.getPageData()) {
                enrichTenantDTO(dto);
            }
        }

        return result;
    }

    /**
     * 根据ID获取租户详情
     */
    @Override
    public TenantDTO getById(Long id) {
        if (id == null) {
            throw new BusinessException("租户ID不能为空");
        }

        SysTenant tenant = baseDao.queryByIdWithDeleteCondition(id, SysTenant.class);
        if (tenant == null) {
            return null;
        }

        TenantDTO dto = convertToDTO(tenant);
        enrichTenantDTO(dto);
        return dto;
    }

    /**
     * 根据租户编码获取租户信息
     */
    @Override
    public TenantDTO getByCode(String tenantCode) {
        if (!StringUtils.hasText(tenantCode)) {
            throw new BusinessException("租户编码不能为空");
        }

        String sql = "SELECT * FROM public.sys_tenant WHERE tenant_code = :tenantCode";
        Map<String, Object> params = Maps.newHashMap();
        params.put("tenantCode", tenantCode);

        SysTenant tenant = baseDao.querySingleForSqlWithDeleteCondition(sql, params, SysTenant.class);
        if (tenant == null) {
            return null;
        }

        TenantDTO dto = convertToDTO(tenant);
        enrichTenantDTO(dto);
        return dto;
    }

    /**
     * 根据Schema名称获取租户信息
     */
    @Override
    public TenantDTO getBySchemaName(String schemaName) {
        if (!StringUtils.hasText(schemaName)) {
            throw new BusinessException("Schema名称不能为空");
        }

        String sql = "SELECT * FROM public.sys_tenant WHERE schema_name = :schemaName";
        Map<String, Object> params = Maps.newHashMap();
        params.put("schemaName", schemaName);

        SysTenant tenant = baseDao.querySingleForSqlWithDeleteCondition(sql, params, SysTenant.class);
        if (tenant == null) {
            return null;
        }

        TenantDTO dto = convertToDTO(tenant);
        enrichTenantDTO(dto);
        return dto;
    }

    /**
     * 创建租户
     * 阶段3：创建租户记录 + 自动创建Schema并初始化
     * 完整流程：
     * 1. 创建租户记录（状态：待激活）
     * 2. 自动创建Schema并初始化表结构和数据
     * 3. 更新租户状态为正常（已激活）
     * 4. 失败时自动回滚
     */
    @Override
    @Transactional(readOnly = false)
    public void create(TenantCreateRequest request) {
        // 检查租户编码是否已存在
        if (existsByCode(request.getTenantCode())) {
            throw new BusinessException("租户编码已存在：" + request.getTenantCode());
        }

        // 检查Schema名称是否已存在
        if (existsBySchemaName(request.getSchemaName())) {
            throw new BusinessException("Schema名称已存在：" + request.getSchemaName());
        }

        // 检查Schema是否在数据库中已存在
        if (tenantSchemaService.schemaExists(request.getSchemaName())) {
            throw new BusinessException("数据库中已存在该Schema：" + request.getSchemaName());
        }

        // 创建租户实体
        SysTenant tenant = new SysTenant();
        tenant.setTenantCode(request.getTenantCode());
        tenant.setTenantName(request.getTenantName());
        tenant.setSchemaName(request.getSchemaName());
        tenant.setAdminUsername(request.getAdminUsername());
        tenant.setAdminRealName(request.getAdminRealName());
        tenant.setContactPhone(request.getContactPhone());
        tenant.setContactEmail(request.getContactEmail());
        tenant.setAddress(request.getAddress());
        tenant.setDescription(request.getDescription());
        tenant.setStatus(TenantStatus.PENDING.getCode()); // 待激活状态
        tenant.setMaxUsers(request.getMaxUsers() != null ? request.getMaxUsers() : 1000);
        tenant.setMaxStorageGb(request.getMaxStorageGb() != null ? request.getMaxStorageGb() : 100);
        tenant.setDeleteFlag(0);
        tenant.setCreatedAt(LocalDateTime.now());
        tenant.setUpdatedAt(LocalDateTime.now());

        // 解析过期时间
        if (StringUtils.hasText(request.getExpiredAt())) {
            try {
                tenant.setExpiredAt(LocalDateTime.parse(request.getExpiredAt(), DATE_TIME_FORMATTER));
            } catch (Exception e) {
                throw new BusinessException("过期时间格式不正确，应为：yyyy-MM-dd HH:mm:ss");
            }
        }

        // 先插入租户记录（待激活状态）
        baseDao.insertPO(tenant, true);

        log.info("租户记录创建成功（待激活状态）: tenantCode={}, schemaName={}, id={}",
                request.getTenantCode(), request.getSchemaName(), tenant.getId());

        // 自动创建Schema并初始化
        try {
            // 生成默认管理员密码（使用租户编码作为初始密码）
            String defaultPassword = request.getAdminPassword() != null ?
                    request.getAdminPassword() : request.getTenantCode() + "123456";

            // 调用Schema创建服务
            tenantSchemaService.createSchemaAndInitTables(
                    tenant.getId(),
                    request.getSchemaName(),
                    request.getAdminUsername(),
                    request.getAdminRealName(),
                    defaultPassword
            );

            // Schema创建成功，更新租户状态为正常（已激活）
            tenant.setStatus(TenantStatus.ACTIVE.getCode());
            tenant.setActivatedAt(LocalDateTime.now());
            tenant.setUpdatedAt(LocalDateTime.now());
            baseDao.updatePO(tenant);

            log.info("租户Schema初始化成功，租户已激活: tenantCode={}, schemaName={}",
                    request.getTenantCode(), request.getSchemaName());

        } catch (Exception e) {
            // Schema创建失败，删除租户记录（回滚）
            log.error("Schema创建失败，删除租户记录: tenantId={}, schemaName={}",
                    tenant.getId(), request.getSchemaName(), e);

            try {
                baseDao.delByIds(SysTenant.class, new String[]{String.valueOf(tenant.getId())});
            } catch (Exception deleteError) {
                log.error("删除租户记录失败: tenantId={}", tenant.getId(), deleteError);
            }

            throw new BusinessException("租户创建失败：Schema初始化失败 - " + e.getMessage(), e);
        }
    }

    /**
     * 更新租户信息
     */
    @Override
    @Transactional(readOnly = false)
    public void update(TenantUpdateRequest request) {
        // 查询现有租户
        SysTenant tenant = baseDao.queryByIdWithDeleteCondition(request.getId(), SysTenant.class);
        if (tenant == null) {
            throw new BusinessException("租户不存在");
        }

        // 更新租户信息（不允许修改租户编码和Schema名称）
        if (StringUtils.hasText(request.getTenantName())) {
            tenant.setTenantName(request.getTenantName());
        }
        if (StringUtils.hasText(request.getAdminRealName())) {
            tenant.setAdminRealName(request.getAdminRealName());
        }
        if (StringUtils.hasText(request.getContactPhone())) {
            tenant.setContactPhone(request.getContactPhone());
        }
        if (StringUtils.hasText(request.getContactEmail())) {
            tenant.setContactEmail(request.getContactEmail());
        }
        if (StringUtils.hasText(request.getAddress())) {
            tenant.setAddress(request.getAddress());
        }
        if (StringUtils.hasText(request.getDescription())) {
            tenant.setDescription(request.getDescription());
        }
        if (request.getMaxUsers() != null) {
            tenant.setMaxUsers(request.getMaxUsers());
        }
        if (request.getMaxStorageGb() != null) {
            tenant.setMaxStorageGb(request.getMaxStorageGb());
        }

        // 解析过期时间
        if (StringUtils.hasText(request.getExpiredAt())) {
            try {
                tenant.setExpiredAt(LocalDateTime.parse(request.getExpiredAt(), DATE_TIME_FORMATTER));
            } catch (Exception e) {
                throw new BusinessException("过期时间格式不正确，应为：yyyy-MM-dd HH:mm:ss");
            }
        }

        tenant.setUpdatedAt(LocalDateTime.now());

        baseDao.updatePO(tenant);

        log.info("更新租户成功: id={}, tenantName={}", request.getId(), request.getTenantName());
    }

    /**
     * 启用租户
     */
    @Override
    @Transactional(readOnly = false)
    public void enable(Long id) {
        SysTenant tenant = baseDao.queryByIdWithDeleteCondition(id, SysTenant.class);
        if (tenant == null) {
            throw new BusinessException("租户不存在");
        }

        if (TenantStatus.ACTIVE.getCode().equals(tenant.getStatus())) {
            throw new BusinessException("租户已是启用状态");
        }

        tenant.setStatus(TenantStatus.ACTIVE.getCode());
        if (tenant.getActivatedAt() == null) {
            tenant.setActivatedAt(LocalDateTime.now());
        }
        tenant.setUpdatedAt(LocalDateTime.now());

        baseDao.updatePO(tenant);

        log.info("启用租户成功: id={}, tenantCode={}", id, tenant.getTenantCode());
    }

    /**
     * 禁用租户
     */
    @Override
    @Transactional(readOnly = false)
    public void disable(Long id) {
        SysTenant tenant = baseDao.queryByIdWithDeleteCondition(id, SysTenant.class);
        if (tenant == null) {
            throw new BusinessException("租户不存在");
        }

        if (TenantStatus.DISABLED.getCode().equals(tenant.getStatus())) {
            throw new BusinessException("租户已是禁用状态");
        }

        tenant.setStatus(TenantStatus.DISABLED.getCode());
        tenant.setUpdatedAt(LocalDateTime.now());

        baseDao.updatePO(tenant);

        log.info("禁用租户成功: id={}, tenantCode={}", id, tenant.getTenantCode());
    }

    /**
     * 删除租户（逻辑删除）
     */
    @Override
    @Transactional(readOnly = false)
    public void delete(Long id) {
        SysTenant tenant = baseDao.queryByIdWithDeleteCondition(id, SysTenant.class);
        if (tenant == null) {
            throw new BusinessException("租户不存在");
        }

        baseDao.delByIds(SysTenant.class, new String[]{String.valueOf(id)});

        log.warn("删除租户成功（逻辑删除，Schema未删除）: id={}, tenantCode={}, schemaName={}",
                id, tenant.getTenantCode(), tenant.getSchemaName());
    }

    /**
     * 检查租户编码是否已存在
     */
    @Override
    public boolean existsByCode(String tenantCode) {
        String sql = "SELECT COUNT(*) FROM public.sys_tenant WHERE tenant_code = :tenantCode";
        Map<String, Object> params = Maps.newHashMap();
        params.put("tenantCode", tenantCode);

        Long count = baseDao.querySingleForSqlWithDeleteCondition(sql, params, Long.class);
        return count != null && count > 0;
    }

    /**
     * 检查Schema名称是否已存在
     */
    @Override
    public boolean existsBySchemaName(String schemaName) {
        String sql = "SELECT COUNT(*) FROM public.sys_tenant WHERE schema_name = :schemaName";
        Map<String, Object> params = Maps.newHashMap();
        params.put("schemaName", schemaName);

        Long count = baseDao.querySingleForSqlWithDeleteCondition(sql, params, Long.class);
        return count != null && count > 0;
    }

    /**
     * 转换为DTO
     */
    private TenantDTO convertToDTO(SysTenant tenant) {
        TenantDTO dto = new TenantDTO();
        dto.setId(tenant.getId());
        dto.setTenantCode(tenant.getTenantCode());
        dto.setTenantName(tenant.getTenantName());
        dto.setSchemaName(tenant.getSchemaName());
        dto.setAdminUsername(tenant.getAdminUsername());
        dto.setAdminRealName(tenant.getAdminRealName());
        dto.setContactPhone(tenant.getContactPhone());
        dto.setContactEmail(tenant.getContactEmail());
        dto.setAddress(tenant.getAddress());
        dto.setDescription(tenant.getDescription());
        dto.setStatus(tenant.getStatus());
        dto.setActivatedAt(tenant.getActivatedAt());
        dto.setExpiredAt(tenant.getExpiredAt());
        dto.setMaxUsers(tenant.getMaxUsers());
        dto.setMaxStorageGb(tenant.getMaxStorageGb());
        dto.setCreatedAt(tenant.getCreatedAt());
        dto.setUpdatedAt(tenant.getUpdatedAt());
        dto.setCreatedBy(tenant.getCreatedBy());
        dto.setUpdatedBy(tenant.getUpdatedBy());
        return dto;
    }

    /**
     * 丰富DTO信息（添加状态文本等）
     */
    private void enrichTenantDTO(TenantDTO dto) {
        // 添加状态文本
        TenantStatus status = TenantStatus.fromCode(dto.getStatus());
        if (status != null) {
            dto.setStatusText(status.getDescription());
        }

        // TODO: 阶段6可以添加统计信息（当前用户数、存储占用等）
    }
}
