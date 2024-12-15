package com.doosan.notification.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private static class ErrorResponse {
        private final boolean success = false; // 항상 false로 설정하여 요청 실패를 나타냄
        private final int statusCode;         // HTTP 상태 코드
        private final String msg;             // 클라이언트에게 반환할 메시지
        private final LocalDateTime timestamp; // 에러 발생 시간

        public ErrorResponse(int statusCode, String msg) {
            this.statusCode = statusCode;
            this.msg = msg;
            this.timestamp = LocalDateTime.now(); // 현재 시간을 타임스탬프로 설정
        }

        // Getter 메서드
        public boolean isSuccess() {
            return success;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getMsg() {
            return msg;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }

    // ResourceNotFoundException: 리소스를 찾을 수 없는 경우 처리
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.error("NOT_FOUND: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND.value(), "재입고 알림 기록이 존재하지 않습니다.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // IllegalStateException: 잘못된 상태로 인해 요청을 처리할 수 없는 경우 처리
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        log.error("BAD_REQUEST: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "알림을 받을 활성화된 사용자가 없습니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // MethodArgumentNotValidException: 입력값 검증 실패 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        log.error("VALIDATION_ERROR: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "입력값이 올바르지 않습니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // MissingServletRequestParameterException: 필수 요청 파라미터가 누락된 경우 처리
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameterException(MissingServletRequestParameterException ex) {
        log.error("MISSING_PARAMETER: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "필수 파라미터가 누락되었습니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // UNIQUE 키 중복, 외래 키 제약 조건 위반 등 데이터베이스 무결성 제약 조건 위반 처리
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleSQLConstraintViolation(SQLIntegrityConstraintViolationException ex) {
        log.error("DATABASE_ERROR: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.CONFLICT.value(), "이미 존재하는 데이터이거나 유효하지 않은 데이터로 인해 요청이 실패했습니다.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    // NoSuchElementException: 요청한 요소를 찾을 수 없는 경우 처리
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElementException(NoSuchElementException ex) {
        log.error("NO_SUCH_ELEMENT: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND.value(), "요청한 요소를 찾을 수 없습니다.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    // UnsupportedOperationException: 지원되지 않는 작업 요청 시 처리
    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedOperationException(UnsupportedOperationException ex) {
        log.error("UNSUPPORTED_OPERATION: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_IMPLEMENTED.value(), "지원되지 않는 작업입니다.");
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(errorResponse);
    }

    // Exception: 모든 예외의 기본 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        log.error("INTERNAL_SERVER_ERROR : {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버에서 예기치 않은 오류가 발생했습니다.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}