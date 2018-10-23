package com.inspur.eipatomapi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.inspur.eipatomapi.util.TypeConstraint;
import lombok.Data;
import org.hibernate.validator.constraints.Range;
import org.springframework.lang.NonNull;

@Data
public class EipUpdateParam {

    @Range(min=1,max=2000)
    @JsonProperty("bandwidth")
    private int bandWidth;

    @TypeConstraint(allowedValues = {"PrePaid","PostPaid"}, message = "Only PrePaid,PostPaid is allowed. ")
    @JsonProperty("chargetype")
    private String chargeType;

    @JsonProperty("serverid")
    private String serverId;

    @JsonProperty("portid")
    private String portId;

    //1：ecs // 2：cps // 3：slb
    @TypeConstraint(allowedValues = {"1","2", "3"}, message = "Only 1,2, 3, is allowed, 1:ecs,2:cps,3:slb. ")
    private String type;

    public EipUpdateParam() {

    }
}
