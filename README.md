# 엑셀 대용량 업로드 서비스 가이드

## 1. 프로젝트 개요

Spring Boot와 Spring Batch를 기반으로 구축된 대용량 엑셀 파일 업로드 및 처리 서비스입니다.
대량의 데이터를 효율적으로 처리하기 위해 Apache POI의 스트리밍 방식을 사용하지 않고, 메모리 효율적인 구조를 채택했으며 비동기 배치 작업을 통해 시스템 안정성을 보장합니다.

## 2. 주요 기능 및 아키텍처

- **대용량 처리**: Spring Batch의 Chunk 지향 처리를 통해 데이터를 일정 단위로 나누어 처리
- **비동기 실행**: 파일 업로드 요청 시 즉시 응답하고 배치는 백그라운드에서 실행
- **유연한 데이터 매핑**: 엑셀 컬럼 순서에 의존하지 않고 헤더 이름을 기반으로 데이터를 매핑 (Header-Based Mapping)
- **확장 가능한 파일 저장소**: 설정 파일을 통해 로컬, S3 등 저장소 유형을 손쉽게 변경 가능
- **전역 예외 처리**: 표준화된 에러 응답 및 예외 처리

## 3. 대량 데이터 추가 및 변경 가이드

프로젝트를 사용하여 새로운 데이터를 추가하거나 기존 형식을 변경할 때 수정해야 할 사항들입니다.

### 3.1. 엑셀 파일 형식 규칙 (헤더 기반 매핑)

이 서비스는 엑셀 파일의 첫 번째 행(Header)에 있는 이름을 기준으로 데이터를 읽어옵니다.
따라서 **컬럼의 순서가 바뀌어도 헤더 이름만 같다면 정상적으로 동작**합니다.

- **필수 헤더 이름**:
    - `Name`
    - `Category`
    - `Price`
    - `Stock Quantity`
    - `Description`

### 3.2. 데이터 필드(컬럼) 추가 시 변경 포인트

1.  **DB & Entity**: `Product.java` 및 DB 테이블에 컬럼 추가
2.  **DTO**: `ProductUploadDto.java`에 필드 추가
3.  **Reader**: `ExcelItemReader.java`의 `read()` 메소드에서 `getCellValue(row, "NewHeader")` 로직 추가

### 3.3. 성능 튜닝 (Chunk Size)

- `src/main/java/com/yukmekim/excelbulkuploadservice/batch/BatchConfig.java`의 `chunkSize` 값을 조절하여 트랜잭션 커밋 단위를 변경할 수 있습니다. (기본값: 100)

---

## 4. 환경 설정 가이드 (Configuration)

이 서비스는 `application.yml` 파일을 통해 파일 저장소 위치 및 유형을 유연하게 설정할 수 있습니다.
코드 수정 없이 설정 변경만으로 로컬 저장 경로를 바꾸거나, 추후 구현될 S3 저장소로 전환할 수 있습니다.

### 4.1. 설정 파일 예시 (`src/main/resources/application.yml`)

```yaml
# File Storage Configuration
file:
  type: LOCAL  # 사용할 저장소 유형 (LOCAL, S3, CDN)
  
  # 로컬 저장소 설정 (type: LOCAL 일 때 사용)
  local:
    upload-dir: upload-dir  # 파일이 저장될 로컬 디렉토리 경로 (상대/절대 경로)
    
  # S3 저장소 설정 (type: S3 일 때 사용 - 구현 필요)
  s3:
    bucket: my-excel-bucket
    region: ap-northeast-2
    access-key: YOUR_ACCESS_KEY
    secret-key: YOUR_SECRET_KEY
```

### 4.2. 설정 항목 상세

| 프로퍼티 경로 | 설명 | 기본값 | 비고 |
| :--- | :--- | :--- | :--- |
| `file.type` | 사용할 스토리지 구현체 선택 | `LOCAL` | `LOCAL`, `S3`, `CDN` 중 택 1 |
| `file.local.upload-dir` | 로컬 파일 저장 경로 | `upload-dir` | `file.type: LOCAL` 필수 |
| `file.s3.bucket` | AWS S3 버킷명 | - | `file.type: S3` 필수 |
| `file.s3.region` | AWS S3 리전 | - | `file.type: S3` 필수 |

---

## 5. 파일 저장소 확장 가이드

현재는 `LocalFileStorageService`가 기본으로 구현되어 있습니다. AWS S3 등을 연결하려면 다음 단계를 따르세요.

1.  **구현체 생성**: `FileStorageService` 인터페이스를 구현하는 `S3FileStorageService` 클래스를 생성합니다.
2.  **조건부 빈 등록**: `@ConditionalOnProperty(prefix = "file", name = "type", havingValue = "S3")` 어노테이션을 붙여 설정값에 따라 활성화되도록 합니다.
3.  **설정값 사용**: `FileStorageProperties`를 주입받아 `properties.getS3().getBucket()` 형태로 설정값을 가져와 사용합니다.
4.  **설정 변경**: `application.yml`에서 `file.type`을 `S3`로 변경합니다.
