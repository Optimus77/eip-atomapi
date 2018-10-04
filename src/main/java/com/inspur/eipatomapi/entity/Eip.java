package com.inspur.eipatomapi.entity;

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
    private String eipid;

    @Column(name="eip_name")
    private String name;

    @NotBlank
    @Column(name="elastic_ip")
    private String eip_address;

    @Column(name="floating_ip")
    private String floating_ip;

    @Column(name="fixed_ip")
    private String private_ip_address;

    @Column(name="ip_version")
    private String ipVersion= "IPv4";

    @Column(name="floating_ip_id")
    private String floating_ipId;

    @Column(name="portid")
    private String portid;

    @Column(name="instance_id")
    private String instanceId;

    @Column(name="instance_type")
    private String instanceType;

    @Column(name="vpc_id")
    private String vpcId;

    @Column(name="charge_type")
    private String chargetype = "PrePaid";

    @Column(name="charge_mode")
    private String chargemode = "BandWidth";

    @Column(name="purchase_time")
    private String purchasetime;

    @Column(name="band_width")
    private int banwidth;

    @Column(name="link_type")
    private String iptype;

    @Column(name="shared_bandwidth_id")
    private String sharedBandWidth_id;

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
    private String status ="DOWN";

    @Column(name="project_id")
    private String projectId;

    @Column(name="create_time" ,nullable = false)
    private Date createtime = new Date(System.currentTimeMillis());

    @Column(name="update_time")
    private Date updateTime;

}
