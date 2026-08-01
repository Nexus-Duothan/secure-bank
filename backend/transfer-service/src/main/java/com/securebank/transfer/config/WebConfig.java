package com.securebank.transfer.config;

import com.securebank.transfer.security.CallerIdentityArgumentResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

  private final CallerIdentityArgumentResolver callerIdentityArgumentResolver;
  private final TransferServiceProperties properties;

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(callerIdentityArgumentResolver);
  }

  /**
   * Direct browser access is a local-development convenience; in a deployed environment the web app
   * is same-origin behind the API Gateway and this mapping matches nothing.
   */
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
      .addMapping("/api/**")
      .allowedOrigins(
        properties
          .cors()
          .allowedOrigins()
          .toArray(String[]::new)
      )
      .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
      .allowedHeaders("*");
  }
}
