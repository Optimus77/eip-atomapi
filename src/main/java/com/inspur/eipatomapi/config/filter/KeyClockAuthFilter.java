package com.inspur.eipatomapi.config.filter;


import com.alibaba.fastjson.JSONObject;
import com.inspur.eipatomapi.config.CodeInfo;
import com.inspur.eipatomapi.config.ConstantClassField;
import com.inspur.eipatomapi.util.HsConstants;
import com.inspur.eipatomapi.util.ReturnMsgUtil;
import com.inspur.eipatomapi.util.ReturnStatus;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author: jiasirui
 * @date: 2018/9/23 22:26
 * @description:
 */
@WebFilter
public class KeyClockAuthFilter implements Filter {


    private final static Logger log = LoggerFactory.getLogger(KeyClockAuthFilter.class);


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("******************KeyClockAuthFilter init");

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)throws IOException, ServletException {
        log.info("******************KeyClockAuthFilter doFilter");
        HttpServletRequest req= (HttpServletRequest)servletRequest;
        HttpServletResponse response=(HttpServletResponse)servletResponse;
        if(req.getRequestURI().startsWith(req.getContextPath()+ConstantClassField.VERSION_REST)){
            if(req.getHeader("Authorization")==null){
                log.info("get authorization is null ");
                JSONObject result=new JSONObject();
                result.put("code",ReturnStatus.SC_FORBIDDEN);
                result.put("message", CodeInfo.getCodeMessage(CodeInfo.KEYCLOAK_NULL));
                result.put("data",null);
                response.setStatus(HttpStatus.SC_BAD_REQUEST);
                response.setContentType(HsConstants.APPLICATION_JSON);
                response.getWriter().write(result.toJSONString());
            }else{
                log.info("get authorization {}",req.getHeader("Authorization"));

                filterChain.doFilter(servletRequest, servletResponse);
            }
        }else{
            filterChain.doFilter(servletRequest, servletResponse);
        }

    }

    @Override
    public void destroy() {
        log.info("******************KeyClockAuthFilter destroy");

    }
}
