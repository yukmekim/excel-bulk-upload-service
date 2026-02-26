package com.yukmekim.excelbulkuploadservice.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DynamicExcelParser {

    /**
     * Parse any Excel file into a List of Maps.
     * The first row is used as the header (map keys).
     *
     * @param is The input stream of the Excel file
     * @return List of key-value maps representing each row
     */
    public static List<Map<String, Object>> parseExcelToMap(InputStream is) {
        try (Workbook workbook = new XSSFWorkbook(is)) {
            // Read the first sheet
            if (workbook.getNumberOfSheets() == 0) {
                return new ArrayList<>();
            }
            Sheet sheet = workbook.getSheetAt(0);

            List<Map<String, Object>> result = new ArrayList<>();
            List<String> headers = new ArrayList<>();

            int rowIndex = 0;
            for (Row row : sheet) {
                // Parse headers from the first row
                if (rowIndex == 0) {
                    for (Cell cell : row) {
                        headers.add(getCellValue(cell).toString().trim());
                    }
                    rowIndex++;
                    continue;
                }

                // Parse data rows
                Map<String, Object> rowMap = new LinkedHashMap<>();
                boolean isEmptyRow = true;

                for (int colIndex = 0; colIndex < headers.size(); colIndex++) {
                    Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    Object value = getCellValue(cell);
                    
                    if (value != null && !value.toString().trim().isEmpty()) {
                        isEmptyRow = false;
                    }
                    
                    rowMap.put(headers.get(colIndex), value);
                }

                // Skip completely empty rows
                if (!isEmptyRow) {
                    result.add(rowMap);
                }
                rowIndex++;
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse Excel file dynamically: " + e.getMessage(), e);
        }
    }

    private static Object getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue();
                } else {
                    double doubleVal = cell.getNumericCellValue();
                    // If it's an integer value, return as Long to avoid unnecessary decimals like "10.0"
                    if (doubleVal == Math.floor(doubleVal) && !Double.isInfinite(doubleVal)) {
                        yield (long) doubleVal; // Or Integer depending on size, Long is safer
                    }
                    yield doubleVal;
                }
            }
            case BOOLEAN -> cell.getBooleanCellValue();
            case FORMULA -> cell.getCellFormula();
            default -> null;
        };
    }
}
