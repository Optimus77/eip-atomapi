package com.inspur.eipatomapi.entity.eip;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.persistence.Column;
import java.io.Serializable;


@Data
public class EipReturnByBandWidth implements Serializable {
    @JsonProperty("eipid")
    private String eipId;

    @Column(name="eip_address")
    @JsonProperty("eip_address")
    private String eipAddress;
}
