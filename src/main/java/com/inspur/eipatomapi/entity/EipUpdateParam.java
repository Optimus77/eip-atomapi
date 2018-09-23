package com.inspur.eipatomapi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EipUpdateParam {

    @JsonProperty("band_width")
    private String bandWidth;

    @JsonProperty("charge_type")
    private String chargeType;

    @JsonProperty("port_id")
    private String portId;
}
