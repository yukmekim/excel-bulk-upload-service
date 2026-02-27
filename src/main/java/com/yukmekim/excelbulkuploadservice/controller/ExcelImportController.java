package com.yukmekim.excelbulkuploadservice.controller;

import com.yukmekim.excelbulkuploadservice.common.exception.BusinessException;
import com.yukmekim.excelbulkuploadservice.common.exception.ErrorCode;
import com.yukmekim.excelbulkuploadservice.dto.ResponseMessage;
import com.yukmekim.excelbulkuploadservice.service.DynamicUploadService;
import com.yukmekim.excelbulkuploadservice.service.ProductBatchService;
import com.yukmekim.excelbulkuploadservice.util.ExcelHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobExecution;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/excel")
@RequiredArgsConstructor
public class ExcelImportController {

    private final ProductBatchService productBatchService;
    private final DynamicUploadService dynamicUploadService;

    /**
     * 범용 엑셀 업로드 엔드포인트
     * 
     * 파라미터를 통해 대상 테이블(targetTableName)과 업로더 식별자(uploaderId)를 동적으로 지정받아 처리합니다.
     * 특정 Entity나 DTO에 종속되지 않습니다.
     */
    @PostMapping("/dynamic/upload")
    public ResponseEntity<ResponseMessage> uploadDynamicFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("targetTableName") String targetTableName,
            @RequestParam(value = "uploaderId", defaultValue = "system") String uploaderId) {

        // 기본적인 엑셀 파일 형식인지 검증
        if (!ExcelHelper.hasExcelFormat(file)) {
            throw new BusinessException(ErrorCode.EXCEL_FILE_NOT_FOUND);
        }

        try {
            // 동적 업로드 서비스 호출
            dynamicUploadService.uploadDynamicExcel(file, targetTableName, uploaderId);
            
            String message = String.format("Successfully uploaded file '%s' into table '%s'", 
                                           file.getOriginalFilename(), targetTableName);
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message));
            
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EXCEL_FILE_UPLOAD_ERROR, e.getMessage(), e);
        }
    }

    /**
     * 비동기 배치 방식의 엑셀 업로드 엔드포인트
     */
    @PostMapping("/upload-batch")
    public ResponseEntity<ResponseMessage> uploadFileBatch(@RequestParam("file") MultipartFile file) {
        if (!ExcelHelper.hasExcelFormat(file)) {
            throw new BusinessException(ErrorCode.EXCEL_FILE_NOT_FOUND);
        }

        try {
            JobExecution execution = productBatchService.runJob(file);
            String message = "Batch Job started successfully. Job ID: " + execution.getJobId() + ", Status: "
                    + execution.getStatus();
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ResponseMessage(message));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EXCEL_JOB_START_ERROR, e.getMessage(), e);
        }
    }
}
