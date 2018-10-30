package com.inspur.eipatomapi.entity.bss;

import lombok.Data;

import java.util.List;

/**
 * @author: jiasirui
 * @date: 2018/10/24 22:35
 * @description:
 */
@Data
public class EipOrderProduct {

    private String region;
    private String availableZone;
    private String productType = "EIP";
    private String productName = "EIP";
    private String instanceCount = "1";
    private String instanceId;
    private List<EipOrderProductItem> itemList;
}
