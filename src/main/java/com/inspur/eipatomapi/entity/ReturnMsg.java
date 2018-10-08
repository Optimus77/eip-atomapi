package com.inspur.eipatomapi.entity;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ReturnMsg<T> {
    private String code;
    private String message;
    private T eip;

}
