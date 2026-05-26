package com.univus.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SpringMvcConfiguration implements WebMvcConfigurer {
	@Value("${file.upload-root}")
	private String uploadRoot;
	
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		// 브라우저에서 /uploads/로 시작하는 요청이 오면 로컬의 uploadRoot 경로에서 파일을 찾도록 설정 
		registry.addResourceHandler("/uploads/**")
			.addResourceLocations("file:///" + uploadRoot);
	}
	

}
