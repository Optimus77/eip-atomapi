package com.inspur.eipatomapi.entity.eipv6;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EipV6UpdateParam {

    @JsonProperty("eipaddress")
    private String eipAddress;


}
