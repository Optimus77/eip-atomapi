package com.inspur.eipatomapi.entity.bss;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import lombok.Data;

import java.util.List;

/**
 * @author: jiasirui
 * @date: 2018/10/24 22:28
 * @description:
 */
@Data
public class EipOrder {

    private String userId;
    private String productLineCode;
    private String setCount;
    private String consoleOrderFlowId;
    private List   flowIdList;
    private String billType;
    private String duration;
    private String durationUnit;
    private String orderWhat;
    private String orderType;
    private String servStartTime;
    private String servEndTime;
    private String rewardActivity;
    private String consoleCustomization;
    private String totalMoney;
    private List<EipOrderProduct> productList;
}
