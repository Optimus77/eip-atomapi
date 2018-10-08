package com.inspur.eipatomapi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EipUpdateParam {

    @JsonProperty("bandwidth")
    private int bandWidth;

    @JsonProperty("chargetype")
    private String chargeType;

    @JsonProperty("serverid")
    private String serverId;

    @JsonProperty("portid")
    private String portId;

    //1：ecs // 2：cps // 3：slb
    @JsonProperty("type")
    private String type;
}
