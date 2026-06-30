package com.devicemind.common.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public enum YesNo {
    YES("Y", "是"),
    NO("N", "否");

    private final String code;
    private final String name;

    YesNo(String code, String name) {
        this.code = code;
        this.name = name;
    }

    private static volatile List<String> codes;

    public static List<String> codes() {
        if (codes == null) {
            synchronized (YesNo.class) {
                if (codes == null) {
                    codes = Arrays.stream(values()).map(i -> i.code).collect(Collectors.toList());
                }
            }
        }
        return codes;
    }
}
