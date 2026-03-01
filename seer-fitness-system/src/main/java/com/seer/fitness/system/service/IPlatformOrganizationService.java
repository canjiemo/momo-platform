package com.seer.fitness.system.service;

import com.seer.fitness.system.dto.*;
import io.github.mocanjie.base.mycommon.pager.Pager;

import java.util.List;

/**
 * 平台组织管理服务接口
 * 管理 tenant_id=NULL 的平台级组织（平台自身的组织架构）
 *
 * @author seer-fitness
 */
public interface IPlatformOrganizationService {

    Pager<OrganizationDTO> search(OrganizationQueryParam param, Pager pager);

    List<OrganizationTreeVO> getOrganizationTree();

    List<OrganizationDTO> list();

    OrganizationDTO getById(Long id);

    void create(OrganizationCreateRequest request);

    void update(OrganizationUpdateRequest request);

    void delete(String[] ids);
}
