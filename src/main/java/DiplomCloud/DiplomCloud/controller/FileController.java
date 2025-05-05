package DiplomCloud.DiplomCloud.controller;


import DiplomCloud.DiplomCloud.dto.ErrorResponse;
import DiplomCloud.DiplomCloud.dto.FileRenameRequest;
import DiplomCloud.DiplomCloud.exception.FileStorageException;
import DiplomCloud.DiplomCloud.exception.UserNotFoundException;
import DiplomCloud.DiplomCloud.services.FileStorageService;
import org.springframework.core.io.Resource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {
    private final FileStorageService fileStorageService;

    @PostMapping
    public ResponseEntity<?> uploadFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename,
            @RequestParam("file") MultipartFile file) {
        try {
            fileStorageService.uploadFile(token, filename, file);
            return ResponseEntity.ok().build();
        } catch (FileStorageException e) {
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse(e.getMessage(), 500));
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(e.getMessage(), 401));
        }
    }

    @GetMapping("/download")
    public ResponseEntity<?> downloadFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename) {
        try {
            Resource resource = fileStorageService.downloadFile(token, filename);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(e.getMessage(), 401));
        } catch (FileStorageException e) {
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse(e.getMessage(), 500));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename) {
        try {
            fileStorageService.deleteFile(token, filename);
            return ResponseEntity.ok().build();
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(e.getMessage(), 401));
        } catch (FileStorageException e) {
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse(e.getMessage(), 500));
        }
    }

    @PutMapping
    public ResponseEntity<?> renameFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename,
            @RequestBody @Valid FileRenameRequest request) {
        try {
            fileStorageService.renameFile(token, filename, request.getName());
            return ResponseEntity.ok().build();
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(e.getMessage(), 401));
        } catch (FileStorageException e) {
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse(e.getMessage(), 500));
        }
    }
}
