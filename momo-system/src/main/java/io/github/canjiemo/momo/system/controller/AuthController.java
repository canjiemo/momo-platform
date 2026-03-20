package io.github.canjiemo.momo.system.controller;

import io.github.canjiemo.base.mymvc.controller.MyBaseController;
import io.github.canjiemo.base.mymvc.data.MyResponseResult;
import io.github.canjiemo.momo.framework.annotation.OperationLog;
import io.github.canjiemo.momo.framework.annotation.RequireAuth;
import io.github.canjiemo.momo.framework.enums.OperationType;
import io.github.canjiemo.momo.system.dto.CaptchaResponse;
import io.github.canjiemo.momo.system.dto.LoginRequest;
import io.github.canjiemo.momo.system.dto.LoginResponse;
import io.github.canjiemo.momo.system.service.IAuthService;
import io.github.canjiemo.momo.system.service.ICaptchaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * 认证控制器
 *
 * @author canjiemo@gmail.com
 */
@RestController
@RequestMapping("/auth")
@RequireAuth(login = false)  // 整个Controller不需要登录
public class AuthController extends MyBaseController {

    @Autowired
    private IAuthService authService;

    @Autowired
    private ICaptchaService captchaService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @OperationLog(
        type = OperationType.LOGIN,
        module = "auth",
        description = "用户登录"
    )
    public MyResponseResult<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        LoginResponse result = authService.login(request, ip);
        return super.doJsonOut(result);
    }

    /**
     * 获取验证码
     */
    @GetMapping("/captcha")
    public MyResponseResult<CaptchaResponse> getCaptcha() {
        CaptchaResponse result = captchaService.generateCaptcha();
        return super.doJsonOut(result);
    }


    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0];
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}