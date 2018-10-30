package com.inspur.eipatomapi.entity.bss;

import lombok.Data;

import java.util.List;

/**
 * @author: jiasirui
 * @date: 2018/10/24 21:21
 * @description:
 */

@Data
public class EipCalculation {

    private String userId;
    private String productLineCode="EIP";
    private String setCount = "1";
    private String billType = "monthly";
    private String duration;
    private String durationUnit = "M";
    private String orderWhat = "formal";
    private String orderType = "new";
    private String serviceStartTime;
    private String serviceEndTime;
    private String rewardActivity;
    private List<EipCalculationProduct> productList;



}
