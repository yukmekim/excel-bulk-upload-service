# 엑셀 대용량 업로드 서비스 가이드

## 1. 프로젝트 개요

Spring Boot와 Spring Batch를 기반으로 구축된 대용량 엑셀 파일 업로드 및 처리 서비스입니다.
특정 엔티티나 테이블에 종속되지 않는 범용 동적 업로드와,
Spring Batch Chunk 처리 기반의 고성능 배치 업로드 두 가지 방식을 제공합니다.

## 2. 주요 기능 및 아키텍처

- **범용 동적 업로드**: 엑셀 헤더를 그대로 컬럼명으로 사용하는 `DynamicExcelParser` + `NamedParameterJdbcTemplate` 기반의 JDBC 배치 insert. 특정 Entity/DTO에 종속되지 않음
- **Spring Batch 업로드**: Chunk 지향 처리(Reader → Processor → Writer)로 대용량 파일을 안정적으로 처리. `saveAll()`로 Chunk 단위 DB 쿼리 최소화
- **비동기 실행**: 배치 Job이 백그라운드에서 실행되어 업로드 요청에 즉시 응답
- **확장 가능한 파일 저장소**: `FileStorageService` 인터페이스 기반으로 로컬, S3 등 저장소 구현체를 설정만으로 전환 가능
- **임시 파일 자동 삭제**: `JobExecutionListener`를 통해 배치 Job 완료(성공/실패) 후 업로드된 임시 파일이 자동으로 삭제됨
- **업로드 이력 추적**: `UploadHistory` 엔티티에 처리 상태, 업로더 ID, 대상 테이블, 실패 원인 메시지를 기록
- **전역 예외 처리**: `GlobalExceptionHandler`를 통해 표준화된 에러 응답 포맷 제공

## 3. API 명세

### 기본 URL: `/api/excel`

| Method | Endpoint | 설명 | 응답 |
| :--- | :--- | :--- | :--- |
| `POST` | `/dynamic/upload` | 범용 동적 엑셀 업로드. 헤더를 테이블 컬럼으로 매핑하여 지정한 테이블에 insert | `200 OK` |
| `POST` | `/upload-batch` | Spring Batch 기반 비동기 엑셀 업로드 | `202 Accepted` |

### 요청 파라미터

**POST `/dynamic/upload`**

| 파라미터 | 타입 | 필수 | 설명 |
| :--- | :--- | :--- | :--- |
| `file` | MultipartFile | O | 업로드할 엑셀 파일 (.xlsx) |
| `targetTableName` | String | O | 데이터를 저장할 대상 테이블명 |
| `uploaderId` | String | X | 업로드 요청자 ID (기본값: `system`) |

**POST `/upload-batch`**

| 파라미터 | 타입 | 필수 | 설명 |
| :--- | :--- | :--- | :--- |
| `file` | MultipartFile | O | 업로드할 엑셀 파일 (.xlsx) |

---

## 4. 엑셀 파일 형식 규칙

업로드 방식에 따라 헤더 규칙이 다릅니다.

### 4.1. 동적 업로드 (`/dynamic/upload`)

`DynamicExcelParser`가 첫 번째 행의 헤더를 그대로 대상 테이블의 컬럼명으로 사용합니다.
엑셀 헤더 이름이 DB 컬럼명과 일치해야 합니다.

```
name | category | price | stock_quantity | description
-----|----------|-------|----------------|------------
...  | ...      | ...   | ...            | ...
```

- 완전히 비어있는 행은 자동으로 건너뜁니다.

### 4.2. 배치 업로드 (`/upload-batch`)

`ExcelItemReader`가 헤더 이름을 기반으로 데이터를 매핑합니다.
컬럼 순서가 달라도 헤더 이름이 같으면 정상 동작합니다.

| 헤더 이름 | 타입 | 설명 |
| :--- | :--- | :--- |
| `Name` | String | 상품명 (필수) |
| `Category` | String | 카테고리 |
| `Price` | BigDecimal | 가격 |
| `Stock Quantity` | Integer | 재고 수량 |
| `Description` | String | 상품 설명 |

- `Name` 컬럼이 비어있는 행은 자동으로 건너뜁니다.
- 파싱 오류가 발생한 행은 경고 로그를 남기고 건너뜁니다. (배치 전체가 중단되지 않음)

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

## 7. 배치 업로드 필드 추가 가이드

`/upload-batch` 경로에 새로운 컬럼을 추가하려면 아래 파일을 순서대로 수정합니다.

1. **Entity**: `Product.java`에 필드 추가
2. **DTO**: `ProductUploadDto.java`에 필드 추가
3. **Reader**: `ExcelItemReader.java`의 `read()` 메서드에서 `getCellStringValue(row, "NewHeader")` 로직 추가

> `/dynamic/upload`는 엑셀 헤더를 그대로 사용하므로 코드 수정이 필요 없습니다.

---

## 8. 파일 저장소 확장 가이드

현재 `LocalFileStorageService`가 기본 구현체로 제공됩니다. AWS S3 등으로 확장하려면 아래 단계를 따릅니다.

1. **구현체 생성**: `FileStorageService` 인터페이스를 구현하는 `S3FileStorageService` 클래스를 생성합니다.
2. **조건부 빈 등록**: `@ConditionalOnProperty(prefix = "file", name = "type", havingValue = "S3")` 어노테이션을 추가합니다.
3. **설정값 주입**: `FileStorageProperties`를 주입받아 `properties.getS3().getBucket()` 형태로 설정값을 사용합니다.
4. **설정 변경**: `application.yml`에서 `file.type`을 `S3`로 변경합니다.

---

## 9. 성능 튜닝

`BatchConfig.java`의 chunk size 값을 조절하여 트랜잭션 커밋 단위를 변경할 수 있습니다.

```java
.<ProductUploadDto, Product>chunk(100, transactionManager)  // 기본값: 100
```

- Chunk 단위로 `saveAll()`이 호출되어 DB 배치 insert가 수행됩니다.
- 파일 크기 및 서버 메모리에 따라 50~500 사이에서 조정을 권장합니다.
