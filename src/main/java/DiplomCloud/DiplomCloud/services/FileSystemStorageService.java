package DiplomCloud.DiplomCloud.services;


import DiplomCloud.DiplomCloud.exception.FileStorageException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


/*Для улучшения механизмама обеспечения целостности, реализовано резделение "зон" ответственности и
создан отдельный сервис для операций с файловой системой
* */
@Service
@Slf4j
public class FileSystemStorageService {
    private final String storagePath;

    public FileSystemStorageService(@Value("${file.storage.path}") String storagePath) {
        this.storagePath = storagePath;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void renameFileCompensable(String username, String sourceName, String targetName) {
        Path source = Paths.get(storagePath, username, sourceName);
        Path target = Paths.get(storagePath, username, targetName);

        try {
            if (Files.exists(target)) {
                throw new FileAlreadyExistsException("Целевой файл уже существует");
            }
            Files.move(source, target);
            log.info("Файл переименован в файловой системе: {} -> {}", sourceName, targetName);
        } catch (IOException e) {
            throw new FileStorageException("Сбой в работе файловой системы", e);
        }
    }

    public boolean checkFileExists(String username, String filename) {
        Path filePath = Paths.get(storagePath, username, filename);
        return Files.exists(filePath);
    }

    public void rollbackRename(String username, String sourceName, String targetName) {
        try {
            Path source = Paths.get(storagePath, username, sourceName);
            Path target = Paths.get(storagePath, username, targetName);

            if (Files.exists(target)) {
                Files.move(target, source);
                log.info("Откат прошел успешно: {} -> {}", targetName, sourceName);
            }
        } catch (IOException e) {
            log.error("Откат не удался для {}: {}", targetName, e.getMessage());
            throw new FileStorageException("Откат не удался", e);
        }
    }
}
