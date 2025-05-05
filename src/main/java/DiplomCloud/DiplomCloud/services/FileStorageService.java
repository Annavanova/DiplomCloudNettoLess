package DiplomCloud.DiplomCloud.services;


import DiplomCloud.DiplomCloud.dto.FileInfoResponse;
import DiplomCloud.DiplomCloud.exception.FileStorageException;
import DiplomCloud.DiplomCloud.exception.UserNotFoundException;
import DiplomCloud.DiplomCloud.models.FileEntity;
import DiplomCloud.DiplomCloud.models.User;
import DiplomCloud.DiplomCloud.repositories.FileRepository;
import DiplomCloud.DiplomCloud.repositories.UserRepository;
import DiplomCloud.DiplomCloud.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
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
public class FileStorageService {
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${file.storage.path}")
    private String storagePath;

    // Сеттер с доступом в пределах пакета (для тестов)
    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public void uploadFile(String authToken, String filename, MultipartFile file) {
        String username = jwtTokenProvider.getUsername(authToken);
        User user = userRepository.findByLogin(username)
                .orElseThrow(() -> new UserNotFoundException(username));

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
        } catch (IOException e) {
            throw new FileStorageException("Не удалось сохранить фвйл " + filename, e);
        }
    }

    public Resource downloadFile(String authToken, String filename) {
        // 1. Получаем безопасный путь
        Path rootLocation = Paths.get(storagePath).normalize();
        Path filePath = rootLocation.resolve(filename).normalize();

        // 2. Проверка безопасности
        if (!filePath.startsWith(rootLocation)) {
            throw new SecurityException("Не удается получить доступ к файлу");
        }

        // 3. Создаем Resource с обработкой ошибок
        try {
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new FileNotFoundException("Файл не найден или недоступен для чтения: " + filename);
            }
        } catch (MalformedURLException | FileNotFoundException ex) {
            throw new FileStorageException("Неверный путь к файлу: " + filePath, ex);
        }
    }

    public void deleteFile(String authToken, String filename) {
        String username = jwtTokenProvider.getUsername(authToken);
        User user = userRepository.findByLogin(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        Path filePath = Paths.get(storagePath, username, filename);
        try {
            Files.deleteIfExists(filePath);
            fileRepository.deleteByOwnerAndFilename(user, filename);
        } catch (IOException e) {
            throw new FileStorageException("Не удалось удалить файл " + filename, e);
        }
    }

    public void renameFile(String authToken, String filename, String newName) {
        String username = jwtTokenProvider.getUsername(authToken);
        User user = userRepository.findByLogin(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        Path source = Paths.get(storagePath, username, filename);
        Path target = Paths.get(storagePath, username, newName);

        try {
            Files.move(source, target);

            FileEntity fileEntity = fileRepository.findByOwnerAndFilename(user, filename)
                    .orElseThrow(() -> new FileNotFoundException("Файл не найден: " + filename));

            fileEntity.setFilename(newName);
            fileEntity.setFilePath(target.toString());
            fileRepository.save(fileEntity);
        } catch (IOException e) {
            throw new FileStorageException("Не удалось переименовать файл " + filename, e);
        }
    }

    public List<FileInfoResponse> listFiles(String authToken, int limit) {
        String username = jwtTokenProvider.getUsername(authToken);
        User user = userRepository.findByLogin(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        return fileRepository.findByOwner(user).stream()
                .limit(limit > 0 ? limit : Long.MAX_VALUE)
                .map(file -> new FileInfoResponse(file.getFilename(), file.getSize()))
                .collect(Collectors.toList());
    }

}
