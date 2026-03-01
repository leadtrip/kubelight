package wood.mike.sbetcd.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import wood.mike.sbetcd.model.FailureResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleEtcdError(Exception ex, WebRequest request) {
        return ResponseEntity.badRequest().body(FailureResponse.failure(ex.getMessage()));
    }
}