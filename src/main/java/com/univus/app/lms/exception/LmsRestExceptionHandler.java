package com.univus.app.lms.exception;

import com.univus.app.exception.StorageFileNotFoundException;
import lombok.extern.slf4j.Slf4j;
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
 * <p>{@code @RestControllerAdvice(basePackages = "com.univus.app.lms")} 로 <b>LMS 컨트롤러에만</b> 적용된다.
 * <ul>
 *   <li>스코프 격리: 팀원 도메인(member/community/subscription 등)에는 영향 없음(공유 exception 패키지 무수정).</li>
 *   <li>우선순위: 전역의 {@code @ExceptionHandler(Exception.class)} catch-all(500)보다 "구체 타입"이라
 *       LMS 컨트롤러에서 발생한 아래 예외들은 이 핸들러가 먼저 잡는다(@Order 불필요).</li>
 * </ul>
 * 이로써 @Valid 실패·깨진 JSON·이미지 검증·파일 부재가 500이 아니라 400/404/415로 응답된다.
 * (정상 흐름의 {@code ResponseStatusException} 403/404/400 등은 스프링이 그대로 처리하므로 여기서 다루지 않음.)
 *
 * <p>응답 형식은 레포의 커스텀 401과 동일한 {@code {success:false, message:...}} 스키마를 따른다.
 */
@Slf4j
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
