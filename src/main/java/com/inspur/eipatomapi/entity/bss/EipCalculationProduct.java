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
    private String productTypeCode;
    private String productName;
    private String instanceCount;
    private String instanceId;
    private List<EipCalculationProductItem> itemList;
}
