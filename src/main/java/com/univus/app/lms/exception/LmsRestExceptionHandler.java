package com.univus.app.lms.exception;

import com.univus.app.exception.StorageFileNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LMS 전용 예외 핸들러.
 *
 * <p>{@code @RestControllerAdvice(basePackages = "com.univus.app.lms")} 로 LMS 컨트롤러에만 적용된다.
 * @Valid 실패·깨진 JSON·이미지 검증·파일 부재를 400/404/415로, DB 접근 오류를 일반 메시지 500으로 응답한다.
 * (정상 흐름의 {@code ResponseStatusException} 403/404/400 등은 스프링이 그대로 처리.)
 *
 * <p>{@code @Order(HIGHEST_PRECEDENCE)} = 전역 catch-all({@code Exception.class})보다 먼저 적용.
 * 응답 형식은 {@code {success:false, message:...}} 스키마.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE) // 전역 catch-all보다 먼저 적용(LMS 스코프 한정)
@RestControllerAdvice(basePackages = "com.univus.app.lms")
public class LmsRestExceptionHandler {

    /* @Valid @RequestBody 실패 → 400 + 필드별 메시지 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        return handleBinding(ex);
    }

    /* @Valid @ModelAttribute(멀티파트 프로필 수정 등) 실패 → 400 + 필드별 메시지 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> handleBinding(BindException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        log.warn("LMS 입력 검증 실패: {}", fieldErrors);
        return body(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다.", fieldErrors);
    }

    /* 본문 JSON 파싱 실패(깨진 JSON·타입 불일치) → 400 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("LMS 요청 본문 파싱 실패: {}", ex.getMostSpecificCause().getMessage());
        return body(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다.", null);
    }

    /* 프로필 이미지 형식/용량/시그니처 위반 → 415 */
    @ExceptionHandler(InvalidProfileImageException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidImage(InvalidProfileImageException ex) {
        log.warn("LMS 이미지 검증 실패: {}", ex.getMessage());
        return body(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage(), null);
    }

    /* 다운로드 대상 파일 없음 → 404 (StorageException 등 진짜 I/O 오류는 전역 500 유지) */
    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleFileNotFound(StorageFileNotFoundException ex) {
        log.warn("LMS 파일 없음: {}", ex.getMessage());
        return body(HttpStatus.NOT_FOUND, "요청한 파일을 찾을 수 없습니다.", null);
    }

    /* DB 접근 오류(MyBatis ORA/SQL 문법오류 등) → 500 일반 메시지.
       원문(ORA 코드·SQL 단편)은 서버 로그에만 남기고 응답엔 일반 메시지만 싣는다. */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccess(DataAccessException ex) {
        log.error("LMS DB 처리 오류", ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", null);
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message, Object errors) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("success", false);
        b.put("message", message);
        if (errors != null) {
            b.put("errors", errors);
        }
        return ResponseEntity.status(status).body(b);
    }
}
