package com.erp.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> business(BusinessException exception) {
        return ApiResult.failure(exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> validation(MethodArgumentNotValidException exception) {
        var error = exception.getBindingResult().getFieldErrors().stream().findFirst();
        return ApiResult.failure(error.map(e -> e.getDefaultMessage() == null ? "参数错误" : e.getDefaultMessage()).orElse("参数错误"));
    }
}

