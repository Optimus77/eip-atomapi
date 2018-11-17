package com.inspur.eipatomapi.config.servlet;

import com.alibaba.druid.support.http.StatViewServlet;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

/**
 * @author: jiasirui
 * @date: 2018/9/25 15:35
 * @description:
 */
@WebServlet(urlPatterns = "/druid/*", initParams={
        @WebInitParam(name="allow",value=""),
        @WebInitParam(name="deny",value="192.168.16.111"),
        @WebInitParam(name="loginUsername",value="admin"),
        @WebInitParam(name="loginPassword",value="admin"),
        @WebInitParam(name="resetEnable",value="true")
})
public class DruidStatViewServlet extends StatViewServlet {

}
