package com.seer.fitness.system.service;

import com.seer.fitness.system.dto.RoleCreateRequest;
import com.seer.fitness.system.dto.RoleDTO;
import com.seer.fitness.system.dto.RoleQueryParam;
import com.seer.fitness.system.dto.RoleUpdateRequest;
import com.seer.fitness.system.entity.SysRole;
import com.seer.fitness.system.entity.SysRoleMenu;
import com.seer.fitness.system.entity.SysTenantRole;
import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import io.github.canjiemo.mycommon.exception.BusinessException;
import io.github.canjiemo.mycommon.pager.Pager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 平台角色管理服务（只操作 tenant_id=NULL 的角色）
 * 平台管理员通过此服务管理可分配给租户的平台角色模板
 *
 * @author seer-fitness
 */
@Service
@Slf4j
public class PlatformRoleService extends BaseServiceImpl implements IPlatformRoleService {

    @Override
    public Pager<RoleDTO> search(RoleQueryParam param, Pager<RoleDTO> pager) {
        return lambdaQuery(SysRole.class, RoleDTO.class)
                .isNull(SysRole::getTenantId)
                .like(SysRole::getRoleName, param.getRoleName())
                .eq(SysRole::getRoleCode, param.getRoleCode())
                .eq(SysRole::getStatus, param.getStatus())
                .orderByDesc(SysRole::getCreateTime)
                .page(pager);
    }

    @Override
    public List<RoleDTO> list() {
        return lambdaQuery(SysRole.class, RoleDTO.class)
                .isNull(SysRole::getTenantId)
                .eq(SysRole::getStatus, 1)
                .orderByDesc(SysRole::getCreateTime)
                .list();
    }

    @Override
    public RoleDTO getById(Long id) {
        RoleDTO dto = lambdaQuery(SysRole.class, RoleDTO.class)
                .eq(SysRole::getId, id)
                .isNull(SysRole::getTenantId)
                .one();
        if (dto == null) throw new BusinessException("平台角色不存在");
        return dto;
    }

    @Override
    @Transactional
    public void create(RoleCreateRequest request) {
        if (isRoleNameExists(request.getRoleName())) throw new BusinessException("角色名称已存在");
        if (isRoleCodeExists(request.getRoleCode(), null)) throw new BusinessException("角色编码已存在");

        SysRole role = new SysRole();
        role.setTenantId(null);
        role.setRoleName(request.getRoleName());
        role.setRoleCode(request.getRoleCode());
        role.setDescription(request.getDescription());
        role.setStatus(request.getStatus());
        role.setDeleteFlag(0);

        baseDao.insertPO(role, true);

        if (request.getMenuIds() != null && !request.getMenuIds().isEmpty()) {
            assignMenus(role.getId(), request.getMenuIds());
        }

        log.info("创建平台角色成功: roleName={}, id={}", request.getRoleName(), role.getId());
    }

    @Override
    @Transactional
    public void update(RoleUpdateRequest request) {
        SysRole role = getPlatformRoleEntity(request.getId());

        if (!role.getRoleName().equals(request.getRoleName()) && isRoleNameExists(request.getRoleName())) {
            throw new BusinessException("角色名称已存在");
        }
        if (!role.getRoleCode().equals(request.getRoleCode()) && isRoleCodeExists(request.getRoleCode(), request.getId())) {
            throw new BusinessException("角色编码已存在");
        }

        role.setRoleName(request.getRoleName());
        role.setRoleCode(request.getRoleCode());
        role.setDescription(request.getDescription());
        role.setStatus(request.getStatus());

        baseDao.updatePO(role);

        if (request.getMenuIds() != null) {
            removeRoleMenus(request.getId());
            if (!request.getMenuIds().isEmpty()) {
                assignMenus(request.getId(), request.getMenuIds());
            }
        }

        log.info("更新平台角色成功: id={}", request.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getPlatformRoleEntity(id); // 验证存在且是平台角色

        // 检查是否有租户正在使用该角色
        boolean inUse = lambdaQuery(SysTenantRole.class)
                .eq(SysTenantRole::getRoleId, id)
                .exists();
        if (inUse) {
            throw new BusinessException("该角色已被租户使用，无法删除");
        }

        removeRoleMenus(id);
        baseDao.delByIds(SysRole.class, String.valueOf(id));

        log.info("删除平台角色成功: id={}", id);
    }

    @Override
    public List<String> getRoleMenuIds(Long roleId) {
        getPlatformRoleEntity(roleId); // 验证存在且是平台角色
        List<SysRoleMenu> roleMenus = lambdaQuery(SysRoleMenu.class)
                .eq(SysRoleMenu::getRoleId, roleId)
                .isNull(SysRoleMenu::getTenantId)
                .list();
        return roleMenus.stream()
                .map(rm -> String.valueOf(rm.getMenuId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void assignMenus(Long roleId, List<String> menuIds) {
        getPlatformRoleEntity(roleId);
        removeRoleMenus(roleId);

        for (String menuIdStr : menuIds) {
            SysRoleMenu roleMenu = new SysRoleMenu();
            roleMenu.setTenantId(null);
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(Long.parseLong(menuIdStr));
            baseDao.insertPO(roleMenu, true);
        }

        log.info("分配平台角色菜单成功: roleId={}, menuCount={}", roleId, menuIds.size());
    }

    private SysRole getPlatformRoleEntity(Long id) {
        SysRole role = lambdaQuery(SysRole.class)
                .eq(SysRole::getId, id)
                .isNull(SysRole::getTenantId)
                .one();
        if (role == null) throw new BusinessException("平台角色不存在");
        return role;
    }

    private void removeRoleMenus(Long roleId) {
        List<SysRoleMenu> roleMenus = lambdaQuery(SysRoleMenu.class)
                .eq(SysRoleMenu::getRoleId, roleId)
                .isNull(SysRoleMenu::getTenantId)
                .list();
        for (SysRoleMenu rm : roleMenus) {
            baseDao.delPO(rm);
        }
    }

    private boolean isRoleNameExists(String roleName) {
        return lambdaQuery(SysRole.class)
                .eq(SysRole::getRoleName, roleName)
                .isNull(SysRole::getTenantId)
                .exists();
    }

    private boolean isRoleCodeExists(String roleCode, Long excludeId) {
        var q = lambdaQuery(SysRole.class)
                .eq(SysRole::getRoleCode, roleCode)
                .isNull(SysRole::getTenantId);
        if (excludeId != null) q.ne(SysRole::getId, excludeId);
        return q.exists();
    }
}
