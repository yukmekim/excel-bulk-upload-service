package com.yukmekim.excelbulkuploadservice.service;

import com.yukmekim.excelbulkuploadservice.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProductBatchService {

    private final JobLauncher jobLauncher;
    private final Job productUploadJob;
    private final FileStorageService fileStorageService;

    /**
     * Run Spring Batch Job for bulk upload (Async)
     */
    public JobExecution runJob(MultipartFile file) throws Exception {
        // Store the uploaded file using FileStorageService (Local, S3, etc.)
        String storedFilePath = fileStorageService.store(file);

        JobParameters params = new JobParametersBuilder()
                .addString("filePath", storedFilePath)
                .addString("originalFileName", file.getOriginalFilename())
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        return jobLauncher.run(productUploadJob, params);
    }
}
