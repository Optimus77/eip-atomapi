package com.inspur.eipatomapi.entity.eip;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EipReturnSbw implements Serializable {

    @JsonProperty("eipId")
    private String eipId;

    @JsonProperty("eipAddress")
    private String eipAddress;

    @JsonProperty("bandWidth")
    private int bandWidth;

    @JsonProperty("billType")
    private String billType;

    @JsonProperty("chargeMode")
    private String chargeMode ;

}
