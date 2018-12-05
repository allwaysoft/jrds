package jrds.webapp;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class SecurityFilter implements Filter {

    static private final Logger logger = Logger.getLogger(SecurityFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        if(request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest req = (HttpServletRequest) request;
            HttpServletResponse rep = (HttpServletResponse) response;
            boolean ok = req.authenticate(rep);
            if(logger.isDebugEnabled()) {
                if(ok) {
                    logger.debug("authenticated as: " + req.getUserPrincipal().getName());
                } else {
                    logger.debug("authenticated failed");
                }
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

}
