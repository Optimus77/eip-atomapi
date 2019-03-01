package com.inspur.eipatomapi.entity.eip;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class EipUpdateParamWrapper {
    @JsonProperty("eip")
    @Valid
    private EipUpdateParam   eipUpdateParam;
}
