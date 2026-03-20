package io.github.canjiemo.momo.system.service;

import io.github.canjiemo.momo.system.dto.*;
import io.github.canjiemo.mycommon.pager.Pager;

import java.util.List;

/**
 * 组织架构管理服务接口
 * 提供组织架构的增删改查、树形结构管理、人员关联等功能
 *
 * @author canjiemo@gmail.com
 */
public interface IOrganizationService {

    /**
     * 分页查询组织架构
     * 支持复杂查询条件、分页、排序
     *
     * @param param 查询参数
     * @param pager 分页信息
     * @return 分页结果
     */
    Pager<OrganizationDTO> search(OrganizationQueryParam param, Pager<OrganizationDTO> pager);

    /**
     * 获取完整的组织架构树形结构
     * 返回所有启用状态的组织，按排序字段排序
     *
     * @return 组织架构树形结构列表
     */
    List<OrganizationTreeVO> getOrganizationTree();

    /**
     * 获取指定组织的子组织树
     * 根据父组织ID返回其下属的组织树
     *
     * @param parentId 父组织ID，"0"表示获取顶级组织
     * @return 子组织树形结构
     */
    List<OrganizationTreeVO> getChildrenTree(String parentId);

    /**
     * 获取所有组织列表（不分页）
     * 用于管理界面的下拉选择框等场景
     *
     * @return 组织列表
     */
    List<OrganizationDTO> list();

    /**
     * 根据ID获取组织详情
     * 包含组织基本信息、负责人信息、统计数据等
     *
     * @param id 组织ID
     * @return 组织详情
     */
    OrganizationDTO getById(Long id);

    /**
     * 创建组织
     * 包括编码唯一性校验、父组织验证等操作
     *
     * @param request 创建请求参数
     */
    void create(OrganizationCreateRequest request);

    /**
     * 更新组织信息
     *
     * @param request 更新请求参数
     */
    void update(OrganizationUpdateRequest request);

    /**
     * 批量删除组织（逻辑删除）
     * 会检查是否有子组织和关联用户
     *
     * @param ids 组织ID数组
     */
    void delete(String[] ids);

    /**
     * 移动组织到新的父组织下
     * 支持跨层级移动，会验证循环引用
     *
     * @param orgId 要移动的组织ID
     * @param newParentId 新的父组织ID，"0"表示移动到顶级
     */
    void moveOrganization(Long orgId, String newParentId);

    /**
     * 获取组织的所有子级组织ID列表
     * 包含直接和间接的所有下级组织
     *
     * @param orgId 组织ID
     * @return 子级组织ID列表
     */
    List<Long> getAllChildrenIds(Long orgId);

    /**
     * 获取组织的上级路径
     * 返回从根组织到当前组织的完整路径
     *
     * @param orgId 组织ID
     * @return 组织路径列表
     */
    List<OrganizationDTO> getOrganizationPath(Long orgId);

    /**
     * 校验组织编码是否唯一
     *
     * @param orgCode 组织编码
     * @param excludeId 排除的组织ID（用于更新时排除自身）
     * @return 是否唯一
     */
    boolean isOrgCodeUnique(String orgCode, Long excludeId);
}