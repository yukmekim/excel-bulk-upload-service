package com.yukmekim.excelbulkuploadservice.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class ExcelHelperTest {

    @Test
    void hasExcelFormat_ShouldReturnTrue_WhenTypeIsXlsx() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "content".getBytes());

        assertTrue(ExcelHelper.hasExcelFormat(file));
    }

    @Test
    void hasExcelFormat_ShouldReturnFalse_WhenTypeIsNotXlsx() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "content".getBytes());

        assertFalse(ExcelHelper.hasExcelFormat(file));
    }
}
