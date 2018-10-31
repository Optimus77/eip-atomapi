package com.inspur.eipatomapi.entity.eip;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import java.io.Serializable;

@Getter
@Setter
public class EipAllocateParamWrapper implements Serializable {
    @JsonProperty("eip")
    @Valid
    private EipAllocateParam   eipAllocateParam;
}
