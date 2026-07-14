package com.rongzer.connector.bigfilesftp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
/**
 * 全局异常处理器。
 *
 * <p>统一将参数校验、业务参数错误和系统异常转换为前端可读的 JSON 响应。</p>
 */
public class GlobalExceptionHandler {

    /**
     * 处理 Bean Validation 参数校验异常。
     *
     * @param exception 参数校验异常
     * @return 400 响应和字段错误列表
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        List<String> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest().body(Map.of("message", "参数校验失败", "errors", errors));
    }

    /**
     * 处理业务参数异常。
     *
     * @param exception 非法参数异常
     * @return 400 响应和错误消息
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    /**
     * 处理未被其它方法捕获的异常。
     *
     * @param exception 服务端异常
     * @return 500 响应和错误消息
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", exception.getMessage() == null ? "服务端处理失败" : exception.getMessage()));
    }
}
