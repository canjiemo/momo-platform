package io.github.canjiemo.momo.ai.session.controller;

import io.github.canjiemo.base.mymvc.controller.MyBaseController;
import io.github.canjiemo.base.mymvc.data.MyResponseResult;
import io.github.canjiemo.momo.ai.session.dto.AiSessionDTO;
import io.github.canjiemo.momo.ai.session.service.IAiSessionService;
import io.github.canjiemo.momo.framework.annotation.RequireAuth;
import io.github.canjiemo.momo.framework.dto.UserCacheInfo;
import io.github.canjiemo.momo.framework.utils.SecurityContextUtil;
import io.github.canjiemo.mycommon.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/ai/session")
@RequireAuth(login = true)
public class AiSessionController extends MyBaseController {

    @Autowired
    private IAiSessionService sessionService;

    /** 新建会话 */
    @PostMapping("/create")
    public MyResponseResult<AiSessionDTO> create() {
        UserCacheInfo user = SecurityContextUtil.getCurrentUser();
        return doJsonOut(sessionService.create(user.getUserId(), user.getTenantId()));
    }

    /** 当前用户会话列表 */
    @GetMapping("/list")
    public MyResponseResult<List<AiSessionDTO>> list() {
        UserCacheInfo user = SecurityContextUtil.getCurrentUser();
        return doJsonOut(sessionService.listByUser(user.getUserId(), user.getTenantId()));
    }

    /** 重命名 */
    @PutMapping("/{sessionId}/title")
    public MyResponseResult<Void> rename(@PathVariable String sessionId,
                                         @RequestBody Map<String, String> body) {
        String title = body.get("title");
        if (title == null || title.isBlank()) throw new BusinessException("标题不能为空");
        UserCacheInfo user = SecurityContextUtil.getCurrentUser();
        sessionService.rename(sessionId, user.getUserId(), title);
        return doJsonOut(null);
    }

    /** 删除会话及其所有消息 */
    @DeleteMapping("/{sessionId}")
    public MyResponseResult<Void> delete(@PathVariable String sessionId) {
        UserCacheInfo user = SecurityContextUtil.getCurrentUser();
        sessionService.delete(sessionId, user.getUserId());
        return doJsonOut(null);
    }
}
