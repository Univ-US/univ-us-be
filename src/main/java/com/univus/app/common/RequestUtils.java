package com.univus.app.common;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


public class RequestUtils {
	/**
	 * 현재 요청에 대한 HttpServletRequest 객체 반환
	 * @return
	 */
	public static HttpServletRequest getCurrentRequest() {
		// 현재 요청 컨텍스트
		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

		// 웹 요청 환경이 아닌 경우
		if (attributes != null) {
			return attributes.getRequest();
		}

		return null;
	}

	/**
	 * 현재 요청에 대한 HttpServletResponse 객체 반환
	 * @return
	 */
	public static HttpServletResponse getCurrentResponse() {
		// 현재 요청 컨텍스트
		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

		// 웹 요청 환경이 아닌 경우
		if (attributes != null) {
			return attributes.getResponse();
		}

		return null;
	}
	
	/**
	 * 현재 요청이 속한 웹 애플리케이션의 컨텍스트 경로 반환
	 * @return
	 */
	public static String getContextPath() {
		HttpServletRequest request = getCurrentRequest();

		return request != null ? request.getContextPath() : "";
	}

	/**
	 * HTTP 요청 헤더(Request Header) 중에서 지정한 이름의 헤더 값 반환
	 * @param headerName	반환할 헤더 이름
	 * @return
	 */
	public static String getHeaderValue(String headerName) {
		HttpServletRequest request = getCurrentRequest();
		return (request != null) ? request.getHeader(headerName) : null;
	}
	
	/**
	 * 요청 환경이 Mobile 기기를 사용하는 경우 true 반환
	 * @return
	 */
	public static boolean isMobileRequest() {
	    String userAgent = getHeaderValue("User-Agent");
	    
	    if(userAgent == null) {
	    	return false;
	    }
	    
	    final String[] MOBILE_KEYWORDS = {
	    		"mobi", "android", "iphone", "ipod", "ipad", "blackberry",
	            "windows ce", "samsung", "lg", "mot", "sonyEricsson", "opera mini", "nokia"
	    };
	    
	    userAgent = userAgent.toLowerCase();
	    for (String keyword : MOBILE_KEYWORDS) {
            if (userAgent.contains(keyword)) {
                return true;
            }
        }
	    
	    return false;
	}

	/**
	 * 요청한 클라이언트의 ip 주소 반환
	 * @return
	 */
	public static String getClientIp() {
		HttpServletRequest request = getCurrentRequest();

		if (request == null) {
			return "0.0.0.0";
		}

		return extractIp(request);
	}

	private static String extractIp(HttpServletRequest request) {
		// 체크할 헤더(프록시 서버 설정을 따름)
		String[] headerNames = { "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP",
				"HTTP_X_FORWARDED_FOR" };

		for (String header : headerNames) {
			String ip = request.getHeader(header);
			if (isValidIp(ip)) {
				// X-Forwarded-For는 "IP1, IP2, IP3" 형태로 올 수 있으므로 첫 번째 IP만 추출
				return ip.contains(",") ? ip.split(",")[0].trim() : ip;
			}
		}

		String remoteIp = request.getRemoteAddr();

		return "0:0:0:0:0:0:0:1".equals(remoteIp) ? "127.0.0.1" : remoteIp;
	}

	private static boolean isValidIp(String ip) {
		return ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip);
	}
}
