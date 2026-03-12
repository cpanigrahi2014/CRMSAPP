package com.crm.common.tenant;

import com.crm.common.security.TenantContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@Order(1)
public class TenantInterceptorFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String tenantHeader = httpRequest.getHeader("X-Tenant-ID");

        if (tenantHeader != null && !tenantHeader.isBlank()) {
            TenantContext.setTenantId(tenantHeader);
            log.debug("Tenant ID set from header: {}", tenantHeader);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
