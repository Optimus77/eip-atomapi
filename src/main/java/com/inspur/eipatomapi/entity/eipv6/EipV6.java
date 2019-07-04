package com.inspur.eipatomapi.entity.eipv6;


import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name="eipV6")
@Data
public class EipV6 {

    @Id
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    @GeneratedValue(generator = "system-uuid")
    @Column(name ="id",nullable = false, insertable = false, updatable = false)
    private String id;

    private String ipVersion="IPv6";

    private String ipv6;

    private String ipv4;

    private String floatingIp;

    private String userId;

    private String region;

    private String firewallId;

    private String snatptId;

    private String dnatptId;

    private String status = "ACTIVE";

    @Column(name="created_time" ,nullable = false)
    private Date createdTime  = new Date(System.currentTimeMillis());

    private Date updatedTime;

    private int isDelete=0;

}
