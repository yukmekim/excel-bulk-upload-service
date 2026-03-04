package com.yukmekim.excelbulkuploadservice.common.exception;

import com.yukmekim.excelbulkuploadservice.controller.ExcelImportController;
import com.yukmekim.excelbulkuploadservice.service.DynamicUploadService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
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
 * GlobalExceptionHandler가 BusinessException을 올바르게 처리하는지 검증하는 테스트.
 * 전체 컨텍스트 대신 @WebMvcTest + @Import로 경량화하여 테스트 속도를 개선한다.
 */
@WebMvcTest(ExcelImportController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private DynamicUploadService dynamicUploadService;

        @Test
        @DisplayName("엑셀 파일이 아닌 파일 업로드 시 400 Bad Request와 에러 메시지를 반환한다")
        void upload_ShouldReturn400_WhenFileIsNotExcel() throws Exception {
                MockMultipartFile file = new MockMultipartFile(
                                "file", "test.txt", "text/plain", "not an excel file".getBytes());

                mockMvc.perform(multipart("/api/excel/dynamic/upload")
                                .file(file)
                                .param("targetTableName", "products")
                                .param("uploaderId", "system"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                                .andExpect(jsonPath("$.message").value(ErrorCode.EXCEL_FILE_NOT_FOUND.getMessage()));
        }

        @Test
        @DisplayName("유효한 엑셀 파일 업로드 시 200 OK를 반환한다")
        void upload_ShouldReturn200_WhenFileIsValidExcel() throws Exception {
                XSSFWorkbook workbook = new XSSFWorkbook();
                Sheet sheet = workbook.createSheet("Products");

                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("name");
                header.createCell(1).setCellValue("category");
                header.createCell(2).setCellValue("price");
                header.createCell(3).setCellValue("stock_quantity");
                header.createCell(4).setCellValue("description");

                Row row = sheet.createRow(1);
                row.createCell(0).setCellValue("Test Product");
                row.createCell(1).setCellValue("Test Category");
                row.createCell(2).setCellValue(100.0);
                row.createCell(3).setCellValue(10);
                row.createCell(4).setCellValue("Test Description");

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                workbook.write(bos);
                workbook.close();

                MockMultipartFile file = new MockMultipartFile(
                                "file", "products.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                bos.toByteArray());

                doNothing().when(dynamicUploadService).uploadDynamicExcel(any(), eq("products"), eq("system"));

                mockMvc.perform(multipart("/api/excel/dynamic/upload")
                                .file(file)
                                .param("targetTableName", "products")
                                .param("uploaderId", "system"))
                                .andExpect(status().isOk());
        }
}
