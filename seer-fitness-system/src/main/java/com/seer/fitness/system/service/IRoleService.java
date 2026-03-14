package com.seer.fitness.system.service;

import com.seer.fitness.system.dto.*;
import io.github.canjiemo.base.myjdbc.service.IBaseService;
import io.github.canjiemo.mycommon.pager.Pager;

import java.util.List;

public interface IRoleService extends IBaseService {

    Pager<RoleDTO> search(RoleQueryParam param, Pager<RoleDTO> pager);

    List<RoleDTO> list(Long tenantId);

    RoleDTO getById(String idStr);

    void create(RoleCreateRequest request);

    void update(RoleUpdateRequest request);

    void delete(String... ids);

    void assignMenus(String roleIdStr, List<String> menuIdStrs);

    List<String> getRoleMenuIds(String roleIdStr);

    List<RoleDTO> getUserRoles(Long userId);

    List<UserDTO> getUsersByRole(Long roleId);
}
