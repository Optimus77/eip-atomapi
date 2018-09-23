package com.inspur.eipatomapi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class EipAllocateParamWrapper implements Serializable {
    @JsonProperty("eip")
    private EipAllocateParam   eipAllocateParam;
}
