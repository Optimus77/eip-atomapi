package com.inspur.eipatomapi.entity.eip;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.USE_DEFAULTS)
public class Resourceset implements Serializable {
    public String resourcetype;
    public String resourceid;
}
