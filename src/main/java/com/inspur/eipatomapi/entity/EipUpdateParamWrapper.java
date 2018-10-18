package com.inspur.eipatomapi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;

@Getter
@Setter
@Nullable
public class EipUpdateParamWrapper {
    @JsonProperty("eip")
    @Nullable
    private EipUpdateParam   eipUpdateParam;


}
