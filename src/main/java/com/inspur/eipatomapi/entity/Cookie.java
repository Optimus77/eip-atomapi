package com.inspur.eipatomapi.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@Setter
@Getter
public class Cookie {
    String token;
    String platform;
    String hwPlatform;
    String hostName;
    String company;
    String oemid;
    String vsysid;
    String vsysname;
    String role;
    String license;
    String httpProtocol;
    String softVersion;
    String username;
    String overseaLicense;
    String hsframeLang = "zh_CN";


    public Cookie(String token, String platform, String hwPlatform, String hostName, String company, String oemid, String vsysid, String vsysname, String role, String license, String httpProtocol, String softVersion, String username, String overseaLicense, String hsframeLang) throws UnsupportedEncodingException {
        this.token = this.urlencode(token);
        this.platform = this.urlencode(platform);
        this.hwPlatform = this.urlencode(hwPlatform);
        this.hostName = this.urlencode(hostName);
        this.company = this.urlencode(company);
        this.oemid = this.urlencode(oemid);
        this.vsysid = this.urlencode(vsysid);
        this.vsysname = this.urlencode(vsysname);
        this.role = this.urlencode(role);
        this.license = this.urlencode(license);
        this.httpProtocol = this.urlencode(httpProtocol);
        this.softVersion = this.urlencode(softVersion);
        this.username = this.urlencode(username);
        this.overseaLicense = this.urlencode(overseaLicense);
        this.hsframeLang = this.urlencode(hsframeLang);
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
        sb.append("hw_platform="+this.hwPlatform+";");
        sb.append("host_name="+this.hostName+";");
        sb.append("company="+this.company+";");
        sb.append("oemid="+this.oemid+";");
        sb.append("vsysid="+this.vsysid+";");
        sb.append("vsysName="+this.vsysname+";");
        sb.append("role="+this.role+";");
        sb.append("license="+this.license+";");
        sb.append("httpProtocol="+this.httpProtocol+";");
        sb.append("soft_version="+this.softVersion+";");
        sb.append("username="+this.username+";");
        sb.append("overseaLicense="+this.overseaLicense+";");
        sb.append("HS.frame.lang="+this.hsframeLang);
        return sb.toString();
    }
}
