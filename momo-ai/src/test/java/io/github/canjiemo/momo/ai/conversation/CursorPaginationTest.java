package io.github.canjiemo.momo.ai.conversation;

import io.github.canjiemo.momo.ai.conversation.dto.ConversationCursorResult;
import io.github.canjiemo.momo.ai.conversation.entity.AiConversation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 游标分页核心逻辑单元测试
 * <p>
 * 不依赖 Spring 容器，直接验证 getHistory 中的
 * "多取一条探测 hasMore → trim → reverse → nextCursor" 逻辑。
 */
class CursorPaginationTest {

    // -----------------------------------------------------------------------
    // 帮助方法：模拟 Service 内部的游标分页后处理逻辑
    // -----------------------------------------------------------------------

    /**
     * 模拟从数据库按 id DESC 取 size+1 条后的处理逻辑。
     *
     * @param dbRows  数据库返回的数据（已按 id DESC 排列）
     * @param size    请求页大小
     */
    private ConversationCursorResult applyPaginationLogic(List<AiConversation> dbRows, int size) {
        boolean hasMore = dbRows.size() > size;
        List<AiConversation> rows = hasMore ? new ArrayList<>(dbRows.subList(0, size)) : new ArrayList<>(dbRows);
        Collections.reverse(rows);
        Long nextCursor = hasMore ? rows.get(0).getId() : null;
        return new ConversationCursorResult(rows, nextCursor, hasMore);
    }

    /** 构造 id 为指定值的消息 */
    private AiConversation msg(long id) {
        AiConversation c = new AiConversation();
        c.setId(id);
        c.setContent("msg-" + id);
        return c;
    }

    /** 构造按 id DESC 排列的消息列表 */
    private List<AiConversation> descList(long... ids) {
        List<AiConversation> list = new ArrayList<>();
        for (long id : ids) list.add(msg(id));
        return list;
    }

    // -----------------------------------------------------------------------
    // 测试用例
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("首次加载：数据条数 < 页大小，hasMore=false，nextCursor=null")
    void firstLoad_lessThanPageSize() {
        // DB 返回 3 条（size+1=6，实际只有 3 条）
        List<AiConversation> dbRows = descList(3, 2, 1);
        ConversationCursorResult result = applyPaginationLogic(dbRows, 5);

        assertFalse(result.isHasMore());
        assertNull(result.getNextCursor());
        assertEquals(3, result.getMessages().size());
        // 验证升序（旧→新）
        assertEquals(1L, result.getMessages().get(0).getId());
        assertEquals(3L, result.getMessages().get(2).getId());
    }

    @Test
    @DisplayName("首次加载：数据条数 == 页大小，hasMore=false")
    void firstLoad_exactPageSize() {
        List<AiConversation> dbRows = descList(5, 4, 3, 2, 1);
        ConversationCursorResult result = applyPaginationLogic(dbRows, 5);

        assertFalse(result.isHasMore());
        assertNull(result.getNextCursor());
        assertEquals(5, result.getMessages().size());
        assertEquals(1L, result.getMessages().get(0).getId());
        assertEquals(5L, result.getMessages().get(4).getId());
    }

    @Test
    @DisplayName("首次加载：DB 返回 size+1 条，hasMore=true，nextCursor 为本页最老 id")
    void firstLoad_hasMoreData() {
        // size=5，DB 返回 6 条（id: 10..5），第 6 条仅用于探测
        List<AiConversation> dbRows = descList(10, 9, 8, 7, 6, 5);
        ConversationCursorResult result = applyPaginationLogic(dbRows, 5);

        assertTrue(result.isHasMore());
        assertEquals(5, result.getMessages().size());
        // 本页消息升序：6,7,8,9,10
        assertEquals(6L, result.getMessages().get(0).getId());
        assertEquals(10L, result.getMessages().get(4).getId());
        // nextCursor = 本页最老消息 id = 6
        assertEquals(6L, result.getNextCursor());
    }

    @Test
    @DisplayName("加载更多：带 cursor=6，本页最老 id=1，hasMore=false")
    void loadMore_lastPage() {
        // 模拟 WHERE id < 6 ORDER BY id DESC LIMIT 6（size+1=6）
        // 实际只有 id 1~5，DB 返回 5 条
        List<AiConversation> dbRows = descList(5, 4, 3, 2, 1);
        ConversationCursorResult result = applyPaginationLogic(dbRows, 5);

        assertFalse(result.isHasMore());
        assertNull(result.getNextCursor());
        assertEquals(5, result.getMessages().size());
        assertEquals(1L, result.getMessages().get(0).getId());
        assertEquals(5L, result.getMessages().get(4).getId());
    }

    @Test
    @DisplayName("加载更多：中间页，hasMore=true，nextCursor 正确指向下一页")
    void loadMore_middlePage() {
        // 模拟 WHERE id < 20 ORDER BY id DESC LIMIT 6（size=5，size+1=6）
        // DB 返回：19,18,17,16,15,14（第 6 条 14 用于探测）
        List<AiConversation> dbRows = descList(19, 18, 17, 16, 15, 14);
        ConversationCursorResult result = applyPaginationLogic(dbRows, 5);

        assertTrue(result.isHasMore());
        assertEquals(5, result.getMessages().size());
        // 本页升序：15,16,17,18,19
        assertEquals(15L, result.getMessages().get(0).getId());
        assertEquals(19L, result.getMessages().get(4).getId());
        // nextCursor = 15（下次请求带 cursor=15，取 id < 15 的数据）
        assertEquals(15L, result.getNextCursor());
    }

    @Test
    @DisplayName("空会话：无任何消息，hasMore=false，messages 为空列表")
    void emptySession() {
        List<AiConversation> dbRows = new ArrayList<>();
        ConversationCursorResult result = applyPaginationLogic(dbRows, 20);

        assertFalse(result.isHasMore());
        assertNull(result.getNextCursor());
        assertTrue(result.getMessages().isEmpty());
    }

    @Test
    @DisplayName("单条消息：hasMore=false，nextCursor=null，消息顺序正确")
    void singleMessage() {
        List<AiConversation> dbRows = descList(42);
        ConversationCursorResult result = applyPaginationLogic(dbRows, 20);

        assertFalse(result.isHasMore());
        assertNull(result.getNextCursor());
        assertEquals(1, result.getMessages().size());
        assertEquals(42L, result.getMessages().get(0).getId());
    }
}
