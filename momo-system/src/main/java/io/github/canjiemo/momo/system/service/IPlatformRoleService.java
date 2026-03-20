package io.github.canjiemo.momo.system.service;

import io.github.canjiemo.momo.system.dto.RoleCreateRequest;
import io.github.canjiemo.momo.system.dto.RoleDTO;
import io.github.canjiemo.momo.system.dto.RoleQueryParam;
import io.github.canjiemo.momo.system.dto.RoleUpdateRequest;
import io.github.canjiemo.mycommon.pager.Pager;

import java.util.List;

/**
 * 平台角色管理服务接口
 * 管理 tenant_id=NULL 的平台级角色，这些角色可分配给租户，决定租户能访问哪些功能
 *
 * @author canjiemo@gmail.com
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
