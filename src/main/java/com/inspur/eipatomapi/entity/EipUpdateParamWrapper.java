package com.inspur.eipatomapi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EipUpdateParamWrapper {
    @JsonProperty("eipatomapi")
    private EipUpdateParam   eipUpdateParam;


}
