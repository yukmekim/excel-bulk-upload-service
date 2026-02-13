package com.yukmekim.excelbulkuploadservice.util;

import com.yukmekim.excelbulkuploadservice.dto.ProductUploadDto;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

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

    @Test
    void excelToProducts_ShouldParseExcelFileCorrectly() throws IOException {
        // Arrange
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Products");

        // Header
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Name");
        headerRow.createCell(1).setCellValue("Category");
        headerRow.createCell(2).setCellValue("Price");
        headerRow.createCell(3).setCellValue("Stock Quantity");
        headerRow.createCell(4).setCellValue("Description");

        // Data Row 1
        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("Test Product 1");
        row1.createCell(1).setCellValue("Electronics");
        row1.createCell(2).setCellValue(100.50);
        row1.createCell(3).setCellValue(10);
        row1.createCell(4).setCellValue("Description 1");

        // Data Row 2 (Numeric values as String)
        Row row2 = sheet.createRow(2);
        row2.createCell(0).setCellValue("Test Product 2");
        row2.createCell(1).setCellValue("Books");
        row2.createCell(2).setCellValue("20.00");
        row2.createCell(3).setCellValue("5");
        row2.createCell(4).setCellValue("Description 2");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();
        ByteArrayInputStream is = new ByteArrayInputStream(bos.toByteArray());

        // Act
        List<ProductUploadDto> products = ExcelHelper.excelToProducts(is);

        // Assert
        assertEquals(2, products.size());

        ProductUploadDto p1 = products.get(0);
        assertEquals("Test Product 1", p1.getName());
        assertEquals("Electronics", p1.getCategory());
        assertEquals(new BigDecimal("100.5"), p1.getPrice());
        assertEquals(10, p1.getStockQuantity());
        assertEquals("Description 1", p1.getDescription());

        ProductUploadDto p2 = products.get(1);
        assertEquals("Test Product 2", p2.getName());
        assertEquals(new BigDecimal("20.00"), p2.getPrice());
        assertEquals(5, p2.getStockQuantity());
    }
}
