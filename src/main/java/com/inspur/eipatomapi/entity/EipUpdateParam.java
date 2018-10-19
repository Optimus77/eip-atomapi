package com.inspur.eipatomapi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Range;
import org.springframework.lang.NonNull;

@Data
public class EipUpdateParam {

    //@Range(min=1,max=2000 ,message = "value must be 1~2000.")
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
