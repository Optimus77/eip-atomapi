package com.inspur.eipatomapi.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name="firewall")
@Getter
@Setter
public class Firewall implements Serializable {
	@Id
	@GenericGenerator(name = "system-uuid", strategy = "uuid2")
	@GeneratedValue(generator = "system-uuid")
	@Column(name ="firewall_id",nullable = false, insertable = false, updatable = false)
    String id;

    @Column(name="ip")
	String ip;

    @Column(name="port")
	private String port;

    @Column(name="user")
	private String user;

    @Column(name="password")
	private String passwd;

    @Column(name="dev_type")
	private String devtype;

    @Column(name="param1")
	private String param1;

    @Column(name="param2")
	private String param2;

    @Column(name="param3")
	private String param3;
}
