package com.github.catstiger.core.web;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * 拦截所有的http method 为“OPTIONS”的请求，直接render 200状态。
 * Cross-Origin Resource Sharing
 */
public class CorsOptionsRequestFilter implements Filter {

    /**
     * Default constructor. 
     */
    public CorsOptionsRequestFilter() {
        
    }

  /**
   * @see Filter#destroy()
   */
  public void destroy() {
    
  }

  /**
   * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
   */
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    if("OPTIONS".equalsIgnoreCase(((HttpServletRequest) request).getMethod())) {
      try {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{}");
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      chain.doFilter(request, response);
    }
    
  }

  /**
   * @see Filter#init(FilterConfig)
   */
  public void init(FilterConfig fConfig) throws ServletException {
    
  }

}
