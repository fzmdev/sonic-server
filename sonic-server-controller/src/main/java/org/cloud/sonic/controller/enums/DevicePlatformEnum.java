package org.cloud.sonic.controller.enums;

public enum DevicePlatformEnum {
    ANDROID(1),
    IOS(2);

    private final int value;

    DevicePlatformEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
