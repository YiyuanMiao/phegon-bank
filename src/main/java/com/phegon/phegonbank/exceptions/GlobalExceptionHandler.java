package com.phegon.phegonbank.exceptions;

import com.phegon.phegonbank.res.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

// 这是一个“切面”类，专门监听所有 Controller 的异常
@ControllerAdvice
public class GlobalExceptionHandler {

    // ================= 方法 1：兜底的（捕获所有未知异常） =================

    // 1. 声明拦截目标：只要是 Exception 及其子类，都进这个方法
    @ExceptionHandler(Exception.class)
    // 2. 返回值是 ResponseEntity，包裹着一个 Response<?> 泛型对象
    public ResponseEntity<Response<?>> handleAllUnknownExceptions(Exception ex) {

        // 3. 构建给前端看的 JSON 数据体 (Payload)
        Response<?> response = Response.builder()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value()) // 500
                .message(ex.getMessage()) // 错误信息
                .build();

        // 4. 构建 HTTP 响应包 (Header + Body + Status Code)
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ================= 方法 2：精准捕获 (NotFoundException) =================

    // 5. 声明拦截目标：只有 NotFoundException 进这个方法
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Response<?>> handleNotFoundException(NotFoundException ex) {

        // 6. 构建 JSON 数据体
        Response<?> response = Response.builder()
                .statusCode(HttpStatus.NOT_FOUND.value()) // 404
                .message(ex.getMessage())
                .build();

        // 7. 返回 404 状态码
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<Response<?>> handleInsufficientBalance(InsufficientBalanceException ex) {

        // 6. 构建 JSON 数据体
        Response<?> response = Response.builder()
                .statusCode(HttpStatus.BAD_REQUEST.value()) // 404
                .message(ex.getMessage())
                .build();

        // 7. 返回 404 状态码
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<Response<?>> handleInvalidTransaction(InvalidTransactionException ex) {

        // 6. 构建 JSON 数据体
        Response<?> response = Response.builder()
                .statusCode(HttpStatus.BAD_REQUEST.value()) // 404
                .message(ex.getMessage())
                .build();

        // 7. 返回 404 状态码
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Response<?>> handleBadRequestException(BadRequestException ex) {

        // 6. 构建 JSON 数据体
        Response<?> response = Response.builder()
                .statusCode(HttpStatus.BAD_REQUEST.value()) // 404
                .message(ex.getMessage())
                .build();

        // 7. 返回 404 状态码
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}