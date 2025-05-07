package DiplomCloud.DiplomCloud.controller;


import DiplomCloud.DiplomCloud.dto.ErrorResponse;
import DiplomCloud.DiplomCloud.dto.FileRenameRequest;
import DiplomCloud.DiplomCloud.exception.FileStorageException;
import DiplomCloud.DiplomCloud.exception.UserNotFoundException;
import DiplomCloud.DiplomCloud.security.JwtTokenProvider;
import DiplomCloud.DiplomCloud.services.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
@Slf4j
public class FileController {
    private final FileStorageService fileStorageService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping
    public ResponseEntity<?> uploadFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename,
            @RequestParam("file") MultipartFile file) {

        log.info("Запрос на загрузку файла - имя файла: {}, размер: {} bytes", filename, file.getSize());
        try {
            String username = jwtTokenProvider.getUsername(token);
            fileStorageService.uploadFile(username, filename, file);
            return ResponseEntity.ok().build();
        } catch (UserNotFoundException e) {
            log.error("Попытка несанкционированного доступа");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(e.getMessage(), 401));
        } catch (FileStorageException e) {
            log.error("Ошибка при загрузке файла: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse(e.getMessage(), 500));
        }
    }

    @GetMapping("/download")
    public ResponseEntity<?> downloadFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename) {

        log.info("Запрос на загрузку файла - имя файла: {}", filename);
        try {
            String username = jwtTokenProvider.getUsername(token);
            Resource resource = fileStorageService.downloadFile(username, filename);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (UserNotFoundException e) {
            log.error("Попытка несанкционированного доступа");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(e.getMessage(), 401));
        } catch (FileStorageException e) {
            log.error("Ошибка при загрузке файла: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse(e.getMessage(), 500));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename) {

        log.info("Запрос на удаление файла - имя файла: {}", filename);
        try {
            String username = jwtTokenProvider.getUsername(token);
            fileStorageService.deleteFile(username, filename);
            return ResponseEntity.ok().build();
        } catch (UserNotFoundException e) {
            log.error("Попытка несанкционированного доступа");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(e.getMessage(), 401));
        } catch (FileStorageException e) {
            log.error("Ошибка удаления файла: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse(e.getMessage(), 500));
        }
    }

    @PutMapping
    public ResponseEntity<?> renameFile(
            @RequestHeader("auth-token") String token,
            @RequestParam("filename") String filename,
            @RequestBody @Valid FileRenameRequest request) {

        log.info("Запрос на переименование файла - старое имя файла: {}, новое имя файла: {}", filename, request.getName());
        try {
            String username = jwtTokenProvider.getUsername(token);
            fileStorageService.renameFile(username, filename, request.getName());
            return ResponseEntity.ok().build();
        } catch (UserNotFoundException e) {
            log.error("Попытка несанкционированного доступа");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(e.getMessage(), 401));
        } catch (FileStorageException e) {
            log.error("Ошибка переименования файла: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse(e.getMessage(), 500));
        }
    }
}
