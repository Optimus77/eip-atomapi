package com.inspur.eipatomapi.util;

import com.inspur.eipatomapi.entity.ReturnMsg;

public class ReturnMsgUtil {

    public static <T> ReturnMsg success(T t) {
        ReturnMsg<Object> returnMsg = ReturnMsg.builder().code("200").msg("success").eip(t).build();

        return returnMsg;
    }


    public static ReturnMsg success() {
        return success(null);
    }

    public static ReturnMsg error(String code, String msg) {
        return ReturnMsg.builder().code(code).msg(msg).build();
    }
}
