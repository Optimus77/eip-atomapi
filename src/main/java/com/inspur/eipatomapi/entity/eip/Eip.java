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
    @Column(name ="eip_id",nullable = false, insertable = false, updatable = false)
    private String eipId;

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

    private String vpcId;

    private String billType = "hourlySettlement";

    private String chargeMode = "BandWidth";

    private String duration;

    private int bandWidth;

    private int oldBandWidth =1;

    private String ipType;

    private String sharedBandWidthId;

    private String aclId;

    private String pipId;

    private String snatId;

    private String dnatId;

    private String firewallId;

    private String status ="DOWN";

    private String projectId;

    private String region;

    @Column(name="create_time" ,nullable = false)
    private Date createTime  = new Date(System.currentTimeMillis());

    private Date updateTime;

    private int isDelete=0;

}
