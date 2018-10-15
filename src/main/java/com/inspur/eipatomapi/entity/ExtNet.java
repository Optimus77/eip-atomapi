package com.inspur.eipatomapi.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name="extnet")
@Getter
@Setter
public class ExtNet implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, insertable = false, updatable = false)
    private Integer id;

    private String name;
    @Column(nullable = false, updatable = false)
    private String netId;

    private String ipVersion = "ipV4";

    private String region;


}
