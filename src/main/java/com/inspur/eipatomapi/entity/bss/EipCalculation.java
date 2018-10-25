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
    private String setCount;
    private String billType;
    private String duration;
    private String durationUnit;
    private String orderWhat;
    private String orderType;
    private String serviceStartTime;
    private String serviceEndTime;
    private String rewardActivity;
    private List<EipCalculationProduct> productList;



}
