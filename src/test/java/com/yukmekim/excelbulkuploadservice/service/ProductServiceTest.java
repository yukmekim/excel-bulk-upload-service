package com.yukmekim.excelbulkuploadservice.service;

import com.yukmekim.excelbulkuploadservice.entity.Product;
import com.yukmekim.excelbulkuploadservice.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("getAllProducts 호출 시 ProductRepository.findAll()이 호출된다")
    void getAllProducts_ShouldCallRepositoryFindAll() {
        // Given
        given(productRepository.findAll()).willReturn(List.of(
                Product.builder().name("P1").category("C1").price(BigDecimal.TEN).stockQuantity(1).build()));

        // When
        List<Product> result = productService.getAllProducts();

        // Then
        assertThat(result).hasSize(1);
        verify(productRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("load 호출 시 DB에서 상품 목록을 가져와 엑셀 스트림으로 반환한다")
    void load_ShouldReturnExcelInputStream() {
        // Given: id가 없으면 ExcelHelper.productsToExcel() 내부에서 NPE 발생하므로 id 설정
        given(productRepository.findAll()).willReturn(List.of(
                Product.builder().id(1L).name("P1").category("C1").price(BigDecimal.TEN).stockQuantity(1).build()));

        // When
        ByteArrayInputStream result = productService.load();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.available()).isGreaterThan(0);
    }

    // ----------------------------------------------------------------
}
