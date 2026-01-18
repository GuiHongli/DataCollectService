package com.datacollect.common;

import com.datacollect.common.exception.CollectTaskException;
import com.datacollect.common.exception.ConfigurationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        LOGGER.error("Parameter validation exception: {}", message);
        return Result.error(message);
    }

    /**
     * process绑定异常
     */
    @ExceptionHandler(BindException.class)
    public Result<String> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        LOGGER.error("Binding exception: {}", message);
        return Result.error(message);
    }

    /**
     * process约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<String> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        LOGGER.error("Constraint violation exception: {}", message);
        return Result.error(message);
    }

    /**
     * process采集任务异常
     */
    @ExceptionHandler(CollectTaskException.class)
    public Result<String> handleCollectTaskException(CollectTaskException e) {
        LOGGER.error("Collect task exception: {}", e.getMessage(), e);
        return Result.error("Collect task exception: " + e.getMessage());
    }

    /**
     * 处理配置异常
     */
    @ExceptionHandler(ConfigurationException.class)
    public Result<String> handleConfigurationException(ConfigurationException e) {
        LOGGER.error("Configuration exception: {}", e.getMessage(), e);
        return Result.error("Configuration exception: " + e.getMessage());
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<String> handleRuntimeException(RuntimeException e) {
        LOGGER.error("Runtime exception", e);
        return Result.error("System runtime exception: " + e.getMessage());
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        LOGGER.error("System exception", e);
        return Result.error("System exception: " + e.getMessage());
    }
}
