package com.yukmekim.excelbulkuploadservice;

import com.yukmekim.excelbulkuploadservice.common.exception.GlobalExceptionHandler;
import com.yukmekim.excelbulkuploadservice.controller.ExcelImportController;
import com.yukmekim.excelbulkuploadservice.service.DynamicUploadService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DynamicUploadService를 Mock 처리하여 컨트롤러 레이어에서의
 * 엑셀 업로드 흐름을 검증하는 통합 슬라이스 테스트.
 */
@WebMvcTest(ExcelImportController.class)
@Import(GlobalExceptionHandler.class)
class ProductUploadIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private DynamicUploadService dynamicUploadService;

        @Test
        void shouldUploadExcelFileAndReturn200() throws Exception {
                // Create a valid Excel file in memory
                XSSFWorkbook workbook = new XSSFWorkbook();
                Sheet sheet = workbook.createSheet("Products");

                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("name");
                header.createCell(1).setCellValue("category");
                header.createCell(2).setCellValue("price");
                header.createCell(3).setCellValue("stock_quantity");
                header.createCell(4).setCellValue("description");

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

                doNothing().when(dynamicUploadService)
                                .uploadDynamicExcel(any(), eq("products"), eq("system"));

                mockMvc.perform(multipart("/api/excel/dynamic/upload")
                                .file(file)
                                .param("targetTableName", "products")
                                .param("uploaderId", "system"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message")
                                                .value("Successfully uploaded file 'integration-test.xlsx' into table 'products'"));
        }

        @Test
        void shouldReturnBadRequestWhenFileIsNotExcel() throws Exception {
                MockMultipartFile file = new MockMultipartFile(
                                "file",
                                "test.txt",
                                "text/plain",
                                "Just some text content".getBytes());

                mockMvc.perform(multipart("/api/excel/dynamic/upload")
                                .file(file)
                                .param("targetTableName", "products")
                                .param("uploaderId", "system"))
                                .andExpect(status().isBadRequest());
        }
}
