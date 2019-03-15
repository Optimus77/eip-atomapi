package com.inspur.eipatomapi.entity.eipv6;

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
public class EipV6ReturnBase implements Serializable {
    @JsonProperty("eip_v6_id")
    private String eipV6Id;

    @Column(name="ipv6")
    @JsonProperty("ipv6")
    private String ipv6;

    @JsonProperty("status")
    private String status;

    @JsonProperty("create_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @Column(name="ipv4")
    @JsonProperty("ipv4")
    private String ipv4;
}
