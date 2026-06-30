package com.devicemind.broker.handler;

import com.devicemind.broker.model.UnsubAckMessage;
import com.devicemind.broker.model.UnsubscribeMessage;
import com.devicemind.broker.session.SubscriptionManager;
import com.devicemind.common.utils.TraceContext;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UnsubscribeHandler extends SimpleChannelInboundHandler<UnsubscribeMessage> {

    private final SubscriptionManager subscriptionManager;

    public UnsubscribeHandler(SubscriptionManager subscriptionManager) {
        super(false);
        this.subscriptionManager = subscriptionManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, UnsubscribeMessage msg) {
        TraceContext.set();
        try {
            log.info("收到取消订阅请求: packetId={}, topics={}", msg.getPacketId(), msg.getTopicFilters());

            for (String topicFilter : msg.getTopicFilters()) {
                subscriptionManager.unsubscribe(ctx.channel(), topicFilter);
            }

            ctx.writeAndFlush(new UnsubAckMessage(msg.getPacketId()));
            log.info("取消订阅完成: topics={}", msg.getTopicFilters());
        } finally {
            TraceContext.clear();
        }
    }
}
