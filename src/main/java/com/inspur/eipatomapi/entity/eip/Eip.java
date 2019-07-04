package com.inspur.eipatomapi.entity.eip;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name="eip")
@Data
public class Eip implements Serializable {

    @Id
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    @GeneratedValue(generator = "system-uuid")
    @Column(name ="id",nullable = false, insertable = false, updatable = false)
    private String id;

    private String name;

    private String ipVersion= "IPv4";

    @NotBlank
    private String eipAddress;

    private String privateIpAddress;

    private String floatingIp;

    private String floatingIpId;

    private String portId;

    private String instanceId;

    private String instanceType;

    private String billType = "hourlySettlement";

    private String chargeMode = "BandWidth";

    private String duration;

    private int bandWidth;

    private int oldBandWidth =1;

    private String ipType;

    private String sbwId;

    private String pipId;

    private String snatId;

    private String dnatId;

    private String firewallId;

    private String status ="DOWN";

    private String projectId;

    private String userId;

    private String region;

    @Column(name="created_time" ,nullable = false)
    private Date createdTime  = new Date(System.currentTimeMillis());

    private Date updatedTime;

    private int isDelete=0;

    private String eipV6Id;

}
