package com.yukmekim.excelbulkuploadservice.util;

import com.yukmekim.excelbulkuploadservice.dto.ProductUploadDto;
import com.yukmekim.excelbulkuploadservice.entity.Product;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExcelHelper {

    public static String TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    static String[] HEADERs = { "Id", "Name", "Category", "Price", "Stock Quantity", "Description" };
    static String SHEET = "Products";

    public static boolean hasExcelFormat(MultipartFile file) {
        if (!TYPE.equals(file.getContentType())) {
            return false;
        }
        return true;
    }

    public static ByteArrayInputStream productsToExcel(List<Product> products) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream();) {
            Sheet sheet = workbook.createSheet(SHEET);

            // Header
            Row headerRow = sheet.createRow(0);

            for (int col = 0; col < HEADERs.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(HEADERs[col]);
            }

            int rowIdx = 1;
            for (Product product : products) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(product.getId());
                row.createCell(1).setCellValue(product.getName());
                row.createCell(2).setCellValue(product.getCategory());
                row.createCell(3).setCellValue(product.getPrice().doubleValue());
                row.createCell(4).setCellValue(product.getStockQuantity());
                row.createCell(5).setCellValue(product.getDescription());
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("fail to import data to Excel file: " + e.getMessage());
        }
    }

    public static List<ProductUploadDto> excelToProducts(InputStream is) {
        try (Workbook workbook = new XSSFWorkbook(is)) {
            // Try to find sheet by name "Products", otherwise use the first one
            Sheet sheet = workbook.getSheet(SHEET);
            if (sheet == null) {
                if (workbook.getNumberOfSheets() > 0) {
                    sheet = workbook.getSheetAt(0);
                } else {
                    return new ArrayList<>(); // workbook은 try-with-resources가 자동 close
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

            return products; // workbook은 try-with-resources가 자동 close
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
            default:
                return null;
        }
    }

    private static BigDecimal getCellValueAsBigDecimal(Cell cell) {
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

    private static Integer getCellValueAsInteger(Cell cell) {
        if (cell == null)
            return 0;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                // Handle cases like "10.0" -> 10
                return Double.valueOf(cell.getStringCellValue()).intValue();
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }
}
