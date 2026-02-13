package com.yukmekim.excelbulkuploadservice.service;

import com.yukmekim.excelbulkuploadservice.entity.Product;
import com.yukmekim.excelbulkuploadservice.entity.UploadHistory;
import com.yukmekim.excelbulkuploadservice.entity.UploadStatus;
import com.yukmekim.excelbulkuploadservice.repository.ProductRepository;
import com.yukmekim.excelbulkuploadservice.repository.UploadHistoryRepository;
import com.yukmekim.excelbulkuploadservice.util.ExcelHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UploadHistoryRepository uploadHistoryRepository;

    public void save(MultipartFile file) {
        // 1. Initialize Upload History
        UploadHistory history = UploadHistory.builder()
                .fileName(file.getOriginalFilename())
                .status(UploadStatus.PENDING)
                .build();
        uploadHistoryRepository.save(history);

        try {
            // 2. Start Processing
            history.startProcessing();
            uploadHistoryRepository.save(history);

            // 3. Parse Excel File
            List<Product> products = ExcelHelper.excelToProducts(file.getInputStream());

            // 4. Save parsed data
            // Note: For extremely large files, we will migrate this to Spring Batch later.
            productRepository.saveAll(products);

            // 5. Update History (Success)
            history.complete(products.size(), products.size(), 0);
            uploadHistoryRepository.save(history);

        } catch (IOException e) {
            // 6. Handle Errors
            history.fail(e.getMessage());
            uploadHistoryRepository.save(history);
            throw new RuntimeException("fail to store excel data: " + e.getMessage());
        } catch (Exception e) {
            history.fail(e.getMessage());
            uploadHistoryRepository.save(history);
            throw new RuntimeException("Unexpected error during upload: " + e.getMessage());
        }
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
}
