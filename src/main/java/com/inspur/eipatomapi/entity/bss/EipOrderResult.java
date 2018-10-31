package com.inspur.eipatomapi.entity.bss;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.List;

/**
 * @author: jiasirui
 * @date: 2018/10/24 22:28
 * @description:
 */
@Data
public class EipOrderResult {

    private String userId;
    private String productLineCode="EIP";
    private String consoleOrderFlowId;
    private String orderId;
    private List<EipOrderProduct> productSetList;

}
