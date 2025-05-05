package DiplomCloud.DiplomCloud;

import DiplomCloud.DiplomCloud.dto.FileInfoResponse;
import DiplomCloud.DiplomCloud.exception.FileStorageException;
import DiplomCloud.DiplomCloud.exception.UserNotFoundException;
import DiplomCloud.DiplomCloud.models.FileEntity;
import DiplomCloud.DiplomCloud.models.User;
import DiplomCloud.DiplomCloud.repositories.FileRepository;
import DiplomCloud.DiplomCloud.repositories.UserRepository;
import DiplomCloud.DiplomCloud.security.JwtTokenProvider;
import DiplomCloud.DiplomCloud.services.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FileControllerUnitTest {
    @Mock
    private FileRepository fileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private MultipartFile multipartFile;

    @Mock
    private Resource resource;

    @InjectMocks
    private FileStorageService fileStorageService;

    private final String testUsername = "testUser";
    private final String testToken = "testToken";
    private final String testFilename = "test.txt";
    private final String testNewFilename = "newTest.txt";
    private final long testFileSize = 1024L;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setLogin(testUsername);
        fileStorageService.setStoragePath("test-storage"); // теперь можем установить значение
    }

    @Test
    void uploadFile_Success() throws IOException {
        // Arrange
        when(jwtTokenProvider.getUsername(testToken)).thenReturn(testUsername);
        when(userRepository.findByLogin(testUsername)).thenReturn(Optional.of(testUser));
        when(multipartFile.getSize()).thenReturn(testFileSize);

        Path mockPath = mock(Path.class);
        when(mockPath.resolve(testFilename)).thenReturn(mockPath);
        when(mockPath.toString()).thenReturn("test-path");

        // Mock Files.createDirectories and file transfer
        doNothing().when(multipartFile).transferTo(any(Path.class));

        // Act
        fileStorageService.uploadFile(testToken, testFilename, multipartFile);

        // Assert
        verify(jwtTokenProvider).getUsername(testToken);
        verify(userRepository).findByLogin(testUsername);
        verify(fileRepository).save(any(FileEntity.class));
    }

    @Test
    void uploadFile_UserNotFound_ThrowsException() {
        // Arrange
        when(jwtTokenProvider.getUsername(testToken)).thenReturn(testUsername);
        when(userRepository.findByLogin(testUsername)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () ->
                fileStorageService.uploadFile(testToken, testFilename, multipartFile));
    }

    @Test
    void uploadFile_IOException_ThrowsFileStorageException() throws IOException {
        // Arrange
        when(jwtTokenProvider.getUsername(testToken)).thenReturn(testUsername);
        when(userRepository.findByLogin(testUsername)).thenReturn(Optional.of(testUser));
        doThrow(IOException.class).when(multipartFile).transferTo(any(Path.class));

        // Act & Assert
        assertThrows(FileStorageException.class, () ->
                fileStorageService.uploadFile(testToken, testFilename, multipartFile));
    }

    @Test
    void downloadFile_Success() throws IOException {
        // Arrange
        Path mockPath = mock(Path.class);
        when(mockPath.toUri()).thenReturn(Paths.get(testFilename).toUri());
        when(mockPath.normalize()).thenReturn(mockPath);

        // Mock resource behavior
        when(resource.exists()).thenReturn(true);
        when(resource.isReadable()).thenReturn(true);

        // Act
        Resource result = fileStorageService.downloadFile(testToken, testFilename);

        // Assert
        assertNotNull(result);
    }

    @Test
    void downloadFile_FileNotReadable_ThrowsException() throws IOException {
        // Arrange
        Path mockPath = mock(Path.class);
        when(mockPath.toUri()).thenReturn(Paths.get(testFilename).toUri());
        when(mockPath.normalize()).thenReturn(mockPath);

        // Mock resource behavior
        when(resource.exists()).thenReturn(true);
        when(resource.isReadable()).thenReturn(false);

        // Act & Assert
        assertThrows(FileNotFoundException.class, () ->
                fileStorageService.downloadFile(testToken, testFilename));
    }

    @Test
    void deleteFile_Success() throws IOException {
        // Arrange
        when(jwtTokenProvider.getUsername(testToken)).thenReturn(testUsername);
        when(userRepository.findByLogin(testUsername)).thenReturn(Optional.of(testUser));

        Path mockPath = mock(Path.class);
        when(Files.deleteIfExists(mockPath)).thenReturn(true);

        // Act
        fileStorageService.deleteFile(testToken, testFilename);

        // Assert
        verify(fileRepository).deleteByOwnerAndFilename(testUser, testFilename);
    }

    @Test
    void deleteFile_UserNotFound_ThrowsException() {
        // Arrange
        when(jwtTokenProvider.getUsername(testToken)).thenReturn(testUsername);
        when(userRepository.findByLogin(testUsername)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () ->
                fileStorageService.deleteFile(testToken, testFilename));
    }

    @Test
    void renameFile_Success() throws IOException {
        // Arrange
        when(jwtTokenProvider.getUsername(testToken)).thenReturn(testUsername);
        when(userRepository.findByLogin(testUsername)).thenReturn(Optional.of(testUser));

        FileEntity fileEntity = new FileEntity();
        fileEntity.setFilename(testFilename);
        when(fileRepository.findByOwnerAndFilename(testUser, testFilename))
                .thenReturn(Optional.of(fileEntity));

        Path mockSource = mock(Path.class);
        Path mockTarget = mock(Path.class);
        when(mockTarget.toString()).thenReturn("new-path");

        // Act
        fileStorageService.renameFile(testToken, testFilename, testNewFilename);

        // Assert
        verify(fileRepository).save(fileEntity);
        assertEquals(testNewFilename, fileEntity.getFilename());
    }

    @Test
    void renameFile_FileNotFound_ThrowsException() {
        // Arrange
        when(jwtTokenProvider.getUsername(testToken)).thenReturn(testUsername);
        when(userRepository.findByLogin(testUsername)).thenReturn(Optional.of(testUser));
        when(fileRepository.findByOwnerAndFilename(testUser, testFilename))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(FileNotFoundException.class, () ->
                fileStorageService.renameFile(testToken, testFilename, testNewFilename));
    }

    @Test
    void listFiles_Success() {
        // Arrange
        when(jwtTokenProvider.getUsername(testToken)).thenReturn(testUsername);
        when(userRepository.findByLogin(testUsername)).thenReturn(Optional.of(testUser));

        FileEntity file1 = new FileEntity();
        file1.setFilename("file1.txt");
        file1.setSize(100L);

        FileEntity file2 = new FileEntity();
        file2.setFilename("file2.txt");
        file2.setSize(200L);

        when(fileRepository.findByOwner(testUser)).thenReturn(List.of(file1, file2));

        // Act
        List<FileInfoResponse> result = fileStorageService.listFiles(testToken, 2);

        // Assert
        assertEquals(2, result.size());
        assertEquals("file1.txt", result.get(0).getFilename());
        assertEquals(Long.valueOf(100L), result.get(0).getSize());
    }

    @Test
    void listFiles_WithLimit() {
        // Arrange
        when(jwtTokenProvider.getUsername(testToken)).thenReturn(testUsername);
        when(userRepository.findByLogin(testUsername)).thenReturn(Optional.of(testUser));

        FileEntity file1 = new FileEntity();
        file1.setFilename("file1.txt");
        file1.setSize(100L);

        FileEntity file2 = new FileEntity();
        file2.setFilename("file2.txt");
        file2.setSize(200L);

        when(fileRepository.findByOwner(testUser)).thenReturn(List.of(file1, file2));

        // Act
        List<FileInfoResponse> result = fileStorageService.listFiles(testToken, 1);

        // Assert
        assertEquals(1, result.size());
    }
}
