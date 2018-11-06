package com.inspur.eipatomapi.entity.fw;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FwResponseException {
    private String code;
    private String message;
    private String stack;

}
