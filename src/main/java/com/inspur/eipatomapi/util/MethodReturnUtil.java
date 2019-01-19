package com.inspur.eipatomapi.util;


import com.inspur.eipatomapi.entity.MethodReturn;
import com.inspur.eipatomapi.entity.MethodSbwReturn;

public class MethodReturnUtil {

    public static <T> MethodReturn success(T t) {
        return MethodReturn.builder().httpCode(200).innerCode(ReturnStatus.SC_OK).eip(t).build();
    }
    /*sbw*/
    public static <T> MethodSbwReturn successSbw(T t) {
        return MethodSbwReturn.builder().httpCode(200).innerCode(ReturnStatus.SC_OK).sbw(t).build();
    }


    public static MethodReturn success() {
        return success(null);
    }
    /*sbw*/
    public static MethodSbwReturn successSbw() {
        return successSbw(null);
    }

    public static MethodReturn error(int httpCode, String code, String msg) {
        return MethodReturn.builder().httpCode(httpCode).innerCode(code).message(msg).build();
    }

    public static MethodSbwReturn errorSbw(int httpCode, String code, String msg) {
        return MethodSbwReturn.builder().httpCode(httpCode).innerCode(code).message(msg).build();
    }

}
