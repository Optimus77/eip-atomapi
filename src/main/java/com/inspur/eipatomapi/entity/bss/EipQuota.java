package com.inspur.eipatomapi.entity.bss;

import lombok.Data;

/**
 * @author: jiasirui
 * @date: 2018/10/24 21:49
 * @description:
 */
@Data
public class EipQuota {
    private String userId;
    private String region;
    private String productLineCode;
    private String productTypeCode;
}
