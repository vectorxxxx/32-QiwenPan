package com.qiwenshare.common.constant;

public enum PlatformEnum
{
    COMMUNITY(1, "社区"),
    ADMIN(2, "管理端"),
    PAN(3, "网盘"),
    STOCK(4, "股票");

    private int code;
    private String name;

    PlatformEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
