package com.inspur.eipatomapi.entity.eipv6;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import java.io.Serializable;

@Getter
@Setter
public class EipV6AllocateParamWrapper implements Serializable {
    @JsonProperty("eipv6")
    @Valid
    private EipV6AllocateParam eipV6AllocateParam;
}
