package com.yukmekim.excelbulkuploadservice.service.storage;

import com.yukmekim.excelbulkuploadservice.common.exception.BusinessException;
import com.yukmekim.excelbulkuploadservice.common.exception.ErrorCode;
import com.yukmekim.excelbulkuploadservice.config.FileStorageProperties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "file", name = "type", havingValue = "LOCAL", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private final Path rootLocation;

    public LocalFileStorageService(FileStorageProperties properties) {
        String uploadDir = properties.getLocal().getUploadDir();
        if (uploadDir == null || uploadDir.trim().isEmpty()) {
            throw new RuntimeException("File upload location can not be empty.");
        }

        this.rootLocation = Paths.get(uploadDir)
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage location", e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        String filename = StringUtils.cleanPath(file.getOriginalFilename());

        // Generate unique filename to avoid collision
        String uniqueFilename = UUID.randomUUID().toString() + "_" + filename;

        try {
            if (file.isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Failed to store empty file " + filename);
            }
            if (filename.contains("..")) {
                // This is a security check
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                        "Cannot store file with relative path outside current directory " + filename);
            }

            Path targetLocation = this.rootLocation.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return targetLocation.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to store file " + filename);
        }
    }

    @Override
    public Resource load(String filename) {
        try {
            Path file = rootLocation.resolve(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Could not read file: " + filename);
        }
    }

    @Override
    public void delete(String pathStr) {
        Path file = Paths.get(pathStr);
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Could not delete file: " + file);
        }
    }

    @Override
    public Path getRootLocation() {
        return rootLocation;
    }
}
