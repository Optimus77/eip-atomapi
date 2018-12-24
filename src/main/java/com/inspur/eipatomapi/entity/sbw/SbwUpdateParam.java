package com.inspur.eipatomapi.entity.sbw;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.lang.NonNull;

@Data
public class SbwUpdateParam {
    @JsonProperty("bandwidth")
    private int bandWidth;

    private String billType;

    @NonNull
    @JsonProperty("serverid")
    private String serverId;
}
