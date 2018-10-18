package com.inspur.eipatomapi.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.inspur.eipatomapi.util.TypeConstraint;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Range;
import org.springframework.lang.NonNull;


/**
 * @author: jiasirui
 * @date: 2018/10/18 15:33
 * @description:
 */
@Data
@Getter
@Setter
public class EipPutUpdateParam {


    @JsonProperty("port_id")
    private String portId;

    @JsonProperty("bandwidth")
    private String bandWidth;

    //@TypeConstraint(allowedValues = {"PrePaid","PostPaid"}, message = "Only PrePaid,PostPaid is allowed. ")
    @JsonProperty("chargetype")
    private String chargeType;


    @JsonProperty("serverid")
    private String serverId;

    //1：ecs // 2：cps // 3：slb
    @JsonProperty("type")
    private String type;
}
