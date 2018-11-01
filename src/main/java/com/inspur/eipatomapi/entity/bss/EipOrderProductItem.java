package com.inspur.eipatomapi.entity.bss;

import lombok.Data;

/**
 * @author: jiasirui
 * @date: 2018/10/24 22:37
 * @description:
 */
@Data
public class EipOrderProductItem {

    private String code;
    private String name;
    private String unit;
    private String value;
    private String type;
}
