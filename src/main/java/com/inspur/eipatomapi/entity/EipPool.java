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
    @Column(nullable = false, insertable = false, updatable = false)
    private String id;

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer num;

    private String fireWallId;

    private String ip;

    private String state; //0:free 1:unbound 2:bound 9:reserve

}
