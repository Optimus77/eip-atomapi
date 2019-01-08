package com.inspur.eipatomapi.entity.fw;

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
    private String id;

	private String ip;

	private String port;

	private String user;

	private String passwd;

	private String devtype;

	private String param1;

	private String param2;

	private String param3;

	private String region;
}
