package com.inspur.eipatomapi.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServerReturn {
    @JSONField(name = "server_id", ordinal = 1)
    String serverId;
    @JSONField(name = "server_name", ordinal = 2)
    String serverName;
    @JSONField(name = "server_ip", ordinal = 3)
    String serverIp;
}