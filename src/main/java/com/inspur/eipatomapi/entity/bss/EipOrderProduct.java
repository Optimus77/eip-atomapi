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
    private String productType;
    private String productName;
    private String instanceCount;
    private String instanceId;
    private List<EipOrderProductItem> itemList;
}
