package com.inspur.eipatomapi.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
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
    private String id;

    @Column(name="eip_name")
    private String name;

    @Column(name="elastic_ip")
    private String eip;

    @Column(name="floating_ip")
    private String floatingIp;

    @Column(name="fixed_ip")
    private String fixedIp;

    @Column(name="ip_version")
    private String ipVersion= "IPv4";

    @Column(name="floating_ip_id")
    private String floatingIpId;

    @Column(name="instance_id")
    private String instanceId;

    @Column(name="instance_type")
    private String instanceType;

    @Column(name="vpc_id")
    private String vpcId;

    @Column(name="charge_type")
    private String chargeType = "PrePaid";

    @Column(name="charge_mode")
    private String chargeMode = "BandWidth";

    @Column(name="purchase_time")
    private String purchaseTime;

    @Column(name="band_width")
    private int banWidth;

    @Column(name="link_type")
    private String linkType;

    @Column(name="shared_bandwidth_id")
    private String sharedBandWidthId;

    @Column(name="acl_id")
    private String aclId;

    @Column(name="qos_id")
    private String pipId;

    @Column(name="snat_id")
    private String snatId;

    @Column(name="dnat_id")
    private String dnatId;

    @Column(name="firewall_id")
    private String firewallId;

    @Column(name="state",nullable = false)
    private String state ="0";

    @Column(name="project_id")
    private String projectId;

    @Column(name="create_time" ,nullable = false)
    private Date createTime=new Date();

    @Column(name="update_time")
    private Date updateTime;

}
