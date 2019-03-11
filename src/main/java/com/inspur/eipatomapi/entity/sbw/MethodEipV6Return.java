package com.inspur.eipatomapi.entity.sbw;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MethodEipV6Return<T> {

    int httpCode;
    String innerCode;
    String message;
    private T EipV6;
}