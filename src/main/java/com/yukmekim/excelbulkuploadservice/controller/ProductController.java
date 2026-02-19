package com.yukmekim.excelbulkuploadservice.controller;

import com.yukmekim.excelbulkuploadservice.common.exception.BusinessException;
import com.yukmekim.excelbulkuploadservice.common.exception.ErrorCode;
import com.yukmekim.excelbulkuploadservice.dto.ResponseMessage;
import com.yukmekim.excelbulkuploadservice.entity.Product;
import com.yukmekim.excelbulkuploadservice.service.ProductService;
import com.yukmekim.excelbulkuploadservice.util.ExcelHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobExecution;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/excel")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping("/upload")
    public ResponseEntity<ResponseMessage> uploadFile(@RequestParam("file") MultipartFile file) {
        if (!ExcelHelper.hasExcelFormat(file)) {
            throw new BusinessException(ErrorCode.EXCEL_FILE_NOT_FOUND);
        }

        try {
            productService.save(file);
            String message = "Uploaded the file successfully: " + file.getOriginalFilename();
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EXCEL_FILE_UPLOAD_ERROR, e.getMessage());
        }
    }

    @PostMapping("/upload-batch")
    public ResponseEntity<ResponseMessage> uploadFileBatch(@RequestParam("file") MultipartFile file) {
        if (!ExcelHelper.hasExcelFormat(file)) {
            throw new BusinessException(ErrorCode.EXCEL_FILE_NOT_FOUND);
        }

        try {
            JobExecution execution = productService.runJob(file);
            String message = "Batch Job started successfully. Job ID: " + execution.getJobId() + ", Status: "
                    + execution.getStatus();
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ResponseMessage(message));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.EXCEL_JOB_START_ERROR, e.getMessage());
        }
    }

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAllProducts() {
        List<Product> products = productService.getAllProducts();

        if (products.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadExcel() {
        String filename = "products.xlsx";
        InputStreamResource file = new InputStreamResource(productService.load());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }
}
