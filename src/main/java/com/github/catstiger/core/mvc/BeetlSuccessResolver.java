package com.github.catstiger.core.mvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.beetl.core.GroupTemplate;
import org.beetl.core.resource.WebAppResourceLoader;
import org.beetl.ext.servlet.ServletGroupTemplate;

import com.github.catstiger.mvc.config.ApiResource;
import com.github.catstiger.mvc.config.Initializer;
import com.github.catstiger.mvc.resolver.JspSuccessResolver;

public class BeetlSuccessResolver extends JspSuccessResolver {
  public static final String BEETL_TEMPLATE_SUFFIX = ".html";
  public static final String CTX_PAPT_KEY = "ctx";
  
  public BeetlSuccessResolver() {
    GroupTemplate groupTemplate = ServletGroupTemplate.instance().getGroupTemplate();
    WebAppResourceLoader webResLoader = (WebAppResourceLoader) groupTemplate.getResourceLoader();
    Initializer initializer = Initializer.getInstance();
    webResLoader.setRoot(initializer.getRealPath() + initializer.getPageFolder());
  }

  @Override
  public void resolve(HttpServletRequest request, HttpServletResponse response, ApiResource apiResource, Object value) {
    String serviceUri = apiResource.getUri();
    String template = new StringBuilder(60).append(serviceUri).append(BEETL_TEMPLATE_SUFFIX).toString();
    //将数据装入Reqeust
    doRequestInternal(value, request);
    //Context Path
    if(request.getAttribute(CTX_PAPT_KEY) == null) {
      request.setAttribute("ctx", request.getContextPath());
    }
    
    ServletGroupTemplate.instance().render(template, request, response);
  }

}
