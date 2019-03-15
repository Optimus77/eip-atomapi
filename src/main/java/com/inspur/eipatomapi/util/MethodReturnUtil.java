package com.inspur.eipatomapi.util;


import com.inspur.eipatomapi.entity.MethodReturn;
import com.inspur.eipatomapi.entity.MethodSbwReturn;
import com.inspur.eipatomapi.entity.sbw.MethodEipV6Return;

public class MethodReturnUtil {

    public static <T> MethodReturn success(T t) {
        return MethodReturn.builder().httpCode(200).innerCode(ReturnStatus.SC_OK).eip(t).build();
    }
    /*sbw*/
    public static <T> MethodSbwReturn successSbw(T t) {
        return MethodSbwReturn.builder().httpCode(200).innerCode(ReturnStatus.SC_OK).sbw(t).build();
    }

    /*eipv6*/
    public static <T> MethodEipV6Return successEipV6(T t) {
        return MethodEipV6Return.builder().httpCode(200).innerCode(ReturnStatus.SC_OK).EipV6(t).build();
    }

    public static MethodReturn success() {
        return success(null);
    }
    /*sbw*/
    public static MethodSbwReturn successSbw() {
        return successSbw(null);
    }

    /*eipv6*/
    public static MethodEipV6Return successEipV6() {
        return successEipV6(null);
    }

    public static MethodReturn error(int httpCode, String code, String msg) {
        return MethodReturn.builder().httpCode(httpCode).innerCode(code).message(msg).build();
    }

    public static MethodSbwReturn errorSbw(int httpCode, String code, String msg) {
        return MethodSbwReturn.builder().httpCode(httpCode).innerCode(code).message(msg).build();
    }

    public static MethodEipV6Return errorEipV6(int httpCode, String code, String msg) {
        return MethodEipV6Return.builder().httpCode(httpCode).innerCode(code).message(msg).build();
    }

}
