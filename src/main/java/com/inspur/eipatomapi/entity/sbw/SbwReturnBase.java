package com.inspur.eipatomapi.entity.sbw;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SbwReturnBase implements Serializable {
    @JsonProperty("sbwid")
    private String sbwId;

    @JsonProperty("bandwidth")
    private int bandWidth;

    @Column(name="sharedbandwidthname")
    @JsonProperty("sharedbandwidthname")
    private String sharedbandwidthName;

    @JsonProperty("create_at")
    @JsonFormat(shape= JsonFormat.Shape.STRING, timezone = "UTC", pattern="yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
