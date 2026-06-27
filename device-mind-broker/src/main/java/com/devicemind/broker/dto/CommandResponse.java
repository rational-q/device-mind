package com.devicemind.broker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CommandResponse {
    private boolean success;
    private String message;
}
