package com.inspur.eipatomapi.entity.eip;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name="eip")
@Getter
@Setter
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

    private String chargeType = "PrePaid";
    private String billType = "hourlySettlement";

    private String chargeMode = "BandWidth";

    private String purchaseTime;
    private String duration;

    private int bandWidth;

    private String ipType;

    private String sharedBandWidthId;

    private String aclId;

    private String pipId;

    private String snatId;

    private String dnatId;

    private String firewallId;

    private String status ="DOWN";

    private String projectId;

    @Column(name="create_time" ,nullable = false)
    private Date createTime = new Date(System.currentTimeMillis());

    private Date updateTime;

}
