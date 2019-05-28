package com.inspur.eipatomapi.entity.sbw;

import com.inspur.eipatomapi.util.HsConstants;
import lombok.Builder;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name="sbw")
@Data
@Builder
public class Sbw implements Serializable {
    @Id
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    @GeneratedValue(generator = "system-uuid")
    @Column(name ="sbw_id",nullable = false, insertable = false, updatable = false)
    private String sbwId;

    private String sbwName;

    //计费方式
    private String billType ;

    private String duration;

    private Integer bandWidth;

    private String region;

    @Column(name="create_time" ,nullable = false)
    private Date createTime  ;

    @Column(name="update_time" ,nullable = false)
    @Builder.Default
    private Date updateTime  = new Date(System.currentTimeMillis());;
    //project id : uuid
    private String projectId;

    private int isDelete;

    private String pipeId;

    @Builder.Default
    private String status = HsConstants.ACTIVE;
    //username :login name
    private String projectName;
}
