package com.yukmekim.excelbulkuploadservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "upload_history")
public class UploadHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UploadStatus status;

    private Integer totalRecords;
    private Integer successCount;
    private Integer failureCount;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime uploadedAt;

    private LocalDateTime completedAt;

    public void startProcessing() {
        this.status = UploadStatus.IN_PROGRESS;
    }

    public void complete(int total, int success, int failure) {
        this.status = UploadStatus.COMPLETED;
        this.totalRecords = total;
        this.successCount = success;
        this.failureCount = failure;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(String errorDetails) { // Could expand to store error details later
        this.status = UploadStatus.FAILED;
        this.completedAt = LocalDateTime.now();
    }
}
