package com.yukmekim.excelbulkuploadservice.service;

import com.yukmekim.excelbulkuploadservice.dto.ProductUploadDto;
import com.yukmekim.excelbulkuploadservice.entity.Product;
import com.yukmekim.excelbulkuploadservice.entity.UploadHistory;
import com.yukmekim.excelbulkuploadservice.entity.UploadStatus;
import com.yukmekim.excelbulkuploadservice.repository.ProductRepository;
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
    private final UploadHistoryService uploadHistoryService;
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
        // "products" 테이블로 가정하고, 임시 사용자 식별자 "admin" 줌
        UploadHistory history = uploadHistoryService.createPendingHistory(file.getOriginalFilename(), "products", "admin");
        Long historyId = history.getId();

        try {
            uploadHistoryService.startProcessing(historyId);

            List<ProductUploadDto> dtos = ExcelHelper.excelToProducts(file.getInputStream());
            List<Product> products = dtos.stream()
                    .map(this::mapToEntity)
                    .collect(Collectors.toList());

            saveProducts(products);

            uploadHistoryService.completeHistory(historyId, products.size(), products.size(), 0);

        } catch (IOException e) {
            String errorMsg = "[IOException] " + e.getMessage();
            uploadHistoryService.failHistory(historyId, errorMsg);
            throw new RuntimeException("Failed to process excel file: " + e.getMessage(), e);
        } catch (Exception e) {
            String errorMsg = "[" + e.getClass().getSimpleName() + "] " + e.getMessage();
            uploadHistoryService.failHistory(historyId, errorMsg);
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

}
