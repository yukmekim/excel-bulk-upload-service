package com.yukmekim.excelbulkuploadservice.batch;

import com.yukmekim.excelbulkuploadservice.dto.ProductUploadDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ExcelItemReader implements ItemStreamReader<ProductUploadDto> {

    private final String filePath;
    private Workbook workbook;
    private Sheet sheet;
    private int currentRowIndex = 0;

    // Header Mapping: Column Name -> Index
    private Map<String, Integer> headerMap = new HashMap<>();

    public ExcelItemReader(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new ItemStreamException("Excel file not found: " + filePath);
        }

        // try-with-resources로 FileInputStream이 Workbook 생성 후 항상 close되도록 보장
        try (FileInputStream fis = new FileInputStream(file)) {
            workbook = WorkbookFactory.create(fis);
            sheet = workbook.getSheetAt(0);

            // Parse Header (First Row)
            Row headerRow = sheet.getRow(0);
            if (headerRow != null) {
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    Cell cell = headerRow.getCell(i);
                    if (cell != null) {
                        String headerName = cell.toString().trim();
                        headerMap.put(headerName, i);
                    }
                }
                log.info("Parsed Excel Headers: {}", headerMap);
            }

            // Data starts from index 1 (skip header)
            currentRowIndex = 1;

        } catch (IOException e) {
            throw new ItemStreamException("Failed to open Excel file", e);
        }
    }

    @Override
    public ProductUploadDto read()
            throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (sheet == null) {
            return null;
        }

        if (currentRowIndex > sheet.getLastRowNum()) {
            return null; // End of file
        }

        Row row = sheet.getRow(currentRowIndex);
        currentRowIndex++;

        if (row == null) {
            return read(); // Skip empty row
        }

        try {
            // Get values using header names
            String name = getCellStringValue(row, "Name");
            // If mandatory field 'Name' is entirely missing, skip row
            if (name == null || name.trim().isEmpty()) {
                return read();
            }

            String category = getCellStringValue(row, "Category");
            BigDecimal price = getCellNumericValue(row, "Price");
            int stockQuantity = getCellIntValue(row, "Stock Quantity");
            String description = getCellStringValue(row, "Description");

            return ProductUploadDto.builder()
                    .name(name)
                    .category(category)
                    .price(price)
                    .stockQuantity(stockQuantity)
                    .description(description)
                    .build();
        } catch (Exception e) {
            log.warn("Skipping row {} due to parsing error: {}", currentRowIndex, e.getMessage());
            return read();
        }
    }

    private String getCellStringValue(Row row, String headerName) {
        Integer index = headerMap.get(headerName);
        if (index == null)
            return null;

        Cell cell = row.getCell(index);
        if (cell == null)
            return null;

        return cell.toString();
    }

    private BigDecimal getCellNumericValue(Row row, String headerName) {
        Integer index = headerMap.get(headerName);
        if (index == null)
            return BigDecimal.ZERO;

        Cell cell = row.getCell(index);
        if (cell == null)
            return BigDecimal.ZERO;

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            } else {
                String val = cell.toString().trim();
                return val.isEmpty() ? BigDecimal.ZERO : new BigDecimal(val);
            }
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private int getCellIntValue(Row row, String headerName) {
        Integer index = headerMap.get(headerName);
        if (index == null)
            return 0;

        Cell cell = row.getCell(index);
        if (cell == null)
            return 0;

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            } else {
                String val = cell.toString().trim();
                if (val.isEmpty())
                    return 0;
                double d = Double.parseDouble(val);
                return (int) d;
            }
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        // No-op
    }

    @Override
    public void close() throws ItemStreamException {
        if (workbook != null) {
            try {
                workbook.close();
            } catch (IOException e) {
                throw new ItemStreamException("Error closing workbook", e);
            }
        }
    }
}
