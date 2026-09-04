package com.ansh.musiclibrary.discoveryserver;

import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfig {
  @Bean
  WebServerFactoryCustomizer<TomcatServletWebServerFactory> connector() {
    return factory -> factory.setProtocol("org.apache.coyote.http11.Http11Nio2Protocol");
  }
}
