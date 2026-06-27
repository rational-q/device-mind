package com.devicemind.broker.session;

import io.netty.channel.Channel;
import lombok.Data;

@Data
public class DeviceSession {

    private String clientId;
    private Channel channel;
    private long connectedAt;
    private long lastHeartbeatAt;
    private int keepAlive;
}
