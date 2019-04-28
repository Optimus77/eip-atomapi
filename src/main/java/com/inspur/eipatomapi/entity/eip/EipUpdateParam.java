package com.inspur.eipatomapi.entity.eip;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.inspur.eipatomapi.util.TypeConstraint;
import lombok.Data;
import org.hibernate.validator.constraints.Range;
import org.springframework.lang.NonNull;

@Data
public class EipUpdateParam {
    //@TypeConstraint(allowedValues = {"monthly","hourlySettlement"}, message = "Only monthly,hourlySettlement is allowed. ")
    private String billType;

    //@Range(min=1,max=200)
    @JsonProperty("bandwidth")
    private int bandWidth;

    @JsonProperty("serverid")
    private String serverId;

    @JsonProperty("portid")
    private String portId;

    //1：ecs // 2：cps // 3：slb

    @JsonProperty("type")
    private String type;

    @JsonProperty("privateip")
    private String privateIp;

    private String chargemode;

    private String sbwId;
}
