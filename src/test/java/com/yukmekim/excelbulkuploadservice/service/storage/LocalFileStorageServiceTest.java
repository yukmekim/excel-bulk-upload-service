package com.yukmekim.excelbulkuploadservice.service.storage;

import com.yukmekim.excelbulkuploadservice.config.FileStorageProperties;
import com.yukmekim.excelbulkuploadservice.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalFileStorageService storageService;

    @BeforeEach
    void setUp() {
        // 테스트용 FileStorageProperties 설정 (TempDir 사용)
        FileStorageProperties properties = new FileStorageProperties();
        properties.getLocal().setUploadDir(tempDir.toString());
        storageService = new LocalFileStorageService(properties);
    }

    @Test
    @DisplayName("파일 저장 시 절대 경로 문자열이 반환된다")
    void store_ShouldReturnAbsolutePath_WhenFileIsValid() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dummy content".getBytes());

        // When
        String storedPath = storageService.store(file);

        // Then
        assertThat(storedPath).isNotBlank();
        assertThat(storedPath).endsWith("test.xlsx");
        assertThat(Path.of(storedPath)).exists();
    }

    @Test
    @DisplayName("빈 파일 저장 시 BusinessException이 발생한다")
    void store_ShouldThrowBusinessException_WhenFileIsEmpty() {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[0]);

        // When & Then
        assertThatThrownBy(() -> storageService.store(emptyFile))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("경로 탐색 패턴(..)이 포함된 파일명은 BusinessException이 발생한다")
    void store_ShouldThrowBusinessException_WhenFilenameContainsPathTraversal() {
        // Given
        MockMultipartFile maliciousFile = new MockMultipartFile(
                "file",
                "../malicious.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "content".getBytes());

        // When & Then
        assertThatThrownBy(() -> storageService.store(maliciousFile))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("저장된 파일을 정상적으로 로드할 수 있다")
    void load_ShouldReturnResource_WhenFileExists() {
        // Given: 먼저 파일 저장
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "loadtest.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dummy content".getBytes());
        String storedPath = storageService.store(file);
        String filename = Path.of(storedPath).getFileName().toString();

        // When
        Resource resource = storageService.load(filename);

        // Then
        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();
    }

    @Test
    @DisplayName("저장된 파일을 삭제하면 파일이 더 이상 존재하지 않는다")
    void delete_ShouldRemoveFile_WhenFileExists() {
        // Given: 먼저 파일 저장
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "deletetest.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dummy content".getBytes());
        String storedPath = storageService.store(file);

        // When
        storageService.delete(storedPath);

        // Then
        assertThat(Path.of(storedPath)).doesNotExist();
    }

    @Test
    @DisplayName("getRootLocation은 설정된 업로드 디렉토리 경로를 반환한다")
    void getRootLocation_ShouldReturnConfiguredPath() {
        // When
        Path rootLocation = storageService.getRootLocation();

        // Then
        assertThat(rootLocation).isNotNull();
        assertThat(rootLocation.toAbsolutePath().toString())
                .contains(tempDir.toAbsolutePath().toString());
    }
}
