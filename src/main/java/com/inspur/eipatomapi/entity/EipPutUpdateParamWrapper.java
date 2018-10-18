package com.inspur.eipatomapi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;

/**
 * @author: jiasirui
 * @date: 2018/10/18 15:34
 * @description:
 */
@Getter
@Setter
@Nullable
public class EipPutUpdateParamWrapper {

    @JsonProperty("eip")
    private EipPutUpdateParam   eipPutUpdateParam;
}
