package com.devicemind.broker.session;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * MQTT 订阅关系管理
 * <p>
 * 维护 topic filter → 订阅者 Channel 的映射，支持通配符匹配。
 */
@Slf4j
@Component
public class SubscriptionManager {

    /** topicFilter → 订阅的 Channel 集合 */
    private final ConcurrentHashMap<String, Set<Channel>> subscriptions = new ConcurrentHashMap<>();

    /** channelId → 该 channel 订阅的 topicFilter 列表（用于取消订阅时清理） */
    private final ConcurrentHashMap<String, Set<String>> channelTopics = new ConcurrentHashMap<>();

    /**
     * 添加订阅关系
     */
    public void subscribe(Channel channel, String topicFilter) {
        String channelId = channel.id().asLongText();

        subscriptions.computeIfAbsent(topicFilter, k -> new CopyOnWriteArraySet<>()).add(channel);
        channelTopics.computeIfAbsent(channelId, k -> new CopyOnWriteArraySet<>()).add(topicFilter);

        log.debug("订阅成功: channelId={}, topicFilter={}", channelId, topicFilter);
    }

    /**
     * 取消单个订阅
     */
    public void unsubscribe(Channel channel, String topicFilter) {
        String channelId = channel.id().asLongText();

        Set<Channel> subs = subscriptions.get(topicFilter);
        if (subs != null) {
            subs.remove(channel);
            if (subs.isEmpty()) {
                subscriptions.remove(topicFilter);
            }
        }

        Set<String> topics = channelTopics.get(channelId);
        if (topics != null) {
            topics.remove(topicFilter);
            if (topics.isEmpty()) {
                channelTopics.remove(channelId);
            }
        }

        log.debug("取消订阅: channelId={}, topicFilter={}", channelId, topicFilter);
    }

    /**
     * 清理某个 Channel 的所有订阅（设备断开时调用）
     */
    public void removeChannel(Channel channel) {
        String channelId = channel.id().asLongText();
        Set<String> topics = channelTopics.remove(channelId);
        if (topics != null) {
            for (String topicFilter : topics) {
                Set<Channel> subs = subscriptions.get(topicFilter);
                if (subs != null) {
                    subs.remove(channel);
                    if (subs.isEmpty()) {
                        subscriptions.remove(topicFilter);
                    }
                }
            }
            log.debug("清理下线设备的订阅: channelId={}, 订阅数={}", channelId, topics.size());
        }
    }

    /**
     * 根据 topic 查找所有匹配的订阅者 Channel
     * <p>
     * 支持通配符：+（单层）、#（多层）
     */
    public List<Channel> getSubscribers(String topic) {
        return subscriptions.entrySet().stream()
                .filter(entry -> matchTopic(entry.getKey(), topic))
                .flatMap(entry -> entry.getValue().stream())
                .filter(Channel::isActive)
                .distinct()
                .toList();
    }

    /**
     * MQTT topic 通配符匹配
     */
    private boolean matchTopic(String filter, String topic) {
        if (filter.equals(topic)) return true;
        if (filter.equals("#")) return true;

        String[] filterParts = filter.split("/", -1);
        String[] topicParts = topic.split("/", -1);

        for (int i = 0; i < filterParts.length; i++) {
            if (i >= topicParts.length) {
                // 如果 filter 以 # 结尾且前面都已匹配
                return filterParts[i].equals("#");
            }
            if (filterParts[i].equals("+")) continue;
            if (filterParts[i].equals("#")) return true;
            if (!filterParts[i].equals(topicParts[i])) return false;
        }

        return filterParts.length == topicParts.length;
    }

    /** 当前订阅关系数 */
    public int getSubscriptionCount() {
        return subscriptions.size();
    }
}
