package com.yukmekim.excelbulkuploadservice.batch;

import com.yukmekim.excelbulkuploadservice.dto.ProductUploadDto;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.item.ExecutionContext;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExcelItemReaderTest {

    @TempDir
    Path tempDir;

    /**
     * 테스트용 엑셀 파일을 생성하고 절대 경로를 반환한다.
     */
    private String createExcelFile(String filename, boolean withHeader, Object[][] data) throws Exception {
        File file = tempDir.resolve(filename).toFile();
        try (Workbook workbook = new XSSFWorkbook();
                FileOutputStream fos = new FileOutputStream(file)) {

            Sheet sheet = workbook.createSheet("Products");
            int rowIndex = 0;

            if (withHeader) {
                Row header = sheet.createRow(rowIndex++);
                header.createCell(0).setCellValue("Name");
                header.createCell(1).setCellValue("Category");
                header.createCell(2).setCellValue("Price");
                header.createCell(3).setCellValue("Stock Quantity");
                header.createCell(4).setCellValue("Description");
            }

            for (Object[] rowData : data) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < rowData.length; i++) {
                    if (rowData[i] instanceof String s) {
                        row.createCell(i).setCellValue(s);
                    } else if (rowData[i] instanceof Number n) {
                        row.createCell(i).setCellValue(n.doubleValue());
                    }
                }
            }
            workbook.write(fos);
        }
        return file.getAbsolutePath();
    }

    @Test
    @DisplayName("헤더 기반으로 데이터를 올바르게 파싱한다")
    void read_ShouldParseDataCorrectly_WhenHeaderExists() throws Exception {
        // Given
        String filePath = createExcelFile("standard.xlsx", true, new Object[][] {
                { "Product A", "Electronics", 99.99, 100, "Description A" },
                { "Product B", "Books", 29.50, 50, "Description B" }
        });

        ExcelItemReader reader = new ExcelItemReader(filePath);
        reader.open(new ExecutionContext());

        // When
        ProductUploadDto first = reader.read();
        ProductUploadDto second = reader.read();
        ProductUploadDto end = reader.read(); // 파일 끝

        reader.close();

        // Then
        assertThat(first).isNotNull();
        assertThat(first.getName()).isEqualTo("Product A");
        assertThat(first.getCategory()).isEqualTo("Electronics");
        assertThat(first.getPrice()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(first.getStockQuantity()).isEqualTo(100);

        assertThat(second).isNotNull();
        assertThat(second.getName()).isEqualTo("Product B");
        assertThat(second.getPrice()).isEqualByComparingTo(new BigDecimal("29.5"));

        assertThat(end).isNull(); // 더 이상 데이터 없음
    }

    @Test
    @DisplayName("컬럼 순서가 달라도 헤더 이름 기반으로 올바르게 파싱한다")
    void read_ShouldParseCorrectly_WhenColumnOrderIsChanged() throws Exception {
        // Given: Price - Name - Category - Description - Stock Quantity 순서로 헤더 작성
        File file = tempDir.resolve("reordered.xlsx").toFile();
        try (Workbook workbook = new XSSFWorkbook();
                FileOutputStream fos = new FileOutputStream(file)) {
            Sheet sheet = workbook.createSheet("Products");

            // 헤더 순서 변경
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Price");
            header.createCell(1).setCellValue("Name");
            header.createCell(2).setCellValue("Category");
            header.createCell(3).setCellValue("Description");
            header.createCell(4).setCellValue("Stock Quantity");

            // 데이터
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(55.00);
            row.createCell(1).setCellValue("Reordered Product");
            row.createCell(2).setCellValue("Toys");
            row.createCell(3).setCellValue("Toy description");
            row.createCell(4).setCellValue(30);

            workbook.write(fos);
        }

        ExcelItemReader reader = new ExcelItemReader(file.getAbsolutePath());
        reader.open(new ExecutionContext());

        // When
        ProductUploadDto result = reader.read();
        reader.close();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Reordered Product");
        assertThat(result.getCategory()).isEqualTo("Toys");
        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("55.0"));
        assertThat(result.getStockQuantity()).isEqualTo(30);
    }

    @Test
    @DisplayName("Name이 비어있는 행은 건너뛴다")
    void read_ShouldSkipRow_WhenNameIsEmpty() throws Exception {
        // Given: 첫 번째 데이터 행은 Name 없음, 두 번째 행은 유효
        String filePath = createExcelFile("skip_empty_name.xlsx", true, new Object[][] {
                { "", "Category", 10.0, 5, "Desc" }, // Name 없음 -> 건너뜀
                { "Valid Product", "Category", 20.0, 10, "Desc" } // 유효
        });

        ExcelItemReader reader = new ExcelItemReader(filePath);
        reader.open(new ExecutionContext());

        // When
        ProductUploadDto result = reader.read();
        reader.close();

        // Then: Name이 있는 두 번째 행이 첫 번째 결과로 반환되어야 함
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Valid Product");
    }

    @Test
    @DisplayName("존재하지 않는 파일 경로로 open 시 ItemStreamException이 발생한다")
    void open_ShouldThrowItemStreamException_WhenFileNotFound() {
        // Given
        ExcelItemReader reader = new ExcelItemReader("/nonexistent/path/file.xlsx");

        // When & Then
        assertThatThrownBy(() -> reader.open(new ExecutionContext()))
                .isInstanceOf(org.springframework.batch.item.ItemStreamException.class);
    }
}
