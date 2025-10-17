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
 * 平台项目服务实现
 * 管理 public.seer_project_info 中的平台项目库
 * <p>
 * 所有方法使用 @PublicSchema 注解，确保操作路由到 public schema
 *
 * @author seer-fitness
 */
@Service
@Slf4j
public class PlatformProjectService extends BaseServiceImpl implements IPlatformProjectService {

    /**
     * 分页查询平台项目
     */
    @Override
    @PublicSchema(reason = "查询平台项目库")
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

        log.info("平台项目分页查询SQL: {}", sql);

        return baseDao.queryPageForSql(sql, queryMap, pager, ProjectInfoDTO.class);
    }

    /**
     * 根据ID获取项目详情
     */
    @Override
    @PublicSchema(reason = "查询平台项目详情")
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
     * 创建平台项目
     */
    @Override
    @Transactional(readOnly = false)
    @PublicSchema(reason = "创建平台项目")
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

        log.info("创建平台项目成功: projectCode={}, id={}", request.getProjectCode(), project.getId());
    }

    /**
     * 更新平台项目
     */
    @Override
    @Transactional(readOnly = false)
    @PublicSchema(reason = "更新平台项目")
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

        log.info("更新平台项目成功: id={}, projectName={}", request.getId(), request.getProjectName());
    }

    /**
     * 删除平台项目
     */
    @Override
    @Transactional(readOnly = false)
    @PublicSchema(reason = "删除平台项目")
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

        log.info("删除平台项目成功: ids={}", java.util.Arrays.toString(ids));
    }

    /**
     * 检查项目编号是否已存在
     */
    @PublicSchema(reason = "检查平台项目编号")
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
