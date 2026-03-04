# 엑셀 대용량 업로드 서비스

## 1. 프로젝트 개요

Spring Boot 기반의 **범용 엑셀 대용량 업로드 서비스**입니다.
특정 Entity나 테이블에 종속되지 않고, 엑셀 헤더를 그대로 DB 컬럼명으로 매핑하는 **동적 업로드** 방식으로
다양한 테이블에 데이터를 유연하게 업로드할 수 있습니다.

## 2. 주요 기능 및 아키텍처

- **범용 동적 업로드**: 엑셀 헤더를 그대로 컬럼명으로 사용하는 `DynamicExcelParser` + `NamedParameterJdbcTemplate` 기반의 JDBC 배치 Insert. 특정 Entity/DTO에 종속되지 않음
- **확장 가능한 파일 저장소**: `FileStorageService` 인터페이스 기반으로 로컬, S3 등 저장소 구현체를 설정만으로 전환 가능
- **업로드 이력 추적**: `UploadHistory` 엔티티에 처리 상태, 업로더 ID, 대상 테이블, 실패 원인 메시지를 기록
- **전역 예외 처리**: `GlobalExceptionHandler`를 통해 표준화된 에러 응답 포맷 제공

## 3. API 명세

### 기본 URL: `/api/excel`

| Method | Endpoint | 설명 | 응답 |
| :--- | :--- | :--- | :--- |
| `POST` | `/dynamic/upload` | 범용 동적 엑셀 업로드. 헤더를 테이블 컬럼으로 매핑하여 지정한 테이블에 insert | `200 OK` |

### 요청 파라미터

**POST `/dynamic/upload`**

| 파라미터 | 타입 | 필수 | 설명 |
| :--- | :--- | :--- | :--- |
| `file` | MultipartFile | O | 업로드할 엑셀 파일 (.xlsx) |
| `targetTableName` | String | O | 데이터를 저장할 대상 테이블명 |
| `uploaderId` | String | X | 업로드 요청자 ID (기본값: `system`) |

---

## 4. 엑셀 파일 형식 규칙

`DynamicExcelParser`가 첫 번째 행의 헤더를 그대로 대상 테이블의 컬럼명으로 사용합니다.
**엑셀 헤더 이름이 DB 컬럼명과 일치해야 합니다.**

```
name | category | price | stock_quantity | description
-----|----------|-------|----------------|------------
...  | ...      | ...   | ...            | ...
```

- 완전히 비어있는 행은 자동으로 건너뜁니다.
- 엑셀 헤더와 DB 컬럼명이 일치하면 어떤 테이블에도 업로드 가능합니다.

---

## 5. 업로드 이력 (UploadHistory)

파일 업로드 처리 내역은 `upload_history` 테이블에 자동으로 기록됩니다.

| 컬럼 | 설명 |
| :--- | :--- |
| `file_name` | 업로드된 원본 파일명 |
| `target_table_name` | 데이터가 저장된 대상 테이블명 |
| `uploader_id` | 업로드 요청자 ID |
| `status` | 처리 상태 (`PENDING`, `IN_PROGRESS`, `COMPLETED`, `FAILED`) |
| `total_records` | 전체 처리 행 수 |
| `success_count` | 성공 처리 행 수 |
| `failure_count` | 실패 처리 행 수 |
| `error_message` | 실패 시 오류 원인 메시지 (최대 1000자) |
| `uploaded_at` | 업로드 시작 시각 |
| `completed_at` | 처리 완료 시각 |

---

## 6. 환경 설정 가이드 (Configuration)

`application.yml`을 통해 파일 저장소 위치 및 유형을 코드 수정 없이 변경할 수 있습니다.

### 설정 파일 예시

```yaml
file:
  type: LOCAL  # 사용할 저장소 유형 (LOCAL, S3)

  # 로컬 저장소 설정 (type: LOCAL 일 때 사용)
  local:
    upload-dir: upload-dir  # 파일이 저장될 로컬 디렉토리 경로

  # S3 저장소 설정 (type: S3 일 때 사용 - 구현 필요)
  s3:
    bucket: my-excel-bucket
    region: ap-northeast-2
    access-key: YOUR_ACCESS_KEY
    secret-key: YOUR_SECRET_KEY
```

### 설정 항목 상세

| 프로퍼티 경로 | 설명 | 기본값 |
| :--- | :--- | :--- |
| `file.type` | 사용할 스토리지 구현체 선택 | `LOCAL` |
| `file.local.upload-dir` | 로컬 파일 저장 경로 | `upload-dir` |
| `file.s3.bucket` | AWS S3 버킷명 | - |
| `file.s3.region` | AWS S3 리전 | - |

---

## 7. 파일 저장소 확장 가이드

현재 `LocalFileStorageService`가 기본 구현체로 제공됩니다. AWS S3 등으로 확장하려면 아래 단계를 따릅니다.

1. **구현체 생성**: `FileStorageService` 인터페이스를 구현하는 `S3FileStorageService` 클래스를 생성합니다.
2. **조건부 빈 등록**: `@ConditionalOnProperty(prefix = "file", name = "type", havingValue = "S3")` 어노테이션을 추가합니다.
3. **설정값 주입**: `FileStorageProperties`를 주입받아 `properties.getS3().getBucket()` 형태로 설정값을 사용합니다.
4. **설정 변경**: `application.yml`에서 `file.type`을 `S3`로 변경합니다.
