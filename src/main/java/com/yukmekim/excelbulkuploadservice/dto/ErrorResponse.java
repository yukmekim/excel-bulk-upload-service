package com.yukmekim.excelbulkuploadservice.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

import com.yukmekim.excelbulkuploadservice.common.exception.ErrorCode;

@Getter
@Builder
public class ErrorResponse {

    private final String error;
    private final String message;
    private final int status;

    public static ResponseEntity<ErrorResponse> toResponseEntity(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.builder()
                        .status(errorCode.getStatus().value())
                        .error(errorCode.getStatus().name())
                        .message(errorCode.getMessage())
                        .build());
    }

    public static ResponseEntity<ErrorResponse> toResponseEntity(ErrorCode errorCode, String detail) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.builder()
                        .status(errorCode.getStatus().value())
                        .error(errorCode.getStatus().name())
                        .message(errorCode.getMessage() + ": " + detail)
                        .build());
    }
}
