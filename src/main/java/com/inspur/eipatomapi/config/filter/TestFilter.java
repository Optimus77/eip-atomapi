package com.inspur.eipatomapi.config.filter;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import java.io.IOException;

/**
 * @author: jiasirui
 * @date: 2018/9/23 22:26
 * @description:
 */
@WebFilter
public class TestFilter implements Filter {


    private final static Log log = LogFactory.getLog(TestFilter.class);


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("******************TestFilter init");

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)throws IOException, ServletException {
        //log.info("******************TestFilter doFilter");
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
        log.info("******************TestFilter destroy");

    }
}
