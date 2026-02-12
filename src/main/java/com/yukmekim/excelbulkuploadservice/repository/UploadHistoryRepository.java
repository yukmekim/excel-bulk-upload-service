package com.yukmekim.excelbulkuploadservice.repository;

import com.yukmekim.excelbulkuploadservice.entity.UploadHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UploadHistoryRepository extends JpaRepository<UploadHistory, Long> {
    List<UploadHistory> findTop10ByOrderByUploadedAtDesc();
}
