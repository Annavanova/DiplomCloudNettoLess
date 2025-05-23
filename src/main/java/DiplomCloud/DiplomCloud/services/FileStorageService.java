package DiplomCloud.DiplomCloud.services;


import DiplomCloud.DiplomCloud.exception.*;
import DiplomCloud.DiplomCloud.models.FileEntity;
import DiplomCloud.DiplomCloud.models.User;
import DiplomCloud.DiplomCloud.repositories.FileRepository;
import DiplomCloud.DiplomCloud.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final FileSystemStorageService fileSystemStorageService;

    @Value("${file.storage.path}")
    private String storagePath;

    public void uploadFile(String username, String filename, MultipartFile file) {
        log.info("Запрос на загрузку - Пользователь: {}, имя файла: {}, размер: {} bytes",
                username, filename, file.getSize());

        User user = userRepository.findByLogin(username)
                .orElseThrow(() -> {
                    log.error("Пользователь не найден: {}", username);
                    return new UserNotFoundException(username);
                });

        Path userDir = Paths.get(storagePath, username);
        try {
            Files.createDirectories(userDir);
            Path destination = userDir.resolve(filename);
            file.transferTo(destination);

            FileEntity fileEntity = new FileEntity();
            fileEntity.setFilename(filename);
            fileEntity.setFilePath(destination.toString());
            fileEntity.setSize(file.getSize());
            fileEntity.setOwner(user);

            fileRepository.save(fileEntity);
            log.info("Файл успешно загружен - пользователь: {}, имя файла: {}", username, filename);
        } catch (IOException e) {
            log.error("Не удалось загрузить файл - пользователь: {}, имя файла: {}, ошибка: {}",
                    username, filename, e.getMessage());
            throw new FileStorageException("Не удалось сохранить файл " + filename, e);
        }
    }

    public Resource downloadFile(String username, String filename) {
        log.info("Запрос на загрузку - пользователь: {}, имя файла: {}", username, filename);

        Path rootLocation = Paths.get(storagePath).normalize();
        Path filePath = rootLocation.resolve(username).resolve(filename).normalize();

        if (!filePath.startsWith(rootLocation)) {
            log.error("Нарушение безопасности - попытка доступа за пределы каталога хранилища: {}", filePath);
            throw new SecurityException("Не удается получить доступ к файлу");
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                log.debug("Файл найден и доступен для чтения - путь: {}", filePath);
                return resource;
            } else {
                log.warn("Файл не найден или недоступен для чтения - путь: {}", filePath);
                throw new FileNotFoundException("Файл не найден или недоступен для чтения: " + filename);
            }
        } catch (MalformedURLException | FileNotFoundException ex) {
            log.error("Не удалось загрузить файл - путь: {}, ошибка: {}", filePath, ex.getMessage());
            throw new FileStorageException("Неверный путь к файлу: " + filePath, ex);
        }
    }

    public void deleteFile(String username, String filename) {
        log.info("Запрос на удаление - пользователь: {}, имя файла: {}", username, filename);

        User user = userRepository.findByLogin(username)
                .orElseThrow(() -> {
                    log.error("Пользователь не найден: {}", username);
                    return new UserNotFoundException(username);
                });

        Path filePath = Paths.get(storagePath, username, filename);
        try {
            Files.deleteIfExists(filePath);
            fileRepository.deleteByOwnerAndFilename(user, filename);
            log.info("Файл успешно удален - пользователь: {}, имя файла: {}", username, filename);
        } catch (IOException e) {
            log.error("Не удалось удалить файл - пользователь: {}, имя файла: {}, ошибка: {}",
                    username, filename, e.getMessage());
            throw new FileStorageException("Не удалось удалить файл " + filename, e);
        }
    }

    @Transactional
    public void renameFile(String username, String filename, String newName) {
        log.info("Запуск операции переименования: {} -> {}", filename, newName);

        User user = userRepository.findByLogin(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        // 1. Проверка существования целевого файла
        if (fileSystemStorageService.checkFileExists(username, newName)) {
            throw new FileAlreadyExistsException("Файл уже существует: " + newName);
        }

        // 2. Получаем текущую запись
        FileEntity fileEntity = fileRepository.findByOwnerAndFilename(user, filename)
                .orElseThrow(() -> new FileNotFoundRuntimeException("Файл не найден: " + filename));

        // 3. Сохраняем оригинальные значения ТОЛЬКО для логов
        String originalName = fileEntity.getFilename();
        String originalPath = fileEntity.getFilePath();

        // 5. Обновляем запись в БД (в рамках транзакции)
        fileEntity.setFilename(newName);
        fileEntity.setFilePath(Paths.get(storagePath, username, newName).toString());
        fileRepository.save(fileEntity);
        log.debug("Обновлена запись в базе данных: {} -> {}", originalName, newName);

        try {
            // 6. Выполняем операцию в файловой системе
            fileSystemStorageService.renameFileInFS(username, filename, newName);
            log.info("Файл успешно переименован в файловой системе");

            // 7. Проверка целостности после операции
            if (!integrityCheckAfterRename(username, newName, fileEntity.getId())) {
                throw new ConsistencyException("Не удалось выполнить проверку целостности после переименования");
            }
        } catch (Exception e) {
            log.error("Сбой в работе файловой системы приведет к автоматическому откату транзакции");
            // Spring автоматически откатит транзакцию БД из-за @Transactional
            throw new FileStorageException("Не удалось выполнить операцию переименования файла", e);
        }
    }

    private boolean integrityCheckAfterRename(String username, String filename, Long fileId) {
        // Проверяем существование файла в ФС
        boolean fsExists = fileSystemStorageService.checkFileExists(username, filename);

        // Проверяем существование записи в БД
        boolean dbExists = fileRepository.existsByIdAndFilename(fileId, filename);

        if (!fsExists || !dbExists) {
            log.error("Не удалось выполнить проверку целостности - FS существует: {}, DB существует: {}", fsExists, dbExists);
            return false;
        }

        // Дополнительные проверки, если нужно
        FileEntity entity = fileRepository.findById(fileId).orElse(null);
        if (entity != null) {
            Path expectedPath = Paths.get(storagePath, username, filename);
            if (!expectedPath.toString().equals(entity.getFilePath())) {
                log.error("Несоответствие пути - ожидаемый: {}, фактический: {}",
                        expectedPath, entity.getFilePath());
                return false;
            }
        }

        return true;
     }

    public List<FileEntity> listFiles(String username, int limit) {
        log.info("Запрос списка файлов - пользователь: {}, limit: {}", username, limit);

        User user = userRepository.findByLogin(username)
                .orElseThrow(() -> {
                    log.error("Пользователь не найден: {}", username);
                    return new UserNotFoundException(username);
                });

        List<FileEntity> files = fileRepository.findByOwner(user).stream()
                .limit(limit > 0 ? limit : Long.MAX_VALUE)
                .collect(Collectors.toList());

        log.debug("Найдено {} файло у пользователя: {}", files.size(), username);
        return files;
    }
}
