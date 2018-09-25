package com.inspur.eipatomapi.entity;

import lombok.Data;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@Data
public class Cookie {
    String token;
    String platform;
    String hw_platform;
    String host_name;
    String company;
    String oemid;
    String vsysid;
    String vsysname;
    String role;
    String license;
    String httpProtocol;
    String soft_version;
    String username;
    String overseaLicense;
    String HS_frame_lang = "zh_CN";


    public Cookie(String token, String platform, String hw_platform, String host_name, String company, String oemid, String vsysid, String vsysname, String role, String license, String httpProtocol, String soft_version, String username, String overseaLicense, String HS_frame_lang) throws UnsupportedEncodingException {
        this.token = this.urlencode(token);
        this.platform = this.urlencode(platform);
        this.hw_platform = this.urlencode(hw_platform);
        this.host_name = this.urlencode(host_name);
        this.company = this.urlencode(company);
        this.oemid = this.urlencode(oemid);
        this.vsysid = this.urlencode(vsysid);
        this.vsysname = this.urlencode(vsysname);
        this.role = this.urlencode(role);
        this.license = this.urlencode(license);
        this.httpProtocol = this.urlencode(httpProtocol);
        this.soft_version = this.urlencode(soft_version);
        this.username = this.urlencode(username);
        this.overseaLicense = this.urlencode(overseaLicense);
        this.HS_frame_lang = this.urlencode(HS_frame_lang);
    }

    String urlencode(String item) throws UnsupportedEncodingException {
        return URLEncoder.encode(item, "utf-8").replaceAll("%3B", ";").replaceAll("%2F", "/").replaceAll("%3F", "?").replaceAll("%3A", ":").replaceAll("%40", "@").replaceAll("%3D", "=").replaceAll("%2B", "+").replaceAll("%24", "$").replaceAll("%2C", ",").replaceAll("%21", "!").replaceAll("%7E", "~").replaceAll("%28", "(").replaceAll("%29", ")").replaceAll("%27", "'").replaceAll("%26", "&");
    }

}
