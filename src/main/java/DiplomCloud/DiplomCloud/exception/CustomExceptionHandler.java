package DiplomCloud.DiplomCloud.exception;

import DiplomCloud.DiplomCloud.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;


import java.io.FileNotFoundException;
import java.util.stream.Collectors;

@ControllerAdvice
public class CustomExceptionHandler {


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return ResponseEntity.badRequest()
                .body(new ErrorResponse(errorMessage, 400));
    }

    @ExceptionHandler({UserNotFoundException.class, BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleUserExceptions(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(ex.getMessage(), 401));
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFileNotFound(FileNotFoundException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(ex.getMessage(), 400));
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponse> handleFileStorage(FileStorageException ex) {
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse(ex.getMessage(), 500));
    }

    @ExceptionHandler(FileAlreadyExistsRuntimeException.class)
    public ResponseEntity<ErrorResponse> handleFileExists(FileAlreadyExistsRuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage(), 409));
    }
}
