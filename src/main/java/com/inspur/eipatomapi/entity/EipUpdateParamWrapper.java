package com.inspur.eipatomapi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@Nullable
public class EipUpdateParamWrapper {
    @JsonProperty("eip")
    @Nullable
    private EipUpdateParam   eipUpdateParam;


}
