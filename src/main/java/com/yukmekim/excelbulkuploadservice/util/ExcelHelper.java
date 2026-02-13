package com.yukmekim.excelbulkuploadservice.util;

import com.yukmekim.excelbulkuploadservice.dto.ProductUploadDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExcelHelper {
    public static String TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    static String[] HEADERS = { "Name", "Category", "Price", "Stock Quantity", "Description" };
    static String SHEET = "Products";

    public static boolean hasExcelFormat(MultipartFile file) {
        if (!TYPE.equals(file.getContentType())) {
            return false;
        }
        return true;
    }

    public static List<ProductUploadDto> excelToProducts(InputStream is) {
        try {
            Workbook workbook = new XSSFWorkbook(is);
            // Try to find sheet by name "Products", otherwise use the first one
            Sheet sheet = workbook.getSheet(SHEET);
            if (sheet == null) {
                if (workbook.getNumberOfSheets() > 0) {
                    sheet = workbook.getSheetAt(0);
                } else {
                    return new ArrayList<>();
                }
            }

            Iterator<Row> rows = sheet.iterator();
            List<ProductUploadDto> products = new ArrayList<>();

            int rowNumber = 0;
            while (rows.hasNext()) {
                Row currentRow = rows.next();

                // Skip header row
                if (rowNumber == 0) {
                    rowNumber++;
                    continue;
                }

                // Get Name cell - essential for a valid product
                Cell nameCell = currentRow.getCell(0);
                String name = getCellValueAsString(nameCell);

                // Skip empty rows or rows without a name
                if (name == null || name.trim().isEmpty()) {
                    continue;
                }

                ProductUploadDto product = ProductUploadDto.builder()
                        .name(name)
                        .category(getCellValueAsString(currentRow.getCell(1)))
                        .price(getCellValueAsBigDecimal(currentRow.getCell(2)))
                        .stockQuantity(getCellValueAsInteger(currentRow.getCell(3)))
                        .description(getCellValueAsString(currentRow.getCell(4)))
                        .build();

                products.add(product);
                rowNumber++;
            }

            workbook.close();
            return products;
        } catch (IOException e) {
            throw new RuntimeException("fail to parse Excel file: " + e.getMessage());
        }
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null)
            return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    private static BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null)
            return BigDecimal.ZERO;

        switch (cell.getCellType()) {
            case NUMERIC:
                return BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING:
                try {
                    String val = cell.getStringCellValue();
                    if (val == null || val.trim().isEmpty())
                        return BigDecimal.ZERO;
                    return new BigDecimal(val);
                } catch (NumberFormatException e) {
                    return BigDecimal.ZERO;
                }
            default:
                return BigDecimal.ZERO;
        }
    }

    private static Integer getCellValueAsInteger(Cell cell) {
        if (cell == null)
            return 0;

        switch (cell.getCellType()) {
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            case STRING:
                try {
                    String val = cell.getStringCellValue();
                    if (val == null || val.trim().isEmpty())
                        return 0;
                    return Integer.parseInt(val);
                } catch (NumberFormatException e) {
                    return 0;
                }
            default:
                return 0;
        }
    }
}
