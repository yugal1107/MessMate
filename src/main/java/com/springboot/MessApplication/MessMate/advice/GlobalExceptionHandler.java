package com.springboot.MessApplication.MessMate.advice;

import com.springboot.MessApplication.MessMate.exceptions.*;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<?>> handleRuntimeException(RuntimeException exception) {
        ApiError apiError = new ApiError("RuntimeException", "Oops, something went wrong", HttpStatus.INTERNAL_SERVER_ERROR);
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleResourceNotFoundException(ResourceNotFoundException exception) {
        ApiError apiError = new ApiError(exception.getClass().getSimpleName(), exception.getMessage(), HttpStatus.NOT_FOUND);
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthenticationException(AuthenticationException exception) {
        ApiError apiError = new ApiError(exception.getClass().getSimpleName(), exception.getMessage(), HttpStatus.UNAUTHORIZED);
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiResponse<?>> handleJwtException(JwtException exception) {
        ApiError apiError = new ApiError(exception.getClass().getSimpleName(), exception.getMessage(), HttpStatus.UNAUTHORIZED);
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDeniedException(AccessDeniedException exception) {
        ApiError apiError = new ApiError(exception.getClass().getSimpleName(), exception.getMessage(), HttpStatus.FORBIDDEN);
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(MealOffDeadlineException.class)
    public ResponseEntity<ApiResponse<?>> handleMealOffDeadlineException(MealOffDeadlineException exception) {
        ApiError apiError = new ApiError(exception.getClass().getSimpleName(), exception.getMessage(), HttpStatus.BAD_REQUEST);
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<?>> handleResponseStatusException(ResponseStatusException exception) {
        ApiError apiError = new ApiError(exception.getClass().getSimpleName(), exception.getMessage(), exception.getStatusCode());
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(UserNotSubscribedException.class)
    public ResponseEntity<ApiResponse<?>> handleUserNotSubscribedException(UserNotSubscribedException exception) {
        ApiError apiError = new ApiError(exception.getClass().getSimpleName(), exception.getMessage(), HttpStatus.BAD_REQUEST);
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(InvalidMealOffStateException.class)
    public ResponseEntity<ApiResponse<?>> handleInvalidMealOffStateException(InvalidMealOffStateException exception) {
        ApiError apiError = new ApiError(exception.getClass().getSimpleName(), exception.getMessage(), HttpStatus.BAD_REQUEST);
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(InvalidCustomOffRequestException.class)
    public ResponseEntity<ApiResponse<?>> handleInvalidCustomOffRequestException(InvalidCustomOffRequestException exception) {
        ApiError apiError = new ApiError(exception.getClass().getSimpleName(), exception.getMessage(), HttpStatus.BAD_REQUEST);
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(PasswordResetException.class)
    public ResponseEntity<ApiResponse<?>> handlePasswordResetException(PasswordResetException exception) {
        ApiError apiError = new ApiError(exception.getClass().getSimpleName(), exception.getMessage(), HttpStatus.BAD_REQUEST);
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<?>> handleBadRequestException(BadRequestException exception) {
        ApiError apiError = new ApiError(exception.getClass().getSimpleName(), exception.getMessage(), HttpStatus.BAD_REQUEST);
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(EmailSendingException.class)
    public ResponseEntity<ApiResponse<?>> handleEmailSendingException(EmailSendingException exception) {
        ApiError apiError = new ApiError(exception.getClass().getSimpleName(), exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        return buildErrorResponseEntity(apiError);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .distinct()
                .collect(Collectors.joining(", "));

        ApiError apiError = new ApiError(exception.getClass().getSimpleName(), message, HttpStatus.BAD_REQUEST);
        return buildErrorResponseEntity(apiError);
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    private ResponseEntity<ApiResponse<?>> buildErrorResponseEntity(ApiError apiError) {
        return new ResponseEntity<>(new ApiResponse<>(apiError), apiError.getStatusCode());
    }
}
