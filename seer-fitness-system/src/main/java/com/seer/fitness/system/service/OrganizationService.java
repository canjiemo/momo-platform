package com.seer.fitness.system.service;

import com.google.common.collect.Maps;
import com.seer.fitness.system.dto.*;
import com.seer.fitness.system.entity.SysOrganization;
import com.seer.fitness.system.entity.SysUser;
import com.seer.fitness.framework.utils.SecurityContextUtil;
import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import io.github.canjiemo.base.myjdbc.tenant.TenantContext;
import io.github.canjiemo.mycommon.exception.BusinessException;
import io.github.canjiemo.mycommon.pager.Pager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 组织架构管理服务实现
 * 提供组织架构的增删改查、树形结构管理、人员关联等功能
 *
 * @author seer-fitness
 */
@Service
@Slf4j
public class OrganizationService extends BaseServiceImpl implements IOrganizationService {


    /**
     * 分页查询组织架构
     * 支持复杂查询条件、分页、排序
     */
    @Override
    public Pager<OrganizationDTO> search(OrganizationQueryParam param, Pager<OrganizationDTO> pager) {
        Map<String, Object> queryMap = Maps.newHashMap();

        String sql = "SELECT o.id, o.org_code, o.org_name, o.parent_id, " +
                    "o.sort_order, o.leader_id, o.contact_phone, o.email, o.address, " +
                    "o.description, o.status, o.create_time, o.update_time, " +
                    "u.real_name as leader_name, " +
                    "COALESCE(child_count.count, 0) as children_count, " +
                    "COALESCE(member_count.count, 0) as member_count " +
                    "FROM sys_organization o " +
                    "LEFT JOIN sys_user u ON o.leader_id = u.id " +
                    "LEFT JOIN (SELECT parent_id, COUNT(*) as count FROM sys_organization GROUP BY parent_id) child_count ON o.id = child_count.parent_id " +
                    "LEFT JOIN (SELECT org_id, COUNT(*) as count FROM sys_user WHERE status = 1 GROUP BY org_id) member_count ON o.id = member_count.org_id";

        List<String> conditions = new ArrayList<>();

        if (StringUtils.hasText(param.getOrgCode())) {
            conditions.add("o.org_code LIKE :orgCode");
            queryMap.put("orgCode", "%" + param.getOrgCode() + "%");
        }

        if (StringUtils.hasText(param.getOrgName())) {
            conditions.add("o.org_name LIKE :orgName");
            queryMap.put("orgName", "%" + param.getOrgName() + "%");
        }


        if (param.getParentId() != null) {
            if (param.getParentId() == 0L) {
                conditions.add("o.parent_id = 0");
            } else {
                conditions.add("o.parent_id = :parentId");
                queryMap.put("parentId", param.getParentId());
            }
        }

        if (param.getLeaderId() != null) {
            conditions.add("o.leader_id = :leaderId");
            queryMap.put("leaderId", param.getLeaderId());
        }

        if (param.getStatus() != null) {
            conditions.add("o.status = :status");
            queryMap.put("status", param.getStatus());
        }

        // 平台管理员可按 tenantId 过滤特定租户数据
        boolean isPlatformAdmin = SecurityContextUtil.isPlatformAdmin();
        if (isPlatformAdmin && param.getTenantId() != null) {
            conditions.add("o.tenant_id = :tenantId");
            queryMap.put("tenantId", param.getTenantId());
        }

        if (!conditions.isEmpty()) {
            sql += " WHERE " + String.join(" AND ", conditions);
        }

        sql += " ORDER BY o.sort_order ASC, o.create_time DESC";

        log.info("组织架构分页查询SQL: {}", sql);

        final String finalSql = sql;
        return (isPlatformAdmin && param.getTenantId() != null)
                ? TenantContext.withoutTenant(() -> baseDao.queryPageForSql(finalSql, queryMap, pager, OrganizationDTO.class))
                : baseDao.queryPageForSql(finalSql, queryMap, pager, OrganizationDTO.class);
    }

    /**
     * 获取完整的组织架构树形结构
     */
    @Override
    public List<OrganizationTreeVO> getOrganizationTree() {
        String sql = "SELECT o.id, o.org_code, o.org_name, o.parent_id, " +
                    "o.sort_order, o.leader_id, o.contact_phone, o.status, " +
                    "u.real_name as leader_name, " +
                    "COALESCE(child_count.count, 0) as children_count, " +
                    "COALESCE(member_count.count, 0) as member_count " +
                    "FROM sys_organization o " +
                    "LEFT JOIN sys_user u ON o.leader_id = u.id " +
                    "LEFT JOIN (SELECT parent_id, COUNT(*) as count FROM sys_organization GROUP BY parent_id) child_count ON o.id = child_count.parent_id " +
                    "LEFT JOIN (SELECT org_id, COUNT(*) as count FROM sys_user WHERE status = 1 GROUP BY org_id) member_count ON o.id = member_count.org_id " +
                    "WHERE o.status = 1 ORDER BY o.sort_order ASC";

        List<OrganizationDTO> allOrgs = baseDao.queryListForSql(sql, Maps.newHashMap(), OrganizationDTO.class);

        return buildOrganizationTree(allOrgs, 0L);
    }

    /**
     * 获取指定组织的子组织树
     */
    @Override
    public List<OrganizationTreeVO> getChildrenTree(String parentId) {
        String parentIdStr = normalizeParentId(parentId);

        String sql = "SELECT o.id, o.org_code, o.org_name, o.parent_id, " +
                    "o.sort_order, o.leader_id, o.contact_phone, o.status, " +
                    "u.real_name as leader_name, " +
                    "COALESCE(child_count.count, 0) as children_count, " +
                    "COALESCE(member_count.count, 0) as member_count " +
                    "FROM sys_organization o " +
                    "LEFT JOIN sys_user u ON o.leader_id = u.id " +
                    "LEFT JOIN (SELECT parent_id, COUNT(*) as count FROM sys_organization GROUP BY parent_id) child_count ON o.id = child_count.parent_id " +
                    "LEFT JOIN (SELECT org_id, COUNT(*) as count FROM sys_user WHERE status = 1 GROUP BY org_id) member_count ON o.id = member_count.org_id " +
                    "WHERE o.status = 1";

        Map<String, Object> params = Maps.newHashMap();
        if ("0".equals(parentIdStr)) {
            sql += " AND o.parent_id = 0";
        } else {
            sql += " AND o.parent_id = :parentId";
            params.put("parentId", Long.parseLong(parentIdStr));
        }
        sql += " ORDER BY o.sort_order ASC";

        List<OrganizationDTO> children = baseDao.queryListForSql(sql, params, OrganizationDTO.class);

        return buildOrganizationTree(children, "0".equals(parentIdStr) ? 0L : Long.parseLong(parentIdStr));
    }

    /**
     * 获取所有组织列表（不分页）
     */
    @Override
    public List<OrganizationDTO> list() {
        String sql = "SELECT o.id, o.org_code, o.org_name, o.parent_id, " +
                    "o.sort_order, o.leader_id, o.contact_phone, o.email, o.address, " +
                    "o.description, o.status, o.create_time, o.update_time, " +
                    "u.real_name as leader_name, " +
                    "COALESCE(child_count.count, 0) as children_count, " +
                    "COALESCE(member_count.count, 0) as member_count " +
                    "FROM sys_organization o " +
                    "LEFT JOIN sys_user u ON o.leader_id = u.id " +
                    "LEFT JOIN (SELECT parent_id, COUNT(*) as count FROM sys_organization GROUP BY parent_id) child_count ON o.id = child_count.parent_id " +
                    "LEFT JOIN (SELECT org_id, COUNT(*) as count FROM sys_user WHERE status = 1 GROUP BY org_id) member_count ON o.id = member_count.org_id " +
                    "ORDER BY o.sort_order ASC, o.create_time DESC";

        return baseDao.queryListForSql(sql, Maps.newHashMap(), OrganizationDTO.class);
    }

    /**
     * 根据ID获取组织详情
     */
    @Override
    public OrganizationDTO getById(Long id) {
        if (id == null) {
            throw new BusinessException("组织ID不能为空");
        }

        String sql = "SELECT o.id, o.org_code, o.org_name, o.parent_id, " +
                    "o.sort_order, o.leader_id, o.contact_phone, o.email, o.address, " +
                    "o.description, o.status, o.create_time, o.update_time, " +
                    "u.real_name as leader_name, " +
                    "COALESCE(child_count.count, 0) as children_count, " +
                    "COALESCE(member_count.count, 0) as member_count " +
                    "FROM sys_organization o " +
                    "LEFT JOIN sys_user u ON o.leader_id = u.id " +
                    "LEFT JOIN (SELECT parent_id, COUNT(*) as count FROM sys_organization GROUP BY parent_id) child_count ON o.id = child_count.parent_id " +
                    "LEFT JOIN (SELECT org_id, COUNT(*) as count FROM sys_user WHERE status = 1 GROUP BY org_id) member_count ON o.id = member_count.org_id " +
                    "WHERE o.id = :id";

        Map<String, Object> params = Maps.newHashMap();
        params.put("id", id);

        OrganizationDTO dto = baseDao.querySingleForSql(sql, params, OrganizationDTO.class);
        if (dto == null) {
            throw new BusinessException("组织不存在");
        }

        return dto;
    }

    /**
     * 创建组织
     */
    @Override
    @Transactional(readOnly = false)
    public void create(OrganizationCreateRequest request) {
        // 校验组织编码唯一性
        if (!isOrgCodeUnique(request.getOrgCode(), null)) {
            throw new BusinessException("组织编码已存在");
        }

        // 处理parentId，null或0表示顶级组织
        Long parentId = request.getParentId() != null ? request.getParentId() : 0L;

        // 验证父组织是否存在
        if (parentId != 0L) {
            SysOrganization parentOrg = baseDao.queryById(parentId, SysOrganization.class);
            if (parentOrg == null) {
                throw new BusinessException("父组织不存在");
            }
        }

        // 验证负责人是否存在
        if (request.getLeaderId() != null) {
            SysUser leader = baseDao.queryById(request.getLeaderId(), SysUser.class);
            if (leader == null) {
                throw new BusinessException("指定的负责人不存在");
            }
        }

        SysOrganization org = new SysOrganization();
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

        baseDao.insertPO(org, true);

        log.info("创建组织成功: orgCode={}, orgName={}, id={}", request.getOrgCode(), request.getOrgName(), org.getId());
    }

    /**
     * 更新组织
     */
    @Override
    @Transactional(readOnly = false)
    public void update(OrganizationUpdateRequest request) {
        SysOrganization org = baseDao.queryById(request.getId(), SysOrganization.class);
        if (org == null) {
            throw new BusinessException("组织不存在");
        }

        // 校验组织编码唯一性
        if (!isOrgCodeUnique(request.getOrgCode(), request.getId())) {
            throw new BusinessException("组织编码已存在");
        }

        // 处理parentId，null或0表示顶级组织
        Long parentId = request.getParentId() != null ? request.getParentId() : 0L;

        // 验证父组织是否存在（如果有设置）
        if (parentId != 0L) {
            if (parentId.equals(request.getId())) {
                throw new BusinessException("不能将自己设为父组织");
            }

            // 检查是否会形成循环引用
            if (wouldCreateCircularReference(request.getId(), parentId)) {
                throw new BusinessException("不能将组织移动到其子组织下，会形成循环引用");
            }

            SysOrganization parentOrg = baseDao.queryById(parentId, SysOrganization.class);
            if (parentOrg == null) {
                throw new BusinessException("父组织不存在");
            }
        }

        // 验证负责人是否存在
        if (request.getLeaderId() != null) {
            SysUser leader = baseDao.queryById(request.getLeaderId(), SysUser.class);
            if (leader == null) {
                throw new BusinessException("指定的负责人不存在");
            }
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

        baseDao.updatePO(org);

        log.info("更新组织成功: id={}, orgCode={}, orgName={}", request.getId(), request.getOrgCode(), request.getOrgName());
    }

    /**
     * 批量删除组织（逻辑删除）
     */
    @Override
    @Transactional(readOnly = false)
    public void delete(String[] ids) {
        if (ids == null || ids.length == 0) {
            throw new BusinessException("删除的组织ID不能为空");
        }

        for (String id : ids) {
            if (!StringUtils.hasText(id)) {
                throw new BusinessException("组织ID不能为空");
            }

            Long orgId = Long.valueOf(id);

            // 检查是否有子组织
            if (hasChildren(orgId)) {
                throw new BusinessException("该组织存在子组织，无法删除");
            }

            // 检查是否有关联用户
            if (hasMembers(orgId)) {
                throw new BusinessException("该组织存在关联用户，无法删除");
            }
        }

        // 逻辑删除组织
        baseDao.delByIds(SysOrganization.class, ids);

        log.info("删除组织成功: ids={}", Arrays.toString(ids));
    }

    /**
     * 移动组织到新的父组织下
     */
    @Override
    @Transactional(readOnly = false)
    public void moveOrganization(Long orgId, String newParentId) {
        if (orgId == null) {
            throw new BusinessException("组织ID不能为空");
        }

        SysOrganization org = baseDao.queryById(orgId, SysOrganization.class);
        if (org == null) {
            throw new BusinessException("组织不存在");
        }

        String parentIdStr = normalizeParentId(newParentId);

        // 验证新父组织
        if (!"0".equals(parentIdStr)) {
            if (Long.parseLong(parentIdStr) == orgId.longValue()) {
                throw new BusinessException("不能将自己设为父组织");
            }

            // 检查是否会形成循环引用
            if (wouldCreateCircularReference(orgId, Long.parseLong(parentIdStr))) {
                throw new BusinessException("不能将组织移动到其子组织下，会形成循环引用");
            }

            SysOrganization newParent = baseDao.queryById(Long.parseLong(parentIdStr), SysOrganization.class);
            if (newParent == null) {
                throw new BusinessException("新父组织不存在");
            }
        }

        org.setParentId("0".equals(parentIdStr) ? 0L : Long.parseLong(parentIdStr));

        baseDao.updatePO(org);

        log.info("移动组织成功: orgId={}, newParentId={}", orgId, newParentId);
    }

    /**
     * 获取组织的所有子级组织ID列表
     */
    @Override
    public List<Long> getAllChildrenIds(Long orgId) {
        if (orgId == null) {
            return new ArrayList<>();
        }

        List<Long> result = new ArrayList<>();
        collectChildrenIds(orgId, result);
        return result;
    }

    /**
     * 获取组织的上级路径
     */
    @Override
    public List<OrganizationDTO> getOrganizationPath(Long orgId) {
        if (orgId == null) {
            return new ArrayList<>();
        }

        List<OrganizationDTO> path = new ArrayList<>();
        SysOrganization current = baseDao.queryById(orgId, SysOrganization.class);

        while (current != null) {
            OrganizationDTO dto = new OrganizationDTO();
            BeanUtils.copyProperties(current, dto);
            dto.setParentId(current.getParentId() != null ? current.getParentId() : 0L);
            path.add(0, dto); // 插入到开头

            if (current.getParentId() != null) {
                current = baseDao.queryById(current.getParentId(), SysOrganization.class);
            } else {
                break;
            }
        }

        return path;
    }

    /**
     * 校验组织编码是否唯一
     */
    @Override
    public boolean isOrgCodeUnique(String orgCode, Long excludeId) {
        var q = lambdaQuery(SysOrganization.class).eq(SysOrganization::getOrgCode, orgCode);
        if (excludeId != null) {
            q.ne(SysOrganization::getId, excludeId);
        }
        return !q.exists();
    }

    /**
     * 构建组织架构树
     */
    private List<OrganizationTreeVO> buildOrganizationTree(List<OrganizationDTO> orgs, Long parentId) {
        return orgs.stream()
                .filter(org -> {
                    if (parentId == 0L) {
                        // 查找顶级组织：parentId为0的组织
                        return org.getParentId() == 0L;
                    }
                    return parentId.equals(org.getParentId());
                })
                .map(org -> {
                    OrganizationTreeVO treeNode = new OrganizationTreeVO();
                    BeanUtils.copyProperties(org, treeNode);
                    treeNode.setChildren(buildOrganizationTree(orgs, org.getId()));
                    return treeNode;
                })
                .collect(Collectors.toList());
    }

    /**
     * 检查是否有子组织
     */
    private boolean hasChildren(Long orgId) {
        return lambdaQuery(SysOrganization.class).eq(SysOrganization::getParentId, orgId).exists();
    }

    /**
     * 检查是否有关联用户
     */
    private boolean hasMembers(Long orgId) {
        return lambdaQuery(SysUser.class).eq(SysUser::getOrgId, orgId).eq(SysUser::getStatus, 1).exists();
    }

    /**
     * 检查是否会形成循环引用
     */
    private boolean wouldCreateCircularReference(Long orgId, Long newParentId) {
        if (newParentId == null) {
            return false;
        }

        List<Long> childrenIds = getAllChildrenIds(orgId);
        return childrenIds.contains(newParentId);
    }

    /**
     * 递归收集所有子级组织ID
     */
    private void collectChildrenIds(Long parentId, List<Long> result) {
        List<Long> directChildren = lambdaQuery(SysOrganization.class)
                .eq(SysOrganization::getParentId, parentId)
                .list()
                .stream()
                .map(SysOrganization::getId)
                .collect(Collectors.toList());
        result.addAll(directChildren);

        // 递归查找子级的子级
        for (Long childId : directChildren) {
            collectChildrenIds(childId, result);
        }
    }


    /**
     * 标准化parentId处理
     * 将null、空字符串""统一处理为"0"，"0"保持不变
     *
     * @param parentId 前端传递的parentId
     * @return 标准化后的parentId，"0"表示顶级组织，其他值正常返回
     */
    private String normalizeParentId(String parentId) {
        // 处理null、空字符串的情况，都表示顶级组织，返回"0"
        if (parentId == null || parentId.trim().isEmpty()) {
            return "0";
        }
        return parentId.trim();
    }
}