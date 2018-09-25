package com.inspur.eipatomapi.entity;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FwResponseException {
    private String code;
    private String message;
    private String stack;

}
