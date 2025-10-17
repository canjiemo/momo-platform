package com.seer.fitness.system.service;

import com.seer.fitness.system.dto.*;
import io.github.mocanjie.base.mycommon.pager.Pager;

import java.util.List;

/**
 * 租户项目服务接口
 * 管理租户 schema 中的 seer_project_info（学校实际使用的项目）
 *
 * @author seer-fitness
 */
public interface ITenantProjectService {

    /**
     * 分页查询租户项目
     *
     * @param param 查询参数
     * @param pager 分页参数
     * @return 分页结果
     */
    Pager<ProjectInfoDTO> search(ProjectInfoQueryParam param, Pager pager);

    /**
     * 根据ID获取项目详情
     *
     * @param id 项目ID
     * @return 项目详情
     */
    ProjectInfoDTO getById(Long id);

    /**
     * 创建租户项目（学校自定义项目）
     *
     * @param request 创建请求
     */
    void create(ProjectInfoCreateRequest request);

    /**
     * 更新租户项目
     *
     * @param request 更新请求
     */
    void update(ProjectInfoUpdateRequest request);

    /**
     * 删除租户项目
     *
     * @param ids 项目ID数组
     */
    void delete(String[] ids);

    /**
     * 从平台分配项目到租户
     * 将选中的平台项目复制到当前租户的项目表
     *
     * @param request 分配请求
     */
    void assignFromPlatform(ProjectAssignRequest request);

    /**
     * 获取所有平台项目（供租户选择分配）
     *
     * @return 平台项目列表
     */
    List<ProjectInfoDTO> getPlatformProjects();
}
