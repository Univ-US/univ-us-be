package com.univus.app.security;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AjaxSessionTimeoutFilter implements Filter{
	private String ajaxHeader;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;
		
		if(isAjaxRequest(req)) {
			// AJAX 요청인 경우
			try {
				chain.doFilter(req, resp);
			} catch (AccessDeniedException e) {
				// 권한이 없거나 로그인이 되지 않은 경우 AccessDeniedException 예외 발생
				resp.sendError(HttpServletResponse.SC_FORBIDDEN); //403
			} catch (AuthenticationException e) {
				resp.sendError(HttpServletResponse.SC_UNAUTHORIZED); //401
			}
		}else {
			chain.doFilter(req, resp);
		}
		
		
	}
	
	public void setAjaxHeader(String ajaxHeader) {
		this.ajaxHeader = ajaxHeader;
	}
	
	private boolean isAjaxRequest(HttpServletRequest req) {
		return req.getHeader(ajaxHeader) != null && req.getHeader(ajaxHeader).equals(Boolean.TRUE.toString());
	}
}
