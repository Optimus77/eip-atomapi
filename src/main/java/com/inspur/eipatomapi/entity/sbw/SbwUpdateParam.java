package com.inspur.eipatomapi.entity.sbw;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.lang.NonNull;

import java.io.Serializable;
import java.util.List;

@Data
public class SbwUpdateParam implements Serializable {
    //@Range(min=1,max=2000)
    @JsonProperty("bandwidth")
    private int bandWidth;

    //1：ecs // 2：cps // 3：slb
    @JsonProperty("type")
    private String type;

    @JsonProperty
    private List<String> eipAddress;

    @JsonProperty("sbwname")
    private String sbwName;

    @JsonProperty
    private String billType;

    @JsonProperty
    private String chargeMode;
}
