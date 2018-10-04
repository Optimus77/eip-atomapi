package com.inspur.eipatomapi.entity;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Builder
public class Resourceset implements Serializable {
    public String resourcetype;
    public String resource_id;
}
