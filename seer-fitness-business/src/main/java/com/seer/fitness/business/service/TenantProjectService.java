package com.seer.fitness.business.service;

import com.google.common.collect.Maps;
import com.seer.fitness.framework.annotation.PublicSchema;
import com.seer.fitness.business.dto.*;
import com.seer.fitness.business.entity.SeerProjectInfo;
import io.github.mocanjie.base.mycommon.exception.BusinessException;
import io.github.mocanjie.base.mycommon.pager.Pager;
import io.github.mocanjie.base.myjpa.service.impl.BaseServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 租户项目服务实现
 * 管理租户 schema 中的 seer_project_info（学校实际使用的项目）
 * <p>
 * 默认操作租户 schema，除了查询平台项目的方法使用 @PublicSchema
 *
 * @author seer-fitness
 */
@Service
@Slf4j
public class TenantProjectService extends BaseServiceImpl implements ITenantProjectService {

    /**
     * 分页查询租户项目
     */
    @Override
    public Pager<ProjectInfoDTO> search(ProjectInfoQueryParam param, Pager pager) {
        Map<String, Object> queryMap = Maps.newHashMap();

        String sql = "SELECT id, project_code, project_name, unit, training_duration, " +
                    "is_higher_better, sort_order, status, remark, created_at, updated_at " +
                    "FROM seer_project_info";

        List<String> conditions = new ArrayList<>();

        if (StringUtils.hasText(param.getProjectCode())) {
            conditions.add("project_code LIKE :projectCode");
            queryMap.put("projectCode", "%" + param.getProjectCode() + "%");
        }

        if (StringUtils.hasText(param.getProjectName())) {
            conditions.add("project_name LIKE :projectName");
            queryMap.put("projectName", "%" + param.getProjectName() + "%");
        }

        if (param.getStatus() != null) {
            conditions.add("status = :status");
            queryMap.put("status", param.getStatus());
        }

        if (!conditions.isEmpty()) {
            sql += " WHERE " + String.join(" AND ", conditions);
        }

        sql += " ORDER BY sort_order ASC, created_at DESC";

        log.info("租户项目分页查询SQL: {}", sql);

        return baseDao.queryPageForSql(sql, queryMap, pager, ProjectInfoDTO.class);
    }

    /**
     * 根据ID获取项目详情
     */
    @Override
    public ProjectInfoDTO getById(Long id) {
        if (id == null) {
            throw new BusinessException("项目ID不能为空");
        }

        SeerProjectInfo project = baseDao.queryById(id, SeerProjectInfo.class);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }

        return convertToDTO(project);
    }

    /**
     * 创建租户项目（学校自定义项目）
     */
    @Override
    @Transactional(readOnly = false)
    public void create(ProjectInfoCreateRequest request) {
        // 检查项目编号是否已存在
        if (isProjectCodeExists(request.getProjectCode())) {
            throw new BusinessException("项目编号已存在");
        }

        SeerProjectInfo project = new SeerProjectInfo();
        project.setProjectCode(request.getProjectCode());
        project.setProjectName(request.getProjectName());
        project.setUnit(request.getUnit());
        project.setTrainingDuration(request.getTrainingDuration());
        project.setIsHigherBetter(request.getIsHigherBetter() != null ? request.getIsHigherBetter() : 1);
        project.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        project.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        project.setRemark(request.getRemark());
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());

        baseDao.insertPO(project, true);

        log.info("创建租户项目成功: projectCode={}, id={}", request.getProjectCode(), project.getId());
    }

    /**
     * 更新租户项目
     */
    @Override
    @Transactional(readOnly = false)
    public void update(ProjectInfoUpdateRequest request) {
        SeerProjectInfo project = baseDao.queryById(request.getId(), SeerProjectInfo.class);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }

        // 如果修改了项目编号，检查新编号是否已被其他项目使用
        if (!project.getProjectCode().equals(request.getProjectCode())) {
            if (isProjectCodeExists(request.getProjectCode())) {
                throw new BusinessException("项目编号已存在");
            }
        }

        project.setProjectCode(request.getProjectCode());
        project.setProjectName(request.getProjectName());
        project.setUnit(request.getUnit());
        project.setTrainingDuration(request.getTrainingDuration());
        project.setIsHigherBetter(request.getIsHigherBetter());
        project.setSortOrder(request.getSortOrder());
        project.setStatus(request.getStatus());
        project.setRemark(request.getRemark());
        project.setUpdatedAt(LocalDateTime.now());

        baseDao.updatePO(project);

        log.info("更新租户项目成功: id={}, projectName={}", request.getId(), request.getProjectName());
    }

    /**
     * 删除租户项目
     */
    @Override
    @Transactional(readOnly = false)
    public void delete(String[] ids) {
        if (ids == null || ids.length == 0) {
            throw new BusinessException("删除的项目ID不能为空");
        }

        for (String id : ids) {
            if (!StringUtils.hasText(id)) {
                throw new BusinessException("项目ID不能为空");
            }
        }

        // 物理删除
        baseDao.delByIds(SeerProjectInfo.class, ids);

        log.info("删除租户项目成功: ids={}", java.util.Arrays.toString(ids));
    }

    /**
     * 从平台分配项目到租户
     * 将选中的平台项目复制到当前租户的项目表
     */
    @Override
    @Transactional(readOnly = false)
    public void assignFromPlatform(ProjectAssignRequest request) {
        if (request.getProjectIds() == null || request.getProjectIds().isEmpty()) {
            throw new BusinessException("项目ID列表不能为空");
        }

        // 获取平台项目（从 public schema）
        List<SeerProjectInfo> platformProjects = getPlatformProjectsByIds(request.getProjectIds());

        if (platformProjects.isEmpty()) {
            throw new BusinessException("未找到指定的平台项目");
        }

        int successCount = 0;
        int skipCount = 0;

        for (SeerProjectInfo platformProject : platformProjects) {
            // 检查项目编号是否已存在于租户表
            if (isProjectCodeExists(platformProject.getProjectCode())) {
                log.warn("项目编号已存在，跳过: projectCode={}", platformProject.getProjectCode());
                skipCount++;
                continue;
            }

            // 复制到租户项目表
            SeerProjectInfo tenantProject = new SeerProjectInfo();
            tenantProject.setProjectCode(platformProject.getProjectCode());
            tenantProject.setProjectName(platformProject.getProjectName());
            tenantProject.setUnit(platformProject.getUnit());
            tenantProject.setTrainingDuration(platformProject.getTrainingDuration());
            tenantProject.setIsHigherBetter(platformProject.getIsHigherBetter());
            tenantProject.setSortOrder(platformProject.getSortOrder());
            tenantProject.setStatus(platformProject.getStatus());
            tenantProject.setRemark(platformProject.getRemark());
            tenantProject.setCreatedAt(LocalDateTime.now());
            tenantProject.setUpdatedAt(LocalDateTime.now());

            baseDao.insertPO(tenantProject, true);
            successCount++;

            log.info("分配项目成功: projectCode={}, newId={}", platformProject.getProjectCode(), tenantProject.getId());
        }

        log.info("项目分配完成: 成功={}, 跳过={}", successCount, skipCount);
    }

    /**
     * 获取所有平台项目（供租户选择分配）
     * 从 public schema 查询
     */
    @Override
    @PublicSchema(reason = "查询平台项目列表供租户选择")
    public List<ProjectInfoDTO> getPlatformProjects() {
        String sql = "SELECT id, project_code, project_name, unit, training_duration, " +
                    "is_higher_better, sort_order, status, remark, created_at, updated_at " +
                    "FROM seer_project_info " +
                    "WHERE status = 1 " +
                    "ORDER BY sort_order ASC, created_at DESC";

        return baseDao.queryListForSql(sql, Maps.newHashMap(), ProjectInfoDTO.class);
    }

    /**
     * 根据ID列表查询平台项目（从 public schema）
     */
    @PublicSchema(reason = "查询平台项目用于分配")
    private List<SeerProjectInfo> getPlatformProjectsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }

        String sql = "SELECT * FROM seer_project_info WHERE id IN (:ids) AND status = 1";
        Map<String, Object> params = Maps.newHashMap();
        params.put("ids", ids);

        return baseDao.queryListForSql(sql, params, SeerProjectInfo.class);
    }

    /**
     * 检查项目编号是否已存在（在租户 schema）
     */
    private boolean isProjectCodeExists(String projectCode) {
        String sql = "SELECT COUNT(*) FROM seer_project_info WHERE project_code = :projectCode";
        Map<String, Object> params = Maps.newHashMap();
        params.put("projectCode", projectCode);

        Long count = baseDao.querySingleForSql(sql, params, Long.class);
        return count != null && count > 0;
    }

    /**
     * 转换为DTO
     */
    private ProjectInfoDTO convertToDTO(SeerProjectInfo project) {
        ProjectInfoDTO dto = new ProjectInfoDTO();
        dto.setId(project.getId());
        dto.setProjectCode(project.getProjectCode());
        dto.setProjectName(project.getProjectName());
        dto.setUnit(project.getUnit());
        dto.setTrainingDuration(project.getTrainingDuration());
        dto.setIsHigherBetter(project.getIsHigherBetter());
        dto.setSortOrder(project.getSortOrder());
        dto.setStatus(project.getStatus());
        dto.setRemark(project.getRemark());
        dto.setCreatedAt(project.getCreatedAt());
        dto.setUpdatedAt(project.getUpdatedAt());
        return dto;
    }
}
