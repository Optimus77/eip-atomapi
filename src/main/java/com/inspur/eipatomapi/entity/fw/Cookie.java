package com.inspur.eipatomapi.entity.fw;

import lombok.Getter;
import lombok.Setter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@Setter
@Getter
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
    /***
     * this.token = token;
     this.platform = platform;
     * this.hw_platform = hw_platform;
     this.company = company;
     this.oemid = oemid;
     this.vsysid = vsysid;
     this.vsysname = vsysname;
     this.role = role;
     this.license = license;
     this.httpProtocol = httpProtocol;
     this.username = username;
     this.overseaLicense = overseaLicense;
     this.HS_frame_lang = HS_frame_lang;
     */
    public String toString(){
        StringBuffer sb = new StringBuffer();
        sb.append("token="+this.token+";");
        sb.append("platform="+this.platform+";");
        sb.append("hw_platform="+this.hw_platform+";");
        sb.append("host_name="+this.host_name+";");
        sb.append("company="+this.company+";");
        sb.append("oemid="+this.oemid+";");
        sb.append("vsysid="+this.vsysid+";");
        sb.append("vsysName="+this.vsysname+";");
        sb.append("role="+this.role+";");
        sb.append("license="+this.license+";");
        sb.append("httpProtocol="+this.httpProtocol+";");
        sb.append("soft_version="+this.soft_version+";");
        sb.append("username="+this.username+";");
        sb.append("overseaLicense="+this.overseaLicense+";");
        sb.append("HS.frame.lang="+this.HS_frame_lang);
        return sb.toString();
    }
}
