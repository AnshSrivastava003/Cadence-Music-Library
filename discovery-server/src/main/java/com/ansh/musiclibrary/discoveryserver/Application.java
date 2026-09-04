package com.ansh.musiclibrary.discoveryserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableEurekaServer
@EnableScheduling
@SpringBootApplication(
    scanBasePackages = {"com.ansh.musiclibrary.discoveryserver", "com.ansh.musiclibrary.common"})
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
