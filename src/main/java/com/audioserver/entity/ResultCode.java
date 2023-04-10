package com.audioserver.entity;

enum ResultCode {

    SUCCESS(100),
    ERROR(500);

    private Integer code;

    ResultCode(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
