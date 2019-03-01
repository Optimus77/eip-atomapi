package com.inspur.eipatomapi.entity.sbw;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name="sbw")
@Data
public class Sbw implements Serializable {
    @Id
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    @GeneratedValue(generator = "system-uuid")
    @Column(name ="sbw_id",nullable = false, insertable = false, updatable = false)
    private String sbwId;

    private String sharedbandwidthName;

    private String instanceId;

    private String instanceType;
    //计费方式
    private String billType = "monthly";
    //付费模式
    private String chargeMode = "BandWidth";

    private String duration;

    private String durationUnit = "M";

    private Integer bandWidth;

    private String region;

    @Column(name="create_time" ,nullable = false)
    private Date createTime  = new Date(System.currentTimeMillis());

    @Column(name="update_time" ,nullable = false)
    private Date updateTime  = new Date(System.currentTimeMillis());

    private String projectId;

    private int isDelete =0;

    private String pipeId;
}
