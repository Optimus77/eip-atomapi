package com.inspur.eipatomapi.entity.bss;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.List;


@Data
public class EipOrderResult {

    private String userId;
    private String productLineCode="EIP";
    private String consoleOrderFlowId;
    private String orderId;
    private List<EipOrderResultProduct> productSetList;

}
