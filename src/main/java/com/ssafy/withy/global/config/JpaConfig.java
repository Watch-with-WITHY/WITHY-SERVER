package com.ssafy.withy.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing // 여기로 이사 옴
public class JpaConfig {
}