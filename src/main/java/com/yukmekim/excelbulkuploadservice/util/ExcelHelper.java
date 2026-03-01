package com.yukmekim.excelbulkuploadservice.util;

import org.springframework.web.multipart.MultipartFile;

public class ExcelHelper {

    public static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    public static boolean hasExcelFormat(MultipartFile file) {
        return EXCEL_CONTENT_TYPE.equals(file.getContentType());
    }
}
