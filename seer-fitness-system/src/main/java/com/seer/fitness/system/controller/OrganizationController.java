package com.seer.fitness.system.controller;

import com.seer.fitness.system.annotation.OperationLog;
import com.seer.fitness.system.enums.OperationType;
import com.seer.fitness.system.dto.*;
import com.seer.fitness.system.security.RequireAuth;
import com.seer.fitness.system.service.IOrganizationService;
import io.github.mocanjie.base.mycommon.pager.Pager;
import io.github.mocanjie.base.mycommon.pager.PagerHandler;
import io.github.mocanjie.base.mymvc.controller.MyBaseController;
import io.github.mocanjie.base.mymvc.data.MyResponseResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 组织架构管理控制器
 * 提供组织架构的增删改查、树形结构管理、移动等功能
 *
 * @author seer-fitness
 */
@RestController
@RequestMapping("/system/organization")
public class OrganizationController extends MyBaseController {

    @Autowired
    private IOrganizationService organizationService;

    /**
     * 分页查询组织架构
     * 支持复杂查询条件、分页、排序
     *
     * @param param 查询参数
     * @return 分页结果
     */
    @PostMapping("/search")
    @RequireAuth(permissions = {"organization:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "organization",
        description = "分页查询组织架构"
    )
    public MyResponseResult<Pager<OrganizationDTO>> search(@RequestBody OrganizationQueryParam param) {
        return super.doJsonPagerOut(organizationService.search(param, PagerHandler.createPager(param)));
    }

    /**
     * 获取完整的组织架构树形结构
     * 返回所有启用状态的组织，按排序字段排序
     *
     * @return 组织架构树形结构列表
     */
    @GetMapping("/tree")
    @RequireAuth(login = true)
    @OperationLog(
        type = OperationType.QUERY,
        module = "organization",
        description = "获取组织架构树"
    )
    public MyResponseResult<List<OrganizationTreeVO>> getOrganizationTree() {
        return super.doJsonOut(organizationService.getOrganizationTree());
    }

    /**
     * 获取指定组织的子组织树
     * 根据父组织ID返回其下属的组织树
     *
     * @param parentId 父组织ID，"0"表示获取顶级组织
     * @return 子组织树形结构
     */
    @GetMapping("/tree/{parentId}")
    @RequireAuth(login = true)
    public MyResponseResult<List<OrganizationTreeVO>> getChildrenTree(@PathVariable String parentId) {
        return super.doJsonOut(organizationService.getChildrenTree(parentId));
    }

    /**
     * 获取所有组织列表（不分页）
     * 用于管理界面的下拉选择框等场景
     *
     * @return 组织列表
     */
    @GetMapping("/list")
    @RequireAuth(login = true)
    public MyResponseResult<List<OrganizationDTO>> list() {
        return super.doJsonOut(organizationService.list());
    }

    /**
     * 根据ID获取组织详情
     * 包含组织基本信息、负责人信息、统计数据等
     *
     * @param id 组织ID
     * @return 组织详情
     */
    @GetMapping("/{id}")
    @RequireAuth(permissions = {"organization:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "organization",
        description = "查询组织详情"
    )
    public MyResponseResult<OrganizationDTO> getById(@PathVariable Long id) {
        return super.doJsonOut(organizationService.getById(id));
    }

    /**
     * 创建组织
     * 包括编码唯一性校验、父组织验证等操作
     *
     * @param request 创建请求参数
     * @return 操作结果
     */
    @PostMapping("/create")
    @RequireAuth(permissions = {"organization:create"})
    @OperationLog(
        type = OperationType.CREATE,
        module = "organization",
        description = "创建组织"
    )
    public MyResponseResult create(@Valid @RequestBody OrganizationCreateRequest request) {
        organizationService.create(request);
        return super.doJsonDefaultMsg();
    }

    /**
     * 更新组织信息
     *
     * @param request 更新请求参数
     * @return 操作结果
     */
    @PostMapping("/update")
    @RequireAuth(permissions = {"organization:update"})
    @OperationLog(
        type = OperationType.UPDATE,
        module = "organization",
        description = "更新组织"
    )
    public MyResponseResult update(@Valid @RequestBody OrganizationUpdateRequest request) {
        organizationService.update(request);
        return super.doJsonDefaultMsg();
    }

    /**
     * 批量删除组织（逻辑删除）
     * 会检查是否有子组织和关联用户
     *
     * @param request 删除请求参数
     * @return 操作结果
     */
    @PostMapping("/delete")
    @RequireAuth(permissions = {"organization:delete"})
    @OperationLog(
        type = OperationType.DELETE,
        module = "organization",
        description = "删除组织"
    )
    public MyResponseResult delete(@Valid @RequestBody OrganizationDeleteRequest request) {
        organizationService.delete(request.getIds());
        return super.doJsonDefaultMsg();
    }

    /**
     * 移动组织到新的父组织下
     * 支持跨层级移动，会验证循环引用
     *
     * @param orgId 要移动的组织ID
     * @param newParentId 新的父组织ID，"0"表示移动到顶级
     * @return 操作结果
     */
    @PostMapping("/move/{orgId}/{newParentId}")
    @RequireAuth(permissions = {"organization:update"})
    @OperationLog(
        type = OperationType.UPDATE,
        module = "organization",
        description = "移动组织"
    )
    public MyResponseResult moveOrganization(@PathVariable Long orgId, @PathVariable String newParentId) {
        organizationService.moveOrganization(orgId, newParentId);
        return super.doJsonDefaultMsg();
    }

    /**
     * 获取组织的所有子级组织ID列表
     * 包含直接和间接的所有下级组织
     *
     * @param orgId 组织ID
     * @return 子级组织ID列表
     */
    @GetMapping("/children/{orgId}")
    @RequireAuth(permissions = {"organization:view"})
    public MyResponseResult<List<Long>> getAllChildrenIds(@PathVariable Long orgId) {
        return super.doJsonOut(organizationService.getAllChildrenIds(orgId));
    }

    /**
     * 获取组织的上级路径
     * 返回从根组织到当前组织的完整路径
     *
     * @param orgId 组织ID
     * @return 组织路径列表
     */
    @GetMapping("/path/{orgId}")
    @RequireAuth(permissions = {"organization:view"})
    public MyResponseResult<List<OrganizationDTO>> getOrganizationPath(@PathVariable Long orgId) {
        return super.doJsonOut(organizationService.getOrganizationPath(orgId));
    }

    /**
     * 校验组织编码是否唯一
     * 用于前端实时校验
     *
     * @param orgCode 组织编码
     * @param excludeId 排除的组织ID（可选，用于更新时排除自身）
     * @return 是否唯一
     */
    @GetMapping("/check-code")
    @RequireAuth(permissions = {"organization:view"})
    public MyResponseResult<Boolean> checkOrgCode(@RequestParam String orgCode,
                                                 @RequestParam(required = false) Long excludeId) {
        return super.doJsonOut(organizationService.isOrgCodeUnique(orgCode, excludeId));
    }
}