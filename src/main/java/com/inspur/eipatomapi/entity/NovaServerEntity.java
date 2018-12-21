package com.inspur.eipatomapi.entity;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class NovaServerEntity {
    public static final long serialVersionUID = 1L;
    private String id;
    private String name;
}

