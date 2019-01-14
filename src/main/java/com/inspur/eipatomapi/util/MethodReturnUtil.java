package com.inspur.eipatomapi.util;


import com.inspur.eipatomapi.entity.MethodReturn;
import com.inspur.eipatomapi.entity.MethodSbwReturn;

public class MethodReturnUtil {

    /**
     * eip success
     * @param t
     * @param <T>
     * @return
     */
    public static <T> MethodReturn success(T t) {
        return MethodReturn.builder().httpCode(200).innerCode(ReturnStatus.SC_OK).eip(t).build();
    }

    /**
     * sbw success
     * @param t
     * @param <T>
     * @return
     */
    public static <T> MethodSbwReturn successSbw(T t) {
        return MethodSbwReturn.builder().httpCode(200).innerCode(ReturnStatus.SC_OK).sbw(t).build();
    }
    public static MethodSbwReturn success() {
        return successSbw(null);
    }

    /**
     * eip error
     * @param httpCode
     * @param code
     * @param msg
     * @return
     */
    public static MethodSbwReturn error(int httpCode, String code, String msg) {
        return MethodSbwReturn.builder().httpCode(httpCode).innerCode(code).message(msg).build();
    }

    /**
     * sbw error
     * @param httpCode
     * @param code
     * @param msg
     * @return
     */
    public static MethodSbwReturn errorSbw(int httpCode, String code, String msg) {
        return MethodSbwReturn.builder().httpCode(httpCode).innerCode(code).message(msg).build();
    }

}
