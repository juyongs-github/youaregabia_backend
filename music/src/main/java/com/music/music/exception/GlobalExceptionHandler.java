package com.music.music.exception;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * DTO Validation 오류 처리
   * (@RequestBody @Valid)
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationException(
      MethodArgumentNotValidException e) {

    List<Map<String, String>> errors = e.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(this::toFieldError)
        .toList();

    Map<String, Object> response = new HashMap<>();
    response.put("code", "VALIDATION_ERROR");
    response.put("message", "요청 값이 올바르지 않습니다.");
    response.put("errors", errors);

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(response);
  }

  /**
   * 비즈니스 로직 오류
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
      IllegalArgumentException e) {

    Map<String, Object> response = new HashMap<>();
    response.put("code", "BUSINESS_ERROR");
    response.put("message", e.getMessage());

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(response);
  }

  /**
   * FieldError → Map 변환
   */
  private Map<String, String> toFieldError(FieldError fieldError) {
    Map<String, String> error = new HashMap<>();
    error.put("field", fieldError.getField());
    error.put("message", fieldError.getDefaultMessage());
    return error;
  }
}
