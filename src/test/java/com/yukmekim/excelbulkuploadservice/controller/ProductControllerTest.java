package com.yukmekim.excelbulkuploadservice.controller;

import com.yukmekim.excelbulkuploadservice.common.exception.GlobalExceptionHandler;
import com.yukmekim.excelbulkuploadservice.entity.Product;
import com.yukmekim.excelbulkuploadservice.service.ProductService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private ProductService productService;

        // ----------------------------------------------------------------
        // GET /api/excel/products
        // ----------------------------------------------------------------

        @Test
        @DisplayName("상품이 존재할 때 GET /products는 200 OK와 상품 목록을 반환한다")
        void getProducts_ShouldReturn200WithList_WhenProductsExist() throws Exception {
                // Given
                given(productService.getAllProducts()).willReturn(List.of(
                                Product.builder().id(1L).name("P1").category("C1")
                                                .price(BigDecimal.TEN).stockQuantity(5).description("Desc").build()));

                // When & Then
                mockMvc.perform(get("/api/excel/products"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].name").value("P1"))
                                .andExpect(jsonPath("$[0].category").value("C1"));
        }

        @Test
        @DisplayName("상품이 없을 때 GET /products는 204 No Content를 반환한다")
        void getProducts_ShouldReturn204_WhenNoProductsExist() throws Exception {
                // Given
                given(productService.getAllProducts()).willReturn(List.of());

                // When & Then
                mockMvc.perform(get("/api/excel/products"))
                                .andExpect(status().isNoContent());
        }

        // ----------------------------------------------------------------
        // GET /api/excel/download
        // ----------------------------------------------------------------

        @Test
        @DisplayName("GET /download는 200 OK와 엑셀 Content-Type 헤더를 반환한다")
        void download_ShouldReturn200WithExcelContentType() throws Exception {
                // Given: 빈 스트림 반환 (내용보다 헤더/상태 코드 검증에 집중)
                given(productService.load()).willReturn(new ByteArrayInputStream(makeExcelBytes()));

                // When & Then
                mockMvc.perform(get("/api/excel/download"))
                                .andExpect(status().isOk())
                                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=products.xlsx"))
                                .andExpect(content().contentType(
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        }

        // ----------------------------------------------------------------
        // 헬퍼 메소드
        // ----------------------------------------------------------------

        private byte[] makeExcelBytes() throws Exception {
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
                return bos.toByteArray();
        }
}
