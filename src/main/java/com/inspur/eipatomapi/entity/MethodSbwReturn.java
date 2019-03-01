package com.inspur.eipatomapi.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MethodSbwReturn<T> {

    int httpCode;
    String innerCode;
    String message;
    private T sbw;
}