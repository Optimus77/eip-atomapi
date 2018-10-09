package com.inspur.eipatomapi.util;

import com.inspur.eipatomapi.entity.ReturnMsg;

public class ReturnMsgUtil {

    public static <T> ReturnMsg success(T t) {
        return ReturnMsg.builder().code("200").message("success").eip(t).build();
    }

    public static <T> ReturnMsg listsuccess(T t) {
        return ReturnMsg.builder().code("200").message("success").data(t).build();
    }


    public static ReturnMsg success() {
        return success(null);
    }

    public static ReturnMsg error(String code, String msg) {
        return ReturnMsg.builder().code(code).message(msg).build();
    }
}
