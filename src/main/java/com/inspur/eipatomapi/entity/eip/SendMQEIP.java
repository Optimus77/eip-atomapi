package com.inspur.eipatomapi.entity.eip;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;

@Entity
@Getter
@Setter
@Data
public class SendMQEIP {

    private String userName;

    private String handlerName;

    private String instanceId;

    private String instanceStatus;

    private String operateType;

    private String messageType;

    private String message;

}
