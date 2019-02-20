package com.inspur.eipatomapi.entity.eipv6;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class EipV6AllocateParam implements Serializable {


    private String eipId;


}
