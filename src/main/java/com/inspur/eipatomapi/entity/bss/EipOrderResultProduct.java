package com.inspur.eipatomapi.entity.bss;

import lombok.Data;

import java.util.List;

@Data
public class EipOrderResultProduct {

    private String orderDetailFlowId;
    private String productSetStatus;
    private String billType;
    private String duration;
    private String durationUnit = "M";
    private String orderWhat = "formal";
    private String orderType = "new";
    private String servStartTime;
    private String servEndTime;
    private String rewardActivity;

    private List<EipOrderProduct> productList;

}
