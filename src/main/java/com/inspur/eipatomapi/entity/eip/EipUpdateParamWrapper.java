package com.inspur.eipatomapi.entity.eip;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@NotNull(message="the body must be a json ")
public class EipUpdateParamWrapper {
    @JsonProperty("eip")
    @Valid
    @NotNull(message="the body must be a json and eip is not null")
    private EipUpdateParam   eipUpdateParam;


}
