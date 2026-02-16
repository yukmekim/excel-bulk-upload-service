package com.yukmekim.excelbulkuploadservice;

import com.yukmekim.excelbulkuploadservice.entity.Product;
import com.yukmekim.excelbulkuploadservice.repository.ProductRepository;
import com.yukmekim.excelbulkuploadservice.repository.UploadHistoryRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
class ProductUploadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UploadHistoryRepository uploadHistoryRepository;

    @BeforeEach
    void setup() {
        productRepository.deleteAll();
        uploadHistoryRepository.deleteAll();
    }

    @Test
    void shouldUploadExcelFileAndSaveProducts() throws Exception {
        // Create a valid Excel file in memory
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Products");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Name");
        header.createCell(1).setCellValue("Category");
        header.createCell(2).setCellValue("Price");
        header.createCell(3).setCellValue("Stock Quantity");
        header.createCell(4).setCellValue("Description");

        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("Integration Test Product");
        row1.createCell(1).setCellValue("Test Category");
        row1.createCell(2).setCellValue(99.99);
        row1.createCell(3).setCellValue(100);
        row1.createCell(4).setCellValue("Integration Description");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "integration-test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bos.toByteArray());

        // Perform upload
        mockMvc.perform(multipart("/api/excel/upload").file(file))
                .andExpect(status().isOk());

        // Verify Database
        List<Product> products = productRepository.findAll();
        assertThat(products).hasSize(1);

        Product savedProduct = products.get(0);
        assertThat(savedProduct.getName()).isEqualTo("Integration Test Product");
        assertThat(savedProduct.getCategory()).isEqualTo("Test Category");
        assertThat(savedProduct.getPrice()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(savedProduct.getStockQuantity()).isEqualTo(100);

        // Verify History
        assertThat(uploadHistoryRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldDownloadExcelFile() throws Exception {
        // Given: Save a product to DB
        Product product = Product.builder()
                .name("Download Test Product")
                .category("Test Category")
                .price(new BigDecimal("150.00"))
                .stockQuantity(50)
                .description("Description for download test")
                .build();
        productRepository.save(product);

        // When & Then: Perform download request
        mockMvc.perform(get("/api/excel/download"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=products.xlsx"))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    public void shouldReturnBadRequestWhenFileIsNotExcel() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Just some text content".getBytes());

        mockMvc.perform(multipart("/api/excel/upload").file(file))
                .andExpect(status().isBadRequest());
    }
}
