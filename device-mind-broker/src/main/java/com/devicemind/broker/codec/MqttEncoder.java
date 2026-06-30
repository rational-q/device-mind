package com.devicemind.broker.codec;

import com.devicemind.broker.model.ConnAckMessage;
import com.devicemind.broker.model.DisconnectMessage;
import com.devicemind.broker.model.SubAckMessage;
import com.devicemind.broker.model.UnsubAckMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * MQTT 协议编码器 — 将报文对象编码为字节流
 */
public class MqttEncoder extends MessageToByteEncoder<Object> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) {
        if (msg instanceof ConnAckMessage m) {
            encodeConnAck(m, out);
        } else if (msg instanceof SubAckMessage m) {
            encodeSubAck(m, out);
        } else if (msg instanceof UnsubAckMessage m) {
            encodeUnsubAck(m, out);
        } else if (msg instanceof DisconnectMessage) {
            out.writeByte(0xE0);
            out.writeByte(0x00);
        } else if (msg instanceof ByteBuf buf) {
            out.writeBytes(buf);
        }
    }

    private void encodeConnAck(ConnAckMessage msg, ByteBuf out) {
        out.writeByte(0x20);
        out.writeByte(0x02);
        out.writeByte(msg.isSessionPresent() ? 1 : 0);
        out.writeByte(msg.getReturnCode());
    }

    private void encodeSubAck(SubAckMessage msg, ByteBuf out) {
        int remainingLength = 2 + msg.getReturnCodes().size();
        out.writeByte(0x90);
        writeRemainingLength(out, remainingLength);
        out.writeShort(msg.getPacketId());
        for (int rc : msg.getReturnCodes()) {
            out.writeByte(rc);
        }
    }

    private void encodeUnsubAck(UnsubAckMessage msg, ByteBuf out) {
        out.writeByte(0xA0);
        writeRemainingLength(out, 2);
        out.writeShort(msg.getPacketId());
    }

    public static void writeRemainingLength(ByteBuf buf, int length) {
        do {
            int digit = length % 128;
            length /= 128;
            if (length > 0) {
                digit |= 0x80;
            }
            buf.writeByte(digit);
        } while (length > 0);
    }
}
