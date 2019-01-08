package com.inspur.eipatomapi.entity.fw;

import lombok.Data;


@Data
public class FwResponseException {
    private String code;
    private String message;
    private String stack;

}
