package com.inspur.eipatomapi.entity.eipv6;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name="eipPoolV6")
@Getter
@Setter
public class EipPoolV6 implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, insertable = false, updatable = false)
    private Integer id;

    private String fireWallId;
    @Column(nullable = false, updatable = false)
    private String ip;

    private String state; //0:free 1:unbound 2:bound 9:reserve


}
