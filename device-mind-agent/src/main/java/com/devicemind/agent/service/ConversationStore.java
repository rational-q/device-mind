package com.devicemind.agent.service;

import com.devicemind.agent.client.DeepSeekClient;
import org.redisson.api.RList;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 对话上下文存储（Redis 滑动窗口 + AI 摘要记忆）
 */
@Slf4j
@Component
public class ConversationStore {

    private static final String KEY_PREFIX = "chat:session:";
    private static final Duration TTL = Duration.ofMinutes(30);
    private static final int MAX_MSGS = 20;
    private static final int WINDOW_SIZE = 10;
    private static final int SUMMARIZE_BATCH = 10;

    private static final String SUMMARY_PROMPT = """
            将以下多轮对话压缩为一段简洁的摘要（100字以内），只保留关键事实。
            不要写"用户问了"、"AI回答"这种句式，直接概括事实。仅输出摘要文本。""";

    @Autowired
    private RedissonClient redisson;
    @Autowired
    private DeepSeekClient deepSeekClient;

    public String create() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public List<DeepSeekClient.Message> getMessages(String sessionId, String systemPrompt, String userMessage) {
        List<DeepSeekClient.Message> messages = new ArrayList<>();
        messages.add(new DeepSeekClient.Message("system", systemPrompt));

        RBucket<String> summaryBucket = redisson.getBucket(summaryKey(sessionId));
        String summary = summaryBucket.get();
        if (summary != null && !summary.isBlank()) {
            messages.add(new DeepSeekClient.Message("system", "【历史对话摘要】" + summary));
        }

        RList<MsgEntry> msgList = redisson.getList(msgsKey(sessionId), JsonCodec.INSTANCE);
        for (MsgEntry m : msgList) {
            messages.add(new DeepSeekClient.Message(m.role, m.content));
        }

        messages.add(new DeepSeekClient.Message("user", userMessage));
        return messages;
    }

    public void saveRound(String sessionId, String userMsg, String assistantMsg) {
        RList<MsgEntry> list = redisson.getList(msgsKey(sessionId), JsonCodec.INSTANCE);
        list.add(new MsgEntry("user", userMsg));
        list.add(new MsgEntry("assistant", assistantMsg));
        list.expire(TTL);

        if (list.size() > MAX_MSGS) {
            summarizeInBackground(sessionId);
        }
    }

    private void summarizeInBackground(String sessionId) {
        try {
            RList<MsgEntry> list = redisson.getList(msgsKey(sessionId), JsonCodec.INSTANCE);
            if (list.size() <= MAX_MSGS) return;

            List<MsgEntry> batch = list.range(0, SUMMARIZE_BATCH - 1);
            StringBuilder old = new StringBuilder();
            for (MsgEntry m : batch) old.append(m.role).append(": ").append(m.content).append("\n");

            RBucket<String> summaryBucket = redisson.getBucket(summaryKey(sessionId));
            String existing = summaryBucket.get();
            if (existing != null && !existing.isBlank()) {
                old.insert(0, "之前的摘要: " + existing + "\n\n新的对话:\n");
            }

            String summary = deepSeekClient.chat(SUMMARY_PROMPT, old.toString());
            if (summary != null && !summary.isBlank()) {
                summaryBucket.set(summary, TTL);
                list.trim(WINDOW_SIZE, -1);
                log.debug("会话 {} 摘要压缩完成", sessionId);
            }
        } catch (Exception e) {
            log.warn("摘要压缩失败: sessionId={}", sessionId, e);
        }
    }

    public void remove(String sessionId) {
        redisson.getList(msgsKey(sessionId)).delete();
        redisson.getBucket(summaryKey(sessionId)).delete();
    }

    private String msgsKey(String id) { return KEY_PREFIX + id + ":msgs"; }
    private String summaryKey(String id) { return KEY_PREFIX + id + ":summary"; }

    /** Redisson JSON 编解码的消息条目 */
    public static class MsgEntry {
        public String role;
        public String content;
        public MsgEntry() {}
        public MsgEntry(String role, String content) { this.role = role; this.content = content; }
    }

    /** 共用 JsonJacksonCodec（Redisson 内置） */
    @Component
    static class JsonCodec extends org.redisson.codec.JsonJacksonCodec {
        static final JsonCodec INSTANCE = new JsonCodec();
    }
}
