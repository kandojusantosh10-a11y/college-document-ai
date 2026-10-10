package com.example.backend.exception;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.backend.util.SafeAiErrorLogger;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, Object>> handleApi(ApiException ex) { return ResponseEntity.status(ex.getStatus()).body(Map.of("success", false, "message", ex.getMessage())); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) { return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getBindingResult().getFieldError().getDefaultMessage())); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) { SafeAiErrorLogger.logFailure(log, "Unhandled API request", ex); return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "Unable to complete this request right now.")); }
}
