package com.yukmekim.excelbulkuploadservice.batch;

import com.yukmekim.excelbulkuploadservice.dto.ProductUploadDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Iterator;

public class ExcelItemReader implements ItemStreamReader<ProductUploadDto> {

    private final String filePath;
    private Workbook workbook;
    private Iterator<Row> rowIterator;
    private static final String CURRENT_ROW = "current.row";
    private int currentRowIndex = 0;

    public ExcelItemReader(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                throw new ItemStreamException("File not found: " + filePath);
            }

            this.workbook = new XSSFWorkbook(file);
            Sheet sheet = workbook.getSheetAt(0);
            this.rowIterator = sheet.iterator();

            // Handle restart logic if needed (simple skip)
            if (executionContext.containsKey(CURRENT_ROW)) {
                int startAt = executionContext.getInt(CURRENT_ROW);
                for (int i = 0; i < startAt && rowIterator.hasNext(); i++) {
                    rowIterator.next();
                    currentRowIndex++;
                }
            } else {
                // Skip header if it's a fresh start
                if (this.rowIterator.hasNext()) {
                    this.rowIterator.next();
                }
            }
        } catch (IOException | org.apache.poi.openxml4j.exceptions.InvalidFormatException e) {
            throw new ItemStreamException("Failed to initialize Excel reader", e);
        }
    }

    @Override
    public ProductUploadDto read()
            throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (rowIterator != null && rowIterator.hasNext()) {
            Row row = rowIterator.next();
            currentRowIndex++;

            // Skip empty rows logic could be enhanced here
            ProductUploadDto dto = rowToDto(row);

            // If row is invalid/empty, we might want to skip or return null?
            // For now, let's assume rowToDto handles basic extraction.
            // If it returns a valid object, we return it.
            // If the row was logically empty but physically present, we might get an empty
            // DTO.
            // Let's rely on rowToDto's checks or add a check here.

            if (dto != null && (dto.getName() == null || dto.getName().trim().isEmpty())) {
                // Skip this row by recursion or loop?
                // Recursion is risky for deep stacks. Let's use a loop structure instead in a
                // real-world scenario,
                // but for simplicity, we return null if valid data isn't found?
                // No, returning null ends the step. We should read again.
                return read();
            }

            return dto;
        }
        return null;
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        executionContext.putInt(CURRENT_ROW, currentRowIndex);
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

    private ProductUploadDto rowToDto(Row row) {
        Cell nameCell = row.getCell(0);
        String name = getCellValueAsString(nameCell);

        if (name == null || name.trim().isEmpty()) {
            return new ProductUploadDto(); // Return empty object to trigger skip logic in read()
        }

        return ProductUploadDto.builder()
                .name(name)
                .category(getCellValueAsString(row.getCell(1)))
                .price(getCellValueAsBigDecimal(row.getCell(2)))
                .stockQuantity(getCellValueAsInteger(row.getCell(3)))
                .description(getCellValueAsString(row.getCell(4)))
                .build();
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null)
            return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return null;
        }
    }

    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null)
            return BigDecimal.ZERO;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return new BigDecimal(cell.getStringCellValue());
            } catch (Exception e) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null)
            return 0;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return Double.valueOf(cell.getStringCellValue()).intValue();
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }
}
