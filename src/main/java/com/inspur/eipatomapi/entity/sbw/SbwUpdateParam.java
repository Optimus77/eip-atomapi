package com.inspur.eipatomapi.entity.sbw;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
public class SbwUpdateParam implements Serializable {
    //@Range(min=1,max=2000)
    @JsonProperty("bandwidth")
    private int bandWidth;

    @JsonProperty
    private List<String> eipAddress;

    @JsonProperty("sbwName")
    private String sbwName;

    @JsonProperty
    private String billType;

    @JsonProperty("chargemode")
    private String chargeMode;

    @JsonProperty("consoleCustomization")
    private ConsoleCustomization consoleCustomization;

    @JsonProperty("method")
    @NotNull(message="the interface method must be a json and  not null")
    private String method;
}
