package com.inspur.eipatomapi.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class LogLevel implements Serializable {
    private String level;
}
