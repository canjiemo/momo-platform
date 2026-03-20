package io.github.canjiemo.momo.system.controller;

import io.github.canjiemo.base.mymvc.controller.MyBaseController;
import io.github.canjiemo.base.mymvc.data.MyResponseResult;
import io.github.canjiemo.momo.framework.annotation.OperationLog;
import io.github.canjiemo.momo.framework.annotation.RequireAuth;
import io.github.canjiemo.momo.framework.dto.UserCacheInfo;
import io.github.canjiemo.momo.framework.enums.OperationType;
import io.github.canjiemo.momo.framework.utils.SecurityContextUtil;
import io.github.canjiemo.momo.system.dto.*;
import io.github.canjiemo.momo.system.service.IUserService;
import io.github.canjiemo.mycommon.exception.BusinessException;
import io.github.canjiemo.mycommon.pager.Pager;
import io.github.canjiemo.mycommon.pager.PagerHandler;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器
 * 提供用户的增删改查、权限管理、密码管理等功能
 *
 * @author canjiemo@gmail.com
 */
@RestController
@RequestMapping("/system/user")
public class UserController extends MyBaseController {

    @Autowired
    private IUserService userService;

    /**
     * 分页查询用户
     * 支持复杂查询条件、分页、排序
     *
     * @param param 查询参数
     * @return 分页结果
     */
    @PostMapping("/search")
    @RequireAuth(permissions = {"user:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "user",
        description = "分页查询用户"
    )
    public MyResponseResult<Pager<UserDTO>> search(@RequestBody UserQueryParam param) {
        return super.doJsonPagerOut(userService.search(param, PagerHandler.createPager(param)));
    }

    /**
     * 根据ID获取用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    @RequireAuth(permissions = {"user:view"})
    @OperationLog(
        type = OperationType.QUERY,
        module = "user",
        description = "查询用户详情"
    )
    public MyResponseResult<UserDTO> getById(@PathVariable Long id) {
        return super.doJsonOut(userService.getById(id));
    }

    /**
     * 创建用户
     *
     * @param request 创建请求参数
     * @return 操作结果
     */
    @PostMapping("/create")
    @RequireAuth(permissions = {"user:create"})
    @OperationLog(
        type = OperationType.CREATE,
        module = "user",
        description = "创建用户"
    )
    public MyResponseResult create(@Valid @RequestBody UserCreateRequest request) {
        userService.create(request);
        return super.doJsonDefaultMsg();
    }

    /**
     * 更新用户
     *
     * @param request 更新请求参数
     * @return 操作结果
     */
    @PostMapping("/update")
    @RequireAuth(permissions = {"user:update"})
    @OperationLog(
        type = OperationType.UPDATE,
        module = "user",
        description = "更新用户"
    )
    public MyResponseResult update(@Valid @RequestBody UserUpdateRequest request) {
        userService.update(request);
        return super.doJsonDefaultMsg();
    }

    /**
     * 批量删除用户
     *
     * @param request 删除请求参数
     * @return 操作结果
     */
    @PostMapping("/delete")
    @RequireAuth(permissions = {"user:delete"})
    @OperationLog(
        type = OperationType.DELETE,
        module = "user",
        description = "删除用户"
    )
    public MyResponseResult delete(@Valid @RequestBody UserDeleteRequest request) {
        userService.delete(request.getIds());
        return super.doJsonDefaultMsg();
    }

    /**
     * 个人修改密码
     * 用户修改自己的密码，需要验证当前密码
     *
     * @param request 修改密码请求参数
     * @return 操作结果
     */
    @PostMapping("/change-password")
    @RequireAuth(login = true)  // 只需要登录
    @OperationLog(
        type = OperationType.UPDATE,
        module = "user",
        description = "修改个人密码"
    )
    public MyResponseResult changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        // 从安全上下文获取当前用户ID
        UserCacheInfo currentUser = SecurityContextUtil.getCurrentUser();
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new BusinessException("无法获取当前用户信息");
        }

        userService.changePassword(currentUser.getUserId(), request.getCurrentPassword(), request.getNewPassword());
        return super.doJsonDefaultMsg();
    }

    /**
     * 管理员初始化密码
     * 将用户密码重置为系统配置的初始密码
     *
     * @param request 初始化密码请求参数
     * @return 操作结果
     */
    @PostMapping("/init-password")
    @RequireAuth(permissions = {"user:init-password"})
    @OperationLog(
        type = OperationType.UPDATE,
        module = "user",
        description = "初始化用户密码"
    )
    public MyResponseResult initPassword(@Valid @RequestBody InitPasswordRequest request) {
        userService.initPassword(request.getUserId());
        return super.doJsonDefaultMsg();
    }

    /**
     * 管理员重置密码
     * 管理员设置用户为指定密码
     *
     * @param request 重置密码请求参数
     * @return 操作结果
     */
    @PostMapping("/reset-password-admin")
    @RequireAuth(permissions = {"user:reset-password"})
    @OperationLog(
        type = OperationType.UPDATE,
        module = "user",
        description = "管理员重置用户密码"
    )
    public MyResponseResult resetPasswordAdmin(@Valid @RequestBody ResetPasswordRequest request) {
        UserCacheInfo currentUser = SecurityContextUtil.getCurrentUser();
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new BusinessException("无法获取当前用户信息");
        }
        userService.resetPassword(currentUser.getUserId(), request.getNewPassword());
        return super.doJsonDefaultMsg();
    }

    /**
     * 获取个人信息（包含角色和菜单权限）
     */
    @GetMapping("/profile")
    @RequireAuth(login = true)  // 只需要登录
    public MyResponseResult<UserDTO> getProfile() {
        UserDTO userProfile = userService.getCurrentUserProfile();
        return super.doJsonOut(userProfile);
    }
}