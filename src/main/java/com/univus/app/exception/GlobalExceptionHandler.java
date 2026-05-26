package com.univus.app.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /** /api/** 요청인지 판단 */
    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");
        return uri.startsWith("/api/")
                || (accept != null && accept.contains("application/json"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Object handleMissingParams(MissingServletRequestParameterException ex,
                                      HttpServletRequest request) {
        log.info("BAD_REQUEST - ", ex);
        if (isApiRequest(request))
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "필수 파라미터가 누락되었습니다."));
        ModelAndView mav = new ModelAndView("error/error2");
        mav.addObject("title", "잘못된 요청입니다.");
        mav.addObject("message", "죄송합니다.<br><strong>400 - 요청을 처리할 수 없습니다.</strong>");
        mav.setStatus(HttpStatus.BAD_REQUEST);
        return mav;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Object handleArgumentTypeMismatch(MethodArgumentTypeMismatchException ex,
                                             HttpServletRequest request) {
        log.info("BAD_REQUEST - ", ex);
        if (isApiRequest(request))
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "파라미터 타입이 올바르지 않습니다."));
        ModelAndView mav = new ModelAndView("error/error2");
        mav.addObject("title", "잘못된 요청입니다.");
        mav.addObject("message", "죄송합니다.<br><strong>400 - 요청을 처리할 수 없습니다.</strong>");
        mav.setStatus(HttpStatus.BAD_REQUEST);
        return mav;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleResourceNotFound(NoResourceFoundException ex,
                                         HttpServletRequest request) {
        log.info("NOT_FOUND - ", ex);
        if (isApiRequest(request))
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "리소스를 찾을 수 없습니다."));
        ModelAndView mav = new ModelAndView("error/error2");
        mav.addObject("title", "페이지를 찾을 수 없습니다.");
        mav.addObject("message", "죄송합니다.<br><strong>404 - 요청하신 페이지가 존재하지 않습니다.</strong>");
        mav.setStatus(HttpStatus.NOT_FOUND);
        return mav;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Object handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex,
                                                       HttpServletRequest request) {
        if (isApiRequest(request))
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(Map.of("success", false, "message", "파일 용량이 초과되었습니다."));
        return new ModelAndView("error/uploadFailure");
    }

    @ExceptionHandler(Exception.class)
    public Object handleServerError(Exception ex, HttpServletRequest request) {
        log.info("INTERNAL_SERVER_ERROR 등 - ", ex);
        if (isApiRequest(request))
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "서버 오류가 발생했습니다: " + ex.getMessage()));
        ModelAndView mav = new ModelAndView("error/error2");
        mav.addObject("title", "시스템 오류.");
        mav.addObject("message", "죄송합니다.<br><strong>요청을 처리할 수 없습니다.</strong>");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        return mav;
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.info("FORBIDDEN - ", ex);
        if (isApiRequest(request))
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", "접근 권한이 없습니다."));
        ModelAndView mav = new ModelAndView("error/error2");
        mav.addObject("title", "접근 권한이 없습니다.");
        mav.addObject("message", "죄송합니다.<br><strong>403 - 관리자만 접근할 수 있는 페이지입니다.</strong>");
        mav.setStatus(HttpStatus.FORBIDDEN);
        return mav;
    }
}
