package com.inspur.eipatomapi.util;

import com.inspur.eipatomapi.entity.MethodReturn;

public class MethodReturnUtil {

    public static <T> MethodReturn success(T t) {
        return MethodReturn.builder().httpCode(200).innerCode(ReturnStatus.SC_OK).eip(t).build();
    }


    public static MethodReturn success() {
        return success(null);
    }

    public static MethodReturn error(int httpCode, String code, String msg) {
        return MethodReturn.builder().httpCode(httpCode).innerCode(code).message(msg).build();
    }

}
