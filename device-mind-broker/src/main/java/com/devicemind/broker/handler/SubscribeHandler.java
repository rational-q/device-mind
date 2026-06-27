package com.devicemind.broker.handler;

import com.devicemind.broker.model.SubAckMessage;
import com.devicemind.broker.model.SubscribeMessage;
import com.devicemind.broker.session.SubscriptionManager;
import com.devicemind.common.utils.TraceContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SubscribeHandler extends SimpleChannelInboundHandler<SubscribeMessage> {

    private final SubscriptionManager subscriptionManager;

    public SubscribeHandler(SubscriptionManager subscriptionManager) {
        super(false);
        this.subscriptionManager = subscriptionManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SubscribeMessage msg) {
        TraceContext.set();
        try {
            log.info("收到订阅请求: packetId={}, topics={}",
                    msg.getPacketId(),
                    msg.getSubscriptions().stream().map(s -> s.getTopicFilter() + "@QoS" + s.getMaxQos()).toList());

            List<Integer> returnCodes = new ArrayList<>();
            for (SubscribeMessage.Subscription sub : msg.getSubscriptions()) {
                // 主题过滤器合法性校验（简单校验）
                if (sub.getTopicFilter() == null || sub.getTopicFilter().isBlank()) {
                    returnCodes.add(0x80); // 失败
                    continue;
                }
                // 存入订阅关系
                subscriptionManager.subscribe(ctx.channel(), sub.getTopicFilter());
                // 返回接受的 QoS（取订阅QoS和服务端QoS的较小值，这里固定返回订阅的QoS）
                returnCodes.add(Math.min(sub.getMaxQos(), 1)); // Broker 最大支持 QoS 1
            }

            // 发送 SUBACK
            ByteBuf buf = Unpooled.buffer();
            buf.writeByte(0x90); // SUBACK 固定报头

            // 计算剩余长度
            int remainingLength = 2 + returnCodes.size(); // packetId(2) + 每个返回码(1)
            writeRemainingLength(buf, remainingLength);

            buf.writeShort(msg.getPacketId());
            for (int rc : returnCodes) {
                buf.writeByte(rc);
            }

            ctx.writeAndFlush(buf);
            log.info("订阅完成: topics={}, returnCodes={}",
                    msg.getSubscriptions().stream().map(s -> s.getTopicFilter()).toList(), returnCodes);
        } finally {
            TraceContext.clear();
        }
    }

    private void writeRemainingLength(ByteBuf buf, int length) {
        do {
            int digit = length % 128;
            length /= 128;
            if (length > 0) digit |= 0x80;
            buf.writeByte(digit);
        } while (length > 0);
    }
}
