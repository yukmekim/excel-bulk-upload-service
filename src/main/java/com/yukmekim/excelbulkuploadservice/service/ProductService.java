package com.yukmekim.excelbulkuploadservice.service;

import com.yukmekim.excelbulkuploadservice.dto.ProductUploadDto;
import com.yukmekim.excelbulkuploadservice.entity.Product;
import com.yukmekim.excelbulkuploadservice.entity.UploadHistory;
import com.yukmekim.excelbulkuploadservice.entity.UploadStatus;
import com.yukmekim.excelbulkuploadservice.repository.ProductRepository;
import com.yukmekim.excelbulkuploadservice.repository.UploadHistoryRepository;
import com.yukmekim.excelbulkuploadservice.service.storage.FileStorageService;
import com.yukmekim.excelbulkuploadservice.util.ExcelHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UploadHistoryRepository uploadHistoryRepository;
    private final FileStorageService fileStorageService;

    // Batch Dependencies
    private final JobLauncher jobLauncher;
    private final Job productUploadJob;

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

    /**
     * Original Synchronous Save Method (Refactored)
     */
    public void save(MultipartFile file) {
        UploadHistory history = createUploadHistory(file.getOriginalFilename());

        try {
            updateHistoryStatus(history, UploadStatus.IN_PROGRESS);

            List<ProductUploadDto> dtos = ExcelHelper.excelToProducts(file.getInputStream());
            List<Product> products = dtos.stream()
                    .map(this::mapToEntity)
                    .collect(Collectors.toList());

            saveProducts(products);

            completeHistory(history, products.size(), products.size(), 0);

        } catch (IOException e) {
            String errorMsg = "[IOException] " + e.getMessage();
            history.fail(errorMsg);
            uploadHistoryRepository.save(history);
            throw new RuntimeException("Failed to process excel file: " + e.getMessage(), e);
        } catch (Exception e) {
            String errorMsg = "[" + e.getClass().getSimpleName() + "] " + e.getMessage();
            history.fail(errorMsg);
            uploadHistoryRepository.save(history);
            throw new RuntimeException("Unexpected error during upload: " + e.getMessage(), e);
        }
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public ByteArrayInputStream load() {
        List<Product> products = productRepository.findAll();
        return ExcelHelper.productsToExcel(products);
    }

    @Transactional
    protected void saveProducts(List<Product> products) {
        productRepository.saveAll(products);
    }

    private Product mapToEntity(ProductUploadDto dto) {
        return Product.builder()
                .name(dto.getName())
                .category(dto.getCategory())
                .price(dto.getPrice())
                .stockQuantity(dto.getStockQuantity())
                .description(dto.getDescription())
                .build();
    }

    // --- Helper methods for UploadHistory management ---

    private UploadHistory createUploadHistory(String fileName) {
        UploadHistory history = UploadHistory.builder()
                .fileName(fileName)
                .status(UploadStatus.PENDING)
                .build();
        return uploadHistoryRepository.save(history);
    }

    private void updateHistoryStatus(UploadHistory history, UploadStatus status) {
        if (status == UploadStatus.IN_PROGRESS) {
            history.startProcessing();
        }
        uploadHistoryRepository.save(history);
    }

    private void completeHistory(UploadHistory history, int total, int success, int failure) {
        history.complete(total, success, failure);
        uploadHistoryRepository.save(history);
    }
}
