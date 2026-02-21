package com.yukmekim.excelbulkuploadservice.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

        @Autowired
        private MockMvc mockMvc;

        @Test
        @DisplayName("엑셀 파일이 아닌 파일 업로드 시 400 Bad Request와 에러 메시지를 반환한다")
        void upload_ShouldReturn400_WhenFileIsNotExcel() throws Exception {
                // Given: 일반 텍스트 파일
                byte[] content = "not an excel file".getBytes();
                org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                                "file", "test.txt", "text/plain", content);

                // When & Then: BusinessException(EXCEL_FILE_NOT_FOUND) 발생 ->
                // GlobalExceptionHandler가 400 반환
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .multipart("/api/excel/upload").file(file))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                                .andExpect(jsonPath("$.message").value(ErrorCode.EXCEL_FILE_NOT_FOUND.getMessage()));
        }

        @Test
        @DisplayName("엑셀 파일 업로드 성공 시 200 OK를 반환한다")
        void upload_ShouldReturn200_WhenFileIsValidExcel() throws Exception {
                // Given: 유효한 엑셀 파일 생성
                org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Products");

                org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("Name");
                header.createCell(1).setCellValue("Category");
                header.createCell(2).setCellValue("Price");
                header.createCell(3).setCellValue("Stock Quantity");
                header.createCell(4).setCellValue("Description");

                org.apache.poi.ss.usermodel.Row row = sheet.createRow(1);
                row.createCell(0).setCellValue("Test Product");
                row.createCell(1).setCellValue("Test Category");
                row.createCell(2).setCellValue(100.0);
                row.createCell(3).setCellValue(10);
                row.createCell(4).setCellValue("Test Description");

                java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                workbook.write(bos);
                workbook.close();

                org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                                "file", "products.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                bos.toByteArray());

                // When & Then
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .multipart("/api/excel/upload").file(file))
                                .andExpect(status().isOk());
        }
}
