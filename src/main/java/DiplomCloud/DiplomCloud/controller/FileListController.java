package DiplomCloud.DiplomCloud.controller;

import DiplomCloud.DiplomCloud.dto.ErrorResponse;
import DiplomCloud.DiplomCloud.dto.FileInfoResponse;
import DiplomCloud.DiplomCloud.exception.UserNotFoundException;
import DiplomCloud.DiplomCloud.models.FileEntity;
import DiplomCloud.DiplomCloud.security.JwtTokenProvider;
import DiplomCloud.DiplomCloud.services.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileListController {
    private final FileStorageService fileStorageService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping
    public ResponseEntity<?> listFiles(
            @RequestHeader("auth-token") String token,
            @RequestParam(value = "limit", defaultValue = "0") int limit) {

        log.info("Запрос списка файлов - лимит: {}", limit);
        try {
            String username = jwtTokenProvider.getUsername(token);
            List<FileEntity> files = fileStorageService.listFiles(username, limit);
            List<FileInfoResponse> response = files.stream()
                    .map(file -> new FileInfoResponse(file.getFilename(), file.getSize()))
                    .collect(Collectors.toList());

            log.debug("{} файлов для пользователя: {}", response.size(), username);
            return ResponseEntity.ok(response);
        } catch (UserNotFoundException e) {
            log.error("Попытка несанкционированного доступа");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(e.getMessage(), 401));
        } catch (Exception e) {
            log.error("Ошибка в списке файлов: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new ErrorResponse("Ошибка при получении списка файлов", 500));
        }
    }
}
