package com.seer.fitness.system.service;

import com.google.common.collect.Maps;
import com.seer.fitness.system.dto.*;
import com.seer.fitness.system.entity.SysOrganization;
import io.github.mocanjie.base.mycommon.exception.BusinessException;
import io.github.mocanjie.base.mycommon.pager.Pager;
import io.github.mocanjie.base.myjpa.service.impl.BaseServiceImpl;
import io.github.mocanjie.base.myjpa.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 平台组织管理服务（只操作 tenant_id=NULL 的组织）
 * 管理平台自身的内部组织架构
 *
 * @author seer-fitness
 */
@Service
@Slf4j
public class PlatformOrganizationService extends BaseServiceImpl implements IPlatformOrganizationService {

    private static final String BASE_SELECT =
            "SELECT o.id, o.org_code, o.org_name, o.parent_id, " +
            "o.sort_order, o.leader_id, o.contact_phone, o.email, o.address, " +
            "o.description, o.status, o.created_at, o.updated_at, " +
            "u.real_name as leader_name, " +
            "COALESCE(child_count.cnt, 0) as children_count, " +
            "COALESCE(member_count.cnt, 0) as member_count " +
            "FROM sys_organization o " +
            "LEFT JOIN sys_user u ON o.leader_id = u.id AND u.delete_flag = 0 AND u.tenant_id IS NULL " +
            "LEFT JOIN (SELECT parent_id, COUNT(*) as cnt FROM sys_organization WHERE delete_flag = 0 AND tenant_id IS NULL GROUP BY parent_id) child_count ON o.id = child_count.parent_id " +
            "LEFT JOIN (SELECT org_id, COUNT(*) as cnt FROM sys_user WHERE delete_flag = 0 AND status = 1 AND tenant_id IS NULL GROUP BY org_id) member_count ON o.id = member_count.org_id " +
            "WHERE o.tenant_id IS NULL AND o.delete_flag = 0";

    @Override
    public Pager<OrganizationDTO> search(OrganizationQueryParam param, Pager pager) {
        Map<String, Object> queryMap = Maps.newHashMap();
        StringBuilder sql = new StringBuilder(BASE_SELECT);

        if (StringUtils.hasText(param.getOrgCode())) {
            sql.append(" AND o.org_code LIKE :orgCode");
            queryMap.put("orgCode", "%" + param.getOrgCode() + "%");
        }
        if (StringUtils.hasText(param.getOrgName())) {
            sql.append(" AND o.org_name LIKE :orgName");
            queryMap.put("orgName", "%" + param.getOrgName() + "%");
        }
        if (param.getParentId() != null) {
            sql.append(" AND o.parent_id = :parentId");
            queryMap.put("parentId", param.getParentId());
        }
        if (param.getStatus() != null) {
            sql.append(" AND o.status = :status");
            queryMap.put("status", param.getStatus());
        }
        sql.append(" ORDER BY o.sort_order ASC, o.created_at DESC");

        final String finalSql = sql.toString();
        return TenantContext.withoutTenant(() ->
                baseDao.queryPageForSql(finalSql, queryMap, pager, OrganizationDTO.class));
    }

    @Override
    public List<OrganizationTreeVO> getOrganizationTree() {
        String sql = BASE_SELECT + " AND o.status = 1 ORDER BY o.sort_order ASC";
        List<OrganizationDTO> allOrgs = TenantContext.withoutTenant(() ->
                baseDao.queryListForSql(sql, Maps.newHashMap(), OrganizationDTO.class));
        return buildTree(allOrgs, 0L);
    }

    @Override
    public List<OrganizationDTO> list() {
        String sql = BASE_SELECT + " AND o.status = 1 ORDER BY o.sort_order ASC, o.created_at DESC";
        return TenantContext.withoutTenant(() ->
                baseDao.queryListForSql(sql, Maps.newHashMap(), OrganizationDTO.class));
    }

    @Override
    public OrganizationDTO getById(Long id) {
        String sql = BASE_SELECT + " AND o.id = :id";
        Map<String, Object> params = Maps.newHashMap();
        params.put("id", id);
        OrganizationDTO dto = TenantContext.withoutTenant(() ->
                baseDao.querySingleForSql(sql, params, OrganizationDTO.class));
        if (dto == null) throw new BusinessException("平台组织不存在");
        return dto;
    }

    @Override
    @Transactional
    public void create(OrganizationCreateRequest request) {
        if (!isOrgCodeUnique(request.getOrgCode(), null)) {
            throw new BusinessException("组织编码已存在");
        }

        Long parentId = request.getParentId() != null ? request.getParentId() : 0L;
        if (parentId != 0L) {
            getPlatformOrgEntity(parentId); // 验证父组织存在且是平台组织
        }

        SysOrganization org = new SysOrganization();
        org.setTenantId(null);
        org.setOrgCode(request.getOrgCode());
        org.setOrgName(request.getOrgName());
        org.setParentId(parentId);
        org.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        org.setLeaderId(request.getLeaderId());
        org.setContactPhone(request.getContactPhone());
        org.setEmail(request.getEmail());
        org.setAddress(request.getAddress());
        org.setDescription(request.getDescription());
        org.setStatus(request.getStatus());
        org.setDeleteFlag(0);
        org.setCreatedAt(LocalDateTime.now());
        org.setUpdatedAt(LocalDateTime.now());

        TenantContext.withoutTenant(() -> {
            baseDao.insertPO(org, true);
            return null;
        });

        log.info("创建平台组织成功: orgCode={}, id={}", request.getOrgCode(), org.getId());
    }

    @Override
    @Transactional
    public void update(OrganizationUpdateRequest request) {
        SysOrganization org = getPlatformOrgEntity(request.getId());

        if (!isOrgCodeUnique(request.getOrgCode(), request.getId())) {
            throw new BusinessException("组织编码已存在");
        }

        Long parentId = request.getParentId() != null ? request.getParentId() : 0L;
        if (parentId != 0L) {
            if (parentId.equals(request.getId())) throw new BusinessException("不能将自己设为父组织");
            if (wouldCreateCircularReference(request.getId(), parentId)) {
                throw new BusinessException("不能将组织移动到其子组织下");
            }
            getPlatformOrgEntity(parentId);
        }
        org.setOrgCode(request.getOrgCode());
        org.setOrgName(request.getOrgName());
        org.setParentId(parentId);
        org.setSortOrder(request.getSortOrder());
        org.setLeaderId(request.getLeaderId());
        org.setContactPhone(request.getContactPhone());
        org.setEmail(request.getEmail());
        org.setAddress(request.getAddress());
        org.setDescription(request.getDescription());
        org.setStatus(request.getStatus());
        org.setUpdatedAt(LocalDateTime.now());

        TenantContext.withoutTenant(() -> {
            baseDao.updatePO(org);
            return null;
        });

        log.info("更新平台组织成功: id={}", request.getId());
    }

    @Override
    @Transactional
    public void delete(String[] ids) {
        if (ids == null || ids.length == 0) throw new BusinessException("删除的组织ID不能为空");

        for (String id : ids) {
            if (!StringUtils.hasText(id)) throw new BusinessException("组织ID不能为空");
            Long orgId = Long.valueOf(id);
            getPlatformOrgEntity(orgId);
            if (hasChildren(orgId)) throw new BusinessException("该组织存在子组织，无法删除");
            if (hasMembers(orgId)) throw new BusinessException("该组织存在关联用户，无法删除");
        }

        TenantContext.withoutTenant(() -> {
            baseDao.delByIds(SysOrganization.class, ids);
            return null;
        });

        log.info("删除平台组织成功: ids={}", Arrays.toString(ids));
    }

    private SysOrganization getPlatformOrgEntity(Long id) {
        String sql = "SELECT * FROM sys_organization WHERE id = :id AND tenant_id IS NULL AND delete_flag = 0";
        Map<String, Object> params = Maps.newHashMap();
        params.put("id", id);
        SysOrganization org = TenantContext.withoutTenant(() ->
                baseDao.querySingleForSql(sql, params, SysOrganization.class));
        if (org == null) throw new BusinessException("平台组织不存在");
        return org;
    }

    private boolean isOrgCodeUnique(String orgCode, Long excludeId) {
        if (!StringUtils.hasText(orgCode)) return true;
        String sql = "SELECT COUNT(*) FROM sys_organization WHERE org_code = :orgCode AND tenant_id IS NULL AND delete_flag = 0";
        Map<String, Object> params = Maps.newHashMap();
        params.put("orgCode", orgCode);
        if (excludeId != null) {
            sql += " AND id != :excludeId";
            params.put("excludeId", excludeId);
        }
        final String finalSql = sql;
        Long count = TenantContext.withoutTenant(() ->
                baseDao.querySingleForSql(finalSql, params, Long.class));
        return count == null || count == 0;
    }

    private boolean hasChildren(Long orgId) {
        String sql = "SELECT COUNT(*) FROM sys_organization WHERE parent_id = :parentId AND tenant_id IS NULL AND delete_flag = 0";
        Map<String, Object> params = Maps.newHashMap();
        params.put("parentId", orgId);
        Long count = TenantContext.withoutTenant(() ->
                baseDao.querySingleForSql(sql, params, Long.class));
        return count != null && count > 0;
    }

    private boolean hasMembers(Long orgId) {
        String sql = "SELECT COUNT(*) FROM sys_user WHERE org_id = :orgId AND tenant_id IS NULL AND delete_flag = 0";
        Map<String, Object> params = Maps.newHashMap();
        params.put("orgId", orgId);
        Long count = TenantContext.withoutTenant(() ->
                baseDao.querySingleForSql(sql, params, Long.class));
        return count != null && count > 0;
    }

    private boolean wouldCreateCircularReference(Long orgId, Long newParentId) {
        List<Long> childrenIds = getAllChildrenIds(orgId);
        return childrenIds.contains(newParentId);
    }

    private List<Long> getAllChildrenIds(Long orgId) {
        List<Long> result = new ArrayList<>();
        collectChildrenIds(orgId, result);
        return result;
    }

    private void collectChildrenIds(Long parentId, List<Long> result) {
        String sql = "SELECT id FROM sys_organization WHERE parent_id = :parentId AND tenant_id IS NULL AND delete_flag = 0";
        Map<String, Object> params = Maps.newHashMap();
        params.put("parentId", parentId);
        List<Long> children = TenantContext.withoutTenant(() ->
                baseDao.queryListForSql(sql, params, Long.class));
        result.addAll(children);
        for (Long childId : children) {
            collectChildrenIds(childId, result);
        }
    }

    private List<OrganizationTreeVO> buildTree(List<OrganizationDTO> orgs, Long parentId) {
        return orgs.stream()
                .filter(org -> parentId.equals(org.getParentId()))
                .map(org -> {
                    OrganizationTreeVO vo = convertToTreeVO(org);
                    vo.setChildren(buildTree(orgs, org.getId()));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private OrganizationTreeVO convertToTreeVO(OrganizationDTO org) {
        OrganizationTreeVO vo = new OrganizationTreeVO();
        vo.setId(org.getId());
        vo.setOrgCode(org.getOrgCode());
        vo.setOrgName(org.getOrgName());
        vo.setParentId(org.getParentId());
        vo.setSortOrder(org.getSortOrder());
        vo.setLeaderId(org.getLeaderId());
        vo.setLeaderName(org.getLeaderName());
        vo.setContactPhone(org.getContactPhone());
        vo.setStatus(org.getStatus());
        vo.setChildrenCount(org.getChildrenCount());
        vo.setMemberCount(org.getMemberCount());
        return vo;
    }
}
