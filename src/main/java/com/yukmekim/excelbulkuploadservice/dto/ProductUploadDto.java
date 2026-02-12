package com.yukmekim.excelbulkuploadservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUploadDto {
    private String name;
    private String category;
    private BigDecimal price;
    private Integer stockQuantity;
    private String description;
}
