package com.yukmekim.excelbulkuploadservice.service;

import com.yukmekim.excelbulkuploadservice.entity.UploadHistory;
import com.yukmekim.excelbulkuploadservice.repository.DynamicUploadRepository;
import com.yukmekim.excelbulkuploadservice.util.DynamicExcelParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicUploadService {

    private final DynamicUploadRepository dynamicUploadRepository;
    private final UploadHistoryService uploadHistoryService;

    /**
     * 동적 엑셀 업로드 처리 메서드
     *
     * @param file 엑셀 파일
     * @param targetTableName 대상 테이블명
     * @param uploaderId 업로더 ID
     */
    @Transactional
    public void uploadDynamicExcel(MultipartFile file, String targetTableName, String uploaderId) {
        // 1. PENDING 상태의 업로드 이력 생성
        UploadHistory history = uploadHistoryService.createPendingHistory(
                file.getOriginalFilename(), targetTableName, uploaderId);
        Long historyId = history.getId();

        try {
            // 2. IN_PROGRESS 상태로 변경
            uploadHistoryService.startProcessing(historyId);

            // 3. 엑셀 파일 파싱 (DynamicExcelParser를 사용해 List<Map> 형태로 파싱)
            List<Map<String, Object>> records = DynamicExcelParser.parseExcelToMap(file.getInputStream());

            if (records.isEmpty()) {
                throw new IllegalArgumentException("업로드된 엑셀 파일에 유효한 데이터가 없습니다.");
            }

            // 4. 추출된 데이터를 바탕으로 대상 테이블에 동적 배치 Insert (DynamicUploadRepository 활용)
            dynamicUploadRepository.batchInsert(targetTableName, records);

            // 5. 성공 시 이력 업데이트 (성공 건수 기록)
            int totalCount = records.size();
            uploadHistoryService.completeHistory(historyId, totalCount, totalCount, 0);

        } catch (IOException e) {
            log.error("엑셀 파일 읽기 오류: {}", e.getMessage(), e);
            uploadHistoryService.failHistory(historyId, "[IOException] " + e.getMessage());
            throw new RuntimeException("엑셀 파일 처리 중 오류가 발생했습니다: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("동적 엑셀 업로드 처리 중 예외 발생: {}", e.getMessage(), e);
            String errorMsg = "[" + e.getClass().getSimpleName() + "] " + e.getMessage();
            uploadHistoryService.failHistory(historyId, errorMsg);
            throw new RuntimeException("동적 엑셀 업로드 중 예기치 않은 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}
