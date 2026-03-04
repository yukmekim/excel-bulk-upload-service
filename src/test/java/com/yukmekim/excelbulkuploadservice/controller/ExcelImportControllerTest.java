package com.yukmekim.excelbulkuploadservice.controller;

import com.yukmekim.excelbulkuploadservice.common.exception.ErrorCode;
import com.yukmekim.excelbulkuploadservice.common.exception.GlobalExceptionHandler;
import com.yukmekim.excelbulkuploadservice.service.DynamicUploadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExcelImportController.class)
@Import(GlobalExceptionHandler.class)
class ExcelImportControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private DynamicUploadService dynamicUploadService;

        // ----------------------------------------------------------------
        // POST /api/excel/dynamic/upload
        // ----------------------------------------------------------------

        @Test
        @DisplayName("유효한 엑셀 파일 동적 업로드 시 200 OK와 성공 메시지를 반환한다")
        void uploadDynamicFile_ShouldReturn200_WhenExcelFileIsValid() throws Exception {
                // Given
                MockMultipartFile file = new MockMultipartFile(
                                "file", "data.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "dummy".getBytes());

                doNothing().when(dynamicUploadService).uploadDynamicExcel(any(), eq("my_table"), eq("my_uploader"));

                // When & Then
                mockMvc.perform(multipart("/api/excel/dynamic/upload")
                                .file(file)
                                .param("targetTableName", "my_table")
                                .param("uploaderId", "my_uploader"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message")
                                                .value("Successfully uploaded file 'data.xlsx' into table 'my_table'"));
        }

        @Test
        @DisplayName("동적 업로드 시 엑셀이 아닌 파일은 400 Bad Request를 반환한다")
        void uploadDynamicFile_ShouldReturn400_WhenFileIsNotExcel() throws Exception {
                // Given
                MockMultipartFile txtFile = new MockMultipartFile(
                                "file", "test.txt", "text/plain", "not excel".getBytes());

                // When & Then
                mockMvc.perform(multipart("/api/excel/dynamic/upload")
                                .file(txtFile)
                                .param("targetTableName", "my_table")
                                .param("uploaderId", "my_uploader"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value(ErrorCode.EXCEL_FILE_NOT_FOUND.getMessage()));
        }
}
