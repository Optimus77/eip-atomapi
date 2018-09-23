package com.inspur.eipatomapi.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name="eipPool")
@Getter
@Setter
public class EipPool implements Serializable {

    @Id
    @GenericGenerator(name = "system-uuid", strategy = "uuid2")
    @GeneratedValue(generator = "system-uuid")
    @Column(name ="eip_id",nullable = false, insertable = false, updatable = false)
    private String id;

    @Column(name="firewall_id")
    private String fireWallId;

    @Column(name="ip")
    private String ip;

    @Column(name="state")
    private String state; //0:free 1:unbound 2:bound 9:reserve

}
