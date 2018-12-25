package com.inspur.eipatomapi.entity.sbw;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.lang.NonNull;

import java.io.Serializable;

@Data
public class SbwUpdateParam implements Serializable {
    @JsonProperty("bandwidth")
    private int bandWidth;

    private String billType;

    @NonNull
    @JsonProperty("serverid")
    private String serverId;
}
