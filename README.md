# 엑셀 대용량 업로드 서비스 가이드

## 1. 프로젝트 개요

Spring Boot와 Spring Batch를 기반으로 구축된 대용량 엑셀 파일 업로드 및 처리 서비스입니다.
대량의 데이터를 효율적으로 처리하기 위해 Chunk 지향 배치 처리 방식을 사용하며,
헤더 이름 기반 매핑을 통해 엑셀 컬럼 순서에 관계없이 유연한 데이터 파싱이 가능합니다.

## 2. 주요 기능 및 아키텍처

- **대용량 처리**: Spring Batch의 Chunk 지향 처리를 통해 데이터를 일정 단위로 나누어 처리. Chunk 단위 `saveAll()` 배치 insert로 DB 쿼리를 최소화
- **비동기 실행**: 배치 Job이 백그라운드에서 실행되어 업로드 요청에 즉시 응답
- **유연한 데이터 매핑**: 엑셀 컬럼 순서에 의존하지 않고 헤더 이름을 기반으로 데이터를 매핑 (Header-Based Mapping)
- **확장 가능한 파일 저장소**: `FileStorageService` 인터페이스 기반으로 로컬, S3 등 저장소 구현체를 설정만으로 전환 가능
- **임시 파일 자동 삭제**: `JobExecutionListener`를 통해 배치 Job 완료(성공/실패) 후 업로드된 임시 파일이 자동으로 삭제됨
- **업로드 이력 추적**: `UploadHistory` 엔티티에 처리 상태, 업로더 ID, 대상 테이블, 실패 원인 메시지를 기록
- **전역 예외 처리**: `GlobalExceptionHandler`를 통해 표준화된 에러 응답 포맷 제공

## 3. API 명세

### 기본 URL: `/api/excel`

| Method | Endpoint | 설명 | 응답 |
| :--- | :--- | :--- | :--- |
| `GET` | `/products` | 저장된 전체 상품 목록 조회 | `200 OK` / `204 No Content` |
| `GET` | `/download` | 상품 목록을 엑셀 파일로 다운로드 | `200 OK` (xlsx 파일) |

> 업로드 엔드포인트(`/upload`, `/upload-batch`)는 별도 동적 업로드 경로(`/api/excel/dynamic/upload`)로 확장
> 하여 `targetTableName`, `uploaderId` 파라미터를 추가로 받도록 구현 예정입니다.

### 업로드 요청 파라미터 (동적 업로드)

| 파라미터 | 타입 | 필수 | 설명 |
| :--- | :--- | :--- | :--- |
| `file` | MultipartFile | O | 업로드할 엑셀 파일 (.xlsx) |
| `targetTableName` | String | O | 데이터를 저장할 대상 테이블명 |
| `uploaderId` | String | O | 업로드 요청자 ID |

---

## 4. 엑셀 파일 형식 규칙 (헤더 기반 매핑)

이 서비스는 엑셀 파일의 첫 번째 행(Header)에 있는 이름을 기준으로 데이터를 읽어옵니다.
**컬럼의 순서가 바뀌어도 헤더 이름만 같다면 정상적으로 동작**합니다.

현재 지원하는 헤더 이름:

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

## 7. 데이터 필드 추가 가이드

새로운 컬럼을 엑셀 업로드에 추가하려면 아래 파일을 순서대로 수정합니다.

1. **Entity**: `Product.java`에 필드 추가
2. **DTO**: `ProductUploadDto.java`에 필드 추가
3. **Reader**: `ExcelItemReader.java`의 `read()` 메소드에서 `getCellStringValue(row, "NewHeader")` 로직 추가
4. **ExcelHelper**: `productsToExcel()`의 `HEADERs` 배열 및 셀 작성 로직 추가

---

## 8. 파일 저장소 확장 가이드

현재 `LocalFileStorageService`가 기본 구현체로 제공됩니다. AWS S3 등으로 확장하려면 아래 단계를 따릅니다.

1. **구현체 생성**: `FileStorageService` 인터페이스를 구현하는 `S3FileStorageService` 클래스를 생성합니다.
2. **조건부 빈 등록**: `@ConditionalOnProperty(prefix = "file", name = "type", havingValue = "S3")` 어노테이션을 추가합니다.
3. **설정값 주입**: `FileStorageProperties`를 주입받아 `properties.getS3().getBucket()` 형태로 설정값을 사용합니다.
4. **설정 변경**: `application.yml`에서 `file.type`을 `S3`로 변경합니다.

---

## 9. 성능 튜닝

`BatchConfig.java`의 `chunkSize` 값을 조절하여 트랜잭션 커밋 단위를 변경할 수 있습니다.

```java
.<ProductUploadDto, Product>chunk(100, transactionManager)  // 기본값: 100
```

- Chunk 단위로 `saveAll()`이 호출되어 DB 배치 insert가 수행됩니다.
- 파일 크기 및 서버 메모리에 따라 50~500 사이에서 조정을 권장합니다.
