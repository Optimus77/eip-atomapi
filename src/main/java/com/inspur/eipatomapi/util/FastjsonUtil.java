package com.inspur.eipatomapi.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

import javax.validation.constraints.NotBlank;

public class FastjsonUtil {
    public static String toJSONString(@NotBlank Object object){
        String[] name=object.getClass().getName().split("\\.");
        return "{\""+name[name.length-1].toLowerCase()+"\":"
                + JSONObject.toJSONString(object, SerializerFeature.QuoteFieldNames)+"}";
    }

    public static String toJOSNStringNull(JSONObject jsonObject){
        return JSON.toJSONString(jsonObject, SerializerFeature.WriteMapNullValue);
    }
}
