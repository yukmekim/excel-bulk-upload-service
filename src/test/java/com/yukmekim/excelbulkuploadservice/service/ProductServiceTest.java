package com.yukmekim.excelbulkuploadservice.service;

import com.yukmekim.excelbulkuploadservice.entity.Product;
import com.yukmekim.excelbulkuploadservice.entity.UploadHistory;
import com.yukmekim.excelbulkuploadservice.entity.UploadStatus;
import com.yukmekim.excelbulkuploadservice.repository.ProductRepository;
import com.yukmekim.excelbulkuploadservice.repository.ProductRepository;
import com.yukmekim.excelbulkuploadservice.service.storage.FileStorageService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UploadHistoryService uploadHistoryService;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ProductService productService;

    // ----------------------------------------------------------------
    // save() 테스트
    // ----------------------------------------------------------------

    @Test
    @DisplayName("save 성공 시 상품이 저장되고 UploadHistory 상태가 COMPLETED로 변경된다")
    void save_ShouldSaveProductsAndCompleteHistory() throws Exception {
        // Given: 유효한 엑셀 파일 생성
        MockMultipartFile file = makeValidExcelFile();

        // UploadHistory mock 설정
        UploadHistory mockHistory = UploadHistory.builder()
                .fileName("products.xlsx")
                .targetTableName("products")
                .uploaderId("admin")
                .status(UploadStatus.PENDING)
                .build();
        given(uploadHistoryService.createPendingHistory(any(), any(), any())).willReturn(mockHistory);

        // When
        productService.save(file);

        // Then: productRepository.saveAll이 1번 호출되었는지 검증
        verify(productRepository, times(1)).saveAll(anyList());
        verify(uploadHistoryService, times(1)).completeHistory(any(), anyInt(), anyInt(), anyInt());
    }

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
    // 헬퍼 메소드
    // ----------------------------------------------------------------

    private MockMultipartFile makeValidExcelFile() throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Products");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Name");
        header.createCell(1).setCellValue("Category");
        header.createCell(2).setCellValue("Price");
        header.createCell(3).setCellValue("Stock Quantity");
        header.createCell(4).setCellValue("Description");

        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue("Test Product");
        row.createCell(1).setCellValue("Electronics");
        row.createCell(2).setCellValue(99.99);
        row.createCell(3).setCellValue(10);
        row.createCell(4).setCellValue("Test Description");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();

        return new MockMultipartFile(
                "file", "products.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bos.toByteArray());
    }
}
