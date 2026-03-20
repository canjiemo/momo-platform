package io.github.canjiemo.momo.ai.conversation.dto;

import io.github.canjiemo.momo.ai.conversation.entity.AiConversation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 游标分页的对话历史响应
 * <p>
 * 使用方式：首次不传 cursor，取最新一页；
 * 加载更多时将 nextCursor 作为下一次请求的 cursor 参数传入。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationCursorResult {

    /** 当前页消息列表，按时间升序（旧→新） */
    private List<AiConversation> messages;

    /**
     * 下一页游标：当前页最老那条消息的 id。
     * 为 null 表示已无更多历史记录。
     */
    private Long nextCursor;

    /** 是否还有更多历史记录 */
    private boolean hasMore;
}
