package com.seer.fitness.system.service;

import com.seer.fitness.system.dto.RoleCreateRequest;
import com.seer.fitness.system.dto.RoleDTO;
import com.seer.fitness.system.dto.RoleQueryParam;
import com.seer.fitness.system.dto.RoleUpdateRequest;
import io.github.canjiemo.mycommon.pager.Pager;

import java.util.List;

/**
 * 平台角色管理服务接口
 * 管理 tenant_id=NULL 的平台级角色，这些角色可分配给租户，决定租户能访问哪些功能
 *
 * @author seer-fitness
 */
public interface IPlatformRoleService {

    Pager<RoleDTO> search(RoleQueryParam param, Pager<RoleDTO> pager);

    List<RoleDTO> list();

    RoleDTO getById(Long id);

    void create(RoleCreateRequest request);

    void update(RoleUpdateRequest request);

    void delete(Long id);

    List<String> getRoleMenuIds(Long roleId);

    void assignMenus(Long roleId, List<String> menuIds);
}
