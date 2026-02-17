# 엑셀 대용량 업로드 서비스 가이드

## 1. 프로젝트 개요

Spring Boot와 Spring Batch를 기반으로 구축된 대용량 엑셀 파일 업로드 및 처리 서비스입니다.
대량의 데이터를 효율적으로 처리하기 위해 Apache POI의 스트리밍 방식을 사용하지 않고, 메모리 효율적인 구조를 채택했으며 비동기 배치 작업을 통해 시스템 안정성을 보장합니다.

## 2. 주요 기능 및 아키텍처

- **대용량 처리**: Spring Batch의 Chunk 지향 처리를 통해 데이터를 일정 단위로 나누어 처리
- **비동기 실행**: 파일 업로드 요청 시 즉시 응답하고 배치는 백그라운드에서 실행
- **유연한 데이터 매핑**: 엑셀 컬럼 순서에 의존하지 않고 헤더 이름을 기반으로 데이터를 매핑 (Header-Based Mapping)
- **확장 가능한 파일 저장소**: 로컬 디스크뿐만 아니라 S3, CDN 등으로 확장 가능한 인터페이스 구조 (FileStorageService)
- **전역 예외 처리**: 표준화된 에러 응답 및 예외 처리

## 3. 대량 데이터 추가 및 변경 가이드

프로젝트를 사용하여 새로운 데이터를 추가하거나 기존 형식을 변경할 때 수정해야 할 사항들입니다.

### 3.1. 엑셀 파일 형식 규칙

이 서비스는 엑셀 파일의 첫 번째 행(Header)에 있는 이름을 기준으로 데이터를 읽어옵니다.
따라서 엑셀 파일 작성 시 다음 규칙을 준수해야 합니다.

1.  **첫 번째 행은 반드시 헤더(컬럼명)여야 합니다.**
2.  컬럼의 순서는 바뀌어도 상관없습니다. (예: Price가 Name보다 앞에 와도 정상 동작)
3.  현재 매핑된 기본 헤더 이름:
    - Name
    - Category
    - Price
    - Stock Quantity
    - Description

### 3.2. 데이터 필드(컬럼) 추가 시 변경해야 할 코드

새로운 정보(예: 제조사, 유통기한 등)를 추가해야 할 경우 다음 순서대로 코드를 수정하십시오.

1.  **데이터베이스 스키마 및 엔티티 수정**
    - `src/main/java/com/yukmekim/excelbulkuploadservice/entity/Product.java` 파일에 새로운 필드를 추가합니다.
    - 데이터베이스의 `product` 테이블에도 해당 컬럼을 추가해야 합니다.

2.  **DTO 수정**
    - `src/main/java/com/yukmekim/excelbulkuploadservice/dto/ProductUploadDto.java` 파일에 새로운 필드를 추가합니다.

3.  **엑셀 읽기 로직 수정 (핵심)**
    - `src/main/java/com/yukmekim/excelbulkuploadservice/batch/ExcelItemReader.java` 파일의 `read()` 메소드를 수정합니다.
    - `getCellValue(row, "새로운헤더이름")` 또는 `getNumericCellValue` 등을 사용하여 값을 읽어오도록 코드를 추가합니다.
    - `ProductUploadDto` 빌더 패턴에 읽어온 값을 매핑합니다.

4.  **프로세서 수정 (선택 사항)**
    - `src/main/java/com/yukmekim/excelbulkuploadservice/batch/ProductProcessor.java`에서 데이터 변환이나 검증 로직이 필요하다면 수정합니다.

### 3.3. 대량 처리 성능 튜닝

데이터의 양이 매우 많아 성능 이슈가 발생할 경우 다음 설정을 변경할 수 있습니다.

- **Chunk Size 변경**
    - `src/main/java/com/yukmekim/excelbulkuploadservice/batch/BatchConfig.java` 파일의 `chunkSize` 값을 조절합니다.
    - 현재 기본값: `100`
    - 데이터가 많고 메모리가 충분하다면 `1000` 등으로 늘려서 DB 커밋 횟수를 줄일 수 있습니다.
    - 너무 크게 설정하면 메모리 부족(OOM) 오류가 발생할 수 있으니 주의하십시오.

### 3.4. 파일 저장소 변경 (로컬 -> S3 등)

현재는 업로드된 파일을 로컬 디스크(`upload-dir`)에 저장하고 있습니다. 이를 AWS S3나 다른 스토리지로 변경하려면 다음과 같이 진행하십시오.

1.  `src/main/java/com/yukmekim/excelbulkuploadservice/service/storage/` 패키지에 `FileStorageService` 인터페이스를 구현하는 새로운 클래스를 생성합니다. (예: `S3FileStorageService`)
2.  `store`, `load`, `delete` 메소드를 해당 스토리지 API에 맞게 구현합니다.
3.  `src/main/java/com/yukmekim/excelbulkuploadservice/service/ProductService.java`에서 주입받는 `FileStorageService` 구현체를 새로 만든 클래스로 교체하거나, Spring 프로파일(@Profile) 기능을 사용하여 환경에 따라 주입받도록 설정합니다.
