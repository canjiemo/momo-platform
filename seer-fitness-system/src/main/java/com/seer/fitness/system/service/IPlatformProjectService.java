package com.seer.fitness.system.service;

import com.seer.fitness.system.dto.*;
import io.github.mocanjie.base.mycommon.pager.Pager;

/**
 * 平台项目服务接口
 * 管理 public.seer_project_info 中的平台项目库
 *
 * @author seer-fitness
 */
public interface IPlatformProjectService {

    /**
     * 分页查询平台项目
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
     * 创建平台项目
     *
     * @param request 创建请求
     */
    void create(ProjectInfoCreateRequest request);

    /**
     * 更新平台项目
     *
     * @param request 更新请求
     */
    void update(ProjectInfoUpdateRequest request);

    /**
     * 删除平台项目
     *
     * @param ids 项目ID数组
     */
    void delete(String[] ids);
}
