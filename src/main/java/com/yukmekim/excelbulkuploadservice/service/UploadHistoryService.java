package com.yukmekim.excelbulkuploadservice.service;

import com.yukmekim.excelbulkuploadservice.entity.UploadHistory;
import com.yukmekim.excelbulkuploadservice.entity.UploadStatus;
import com.yukmekim.excelbulkuploadservice.repository.UploadHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UploadHistoryService {

    private final UploadHistoryRepository uploadHistoryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UploadHistory createPendingHistory(String fileName, String targetTableName, String uploaderId) {
        UploadHistory history = UploadHistory.builder()
                .fileName(fileName)
                .targetTableName(targetTableName)
                .uploaderId(uploaderId)
                .status(UploadStatus.PENDING)
                .build();
        return uploadHistoryRepository.save(history);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void startProcessing(Long historyId) {
        uploadHistoryRepository.findById(historyId).ifPresent(history -> {
            history.startProcessing();
            uploadHistoryRepository.save(history);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeHistory(Long historyId, int total, int success, int failure) {
        uploadHistoryRepository.findById(historyId).ifPresent(history -> {
            history.complete(total, success, failure);
            uploadHistoryRepository.save(history);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failHistory(Long historyId, String errorMessage) {
        uploadHistoryRepository.findById(historyId).ifPresent(history -> {
            history.fail(errorMessage);
            uploadHistoryRepository.save(history);
        });
    }
}
