package edu.ohsu.cmp.ecareplan.config;

import edu.ohsu.cmp.ecareplan.interceptor.CacheControlInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new CacheControlInterceptor())
                .addPathPatterns(
                        "/patient/**",
                        "/logout",
                        "/inactivity-logout",
                        "/unauthorized",
                        "/error"
                );
    }
}