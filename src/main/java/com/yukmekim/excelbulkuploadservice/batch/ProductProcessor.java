package com.yukmekim.excelbulkuploadservice.batch;

import com.yukmekim.excelbulkuploadservice.dto.ProductUploadDto;
import com.yukmekim.excelbulkuploadservice.entity.Product;
import org.springframework.batch.item.ItemProcessor;

public class ProductProcessor implements ItemProcessor<ProductUploadDto, Product> {

    @Override
    public Product process(ProductUploadDto item) throws Exception {
        return Product.builder()
                .name(item.getName())
                .category(item.getCategory())
                .price(item.getPrice())
                .stockQuantity(item.getStockQuantity())
                .description(item.getDescription())
                .build();
    }
}
