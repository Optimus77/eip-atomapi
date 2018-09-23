package com.inspur.eipatomapi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class EipAllocateParamWrapper implements Serializable {
    @JsonProperty("eipatomapi")
    private EipAllocateParam   eipAllocateParam;
}
