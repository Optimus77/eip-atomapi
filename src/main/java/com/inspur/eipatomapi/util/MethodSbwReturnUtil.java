package com.inspur.eipatomapi.util;


import com.inspur.eipatomapi.entity.MethodSbwReturn;

public class MethodSbwReturnUtil {

    public static <T> MethodSbwReturn success(T t) {
        return MethodSbwReturn.builder().httpCode(200).innerCode(ReturnStatus.SC_OK).sbw(t).build();
    }


    public static MethodSbwReturn success() {
        return success(null);
    }

    public static MethodSbwReturn error(int httpCode, String code, String msg) {
        return MethodSbwReturn.builder().httpCode(httpCode).innerCode(code).message(msg).build();
    }

}
