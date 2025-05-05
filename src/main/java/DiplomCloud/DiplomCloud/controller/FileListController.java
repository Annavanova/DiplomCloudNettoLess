package DiplomCloud.DiplomCloud.controller;

import DiplomCloud.DiplomCloud.dto.ErrorResponse;
import DiplomCloud.DiplomCloud.dto.FileInfoResponse;
import DiplomCloud.DiplomCloud.exception.UserNotFoundException;
import DiplomCloud.DiplomCloud.services.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileListController {
    private final FileStorageService fileStorageService;

    @GetMapping
    public ResponseEntity<?> listFiles(
            @RequestHeader("auth-token") String token,
            @RequestParam(value = "limit", defaultValue = "0") int limit) {
        try {
            List<FileInfoResponse> files = fileStorageService.listFiles(token, limit);
            return ResponseEntity.ok(files);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(e.getMessage(), 401));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("Ошибка при получении списка файлов", 500));
        }
    }
}
