package com.yukmekim.excelbulkuploadservice.service;

import com.yukmekim.excelbulkuploadservice.dto.ProductUploadDto;
import com.yukmekim.excelbulkuploadservice.entity.Product;
import com.yukmekim.excelbulkuploadservice.entity.UploadHistory;
import com.yukmekim.excelbulkuploadservice.entity.UploadStatus;
import com.yukmekim.excelbulkuploadservice.repository.ProductRepository;
import com.yukmekim.excelbulkuploadservice.repository.UploadHistoryRepository;
import com.yukmekim.excelbulkuploadservice.util.ExcelHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UploadHistoryRepository uploadHistoryRepository;

    public void save(MultipartFile file) {
        // 1. Initialize History (PENDING)
        UploadHistory history = UploadHistory.builder()
                .fileName(file.getOriginalFilename())
                .status(UploadStatus.PENDING)
                .build();
        uploadHistoryRepository.save(history);

        try {
            // 2. Start Processing (IN_PROGRESS)
            history.startProcessing();
            uploadHistoryRepository.save(history);

            // 3. Parse Excel (Returns DTOs)
            List<ProductUploadDto> dtos = ExcelHelper.excelToProducts(file.getInputStream());

            // 4. Convert DTO -> Entity
            List<Product> products = dtos.stream()
                    .map(this::mapToEntity)
                    .collect(Collectors.toList());

            // 5. Save Entities (Transactional)
            saveProducts(products);

            // 6. Complete History (COMPLETED)
            history.complete(products.size(), products.size(), 0);
            uploadHistoryRepository.save(history);

        } catch (IOException e) {
            history.fail("IO Error: " + e.getMessage());
            uploadHistoryRepository.save(history);
            throw new RuntimeException("Fail to process excel file: " + e.getMessage());
        } catch (Exception e) {
            history.fail("Unexpected Error: " + e.getMessage());
            uploadHistoryRepository.save(history);
            throw new RuntimeException("Unexpected error during upload: " + e.getMessage());
        }
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // Isolate the DB write to ensure it's transactional
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
