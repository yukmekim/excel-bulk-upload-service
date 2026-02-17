package com.yukmekim.excelbulkuploadservice.service.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface FileStorageService {
    /**
     * Store a file and return its accessible path or identifier.
     * 
     * @param file The file to store
     * @return The stored file path or identifier
     */
    String store(MultipartFile file);

    /**
     * Load a file as a Resource.
     * 
     * @param filename The identifier of the file to load
     * @return Resource representing the file
     */
    Resource load(String filename);

    /**
     * Delete a stored file.
     * 
     * @param filename The identifier of the file to delete
     */
    void delete(String filename);

    /**
     * Get the root location path if applicable (mainly for local storage)
     */
    Path getRootLocation();
}
