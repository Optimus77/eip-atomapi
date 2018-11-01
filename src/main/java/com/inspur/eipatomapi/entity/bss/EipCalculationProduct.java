package com.inspur.eipatomapi.entity.bss;

import lombok.Data;

import java.util.List;

/**
 * @author: jiasirui
 * @date: 2018/10/24 21:30
 * @description:
 */
@Data
public class EipCalculationProduct {

    private String region;
    private String availableZone;
    private String productTypeCode = "EIP";
    private String productName = "EIP";
    private String instanceCount = "1";
    private String instanceId;
    private List<EipOrderProductItem> itemList;

}
