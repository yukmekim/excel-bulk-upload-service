package com.yukmekim.excelbulkuploadservice.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error"),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "Invalid Input Value"),

    // Excel
    EXCEL_FILE_UPLOAD_ERROR(HttpStatus.EXPECTATION_FAILED, "Could not upload the file"),
    EXCEL_FILE_PARSE_ERROR(HttpStatus.BAD_REQUEST, "Fail to parse Excel file"),
    EXCEL_FILE_NOT_FOUND(HttpStatus.BAD_REQUEST, "Please upload an excel file!"),
    EXCEL_JOB_START_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Could not start batch job"),

    // Product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "Product not found");

    private final HttpStatus status;
    private final String message;
}
