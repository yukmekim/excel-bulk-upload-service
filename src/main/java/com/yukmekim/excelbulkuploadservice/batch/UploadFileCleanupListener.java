package com.yukmekim.excelbulkuploadservice.batch;

import com.yukmekim.excelbulkuploadservice.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

/**
 * Spring Batch Job 완료 후 업로드된 임시 엑셀 파일을 삭제하는 리스너.
 * Job 성공/실패 여부와 관계없이 afterJob()이 항상 호출되어 파일 누적을 방지한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UploadFileCleanupListener implements JobExecutionListener {

    private final FileStorageService fileStorageService;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        // No-op
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String filePath = jobExecution.getJobParameters().getString("filePath");

        if (filePath == null || filePath.isBlank()) {
            log.warn("No filePath found in job parameters. Skipping cleanup.");
            return;
        }

        try {
            fileStorageService.delete(filePath);
            log.info("Cleaned up uploaded temp file after batch job: {}", filePath);
        } catch (Exception e) {
            // 파일 삭제 실패는 치명적이지 않으므로 경고 로그만 남기고 계속
            log.warn("Failed to clean up temp file: {}. Reason: {}", filePath, e.getMessage());
        }
    }
}
