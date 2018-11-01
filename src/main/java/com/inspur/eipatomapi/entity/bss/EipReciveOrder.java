package com.inspur.eipatomapi.entity.bss;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.List;

@Data
public class EipReciveOrder {
    private String token;
    private String consoleOrderFlowId;
    private String orderStatus;
    private String statusTime;
    private String orderId;
    private List<String> orderDetailFlowIdList;
    private EipOrder returnConsoleMessage;

}
