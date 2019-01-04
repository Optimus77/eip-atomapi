package com.inspur.eipatomapi.entity.eip;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class EipShardBand implements Serializable {

    @NotNull
    @JsonProperty("shardBandWidthId")
    private String shardBandId;
}
