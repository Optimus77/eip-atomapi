package com.inspur.eipatomapi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import javax.validation.Valid;

@Getter
@Setter
public class EipUpdateParamWrapper {
    @JsonProperty("eip")
    @Valid
    private EipUpdateParam   eipUpdateParam;


}
