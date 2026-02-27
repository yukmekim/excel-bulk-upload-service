package com.yukmekim.excelbulkuploadservice.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class DynamicUploadRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    /**
     * 동적으로 테이블과 데이터를 매핑하여 Batch Insert를 수행합니다.
     * NamedParameterJdbcTemplate을 사용하여 Map 구조의 데이터를 매우 빠르게 삽입합니다.
     *
     * @param tableName 대상 테이블명 (보안을 위해 내부 시스템에서 매핑한 안전한 테이블명만 전달해야 함)
     * @param records List of Maps 형태의 삽입할 데이터
     */
    public void batchInsert(String tableName, List<Map<String, Object>> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        // 첫 번째 레코드를 기준(Excel Header)으로 컬럼명(Key) 추출
        Map<String, Object> firstRecord = records.get(0);
        Set<String> columnNames = firstRecord.keySet();

        // SQL 조립: INSERT INTO tableName (col1, col2) VALUES (:col1, :col2)
        String sql = buildInsertSql(tableName, columnNames);

        // List<Map<...>> 형태를 NamedParameterJdbcTemplate 이 인식할 수 있는 Map 배열로 변환
        @SuppressWarnings("unchecked")
        Map<String, Object>[] batchValues = records.toArray(new Map[0]);

        // 대량(Bulk) Insert 수행
        namedParameterJdbcTemplate.batchUpdate(sql, batchValues);
    }

    private String buildInsertSql(String tableName, Set<String> columnNames) {
        // (col1, col2, col3)
        String columns = String.join(", ", columnNames);
        
        // (:col1, :col2, :col3)
        String values = columnNames.stream()
                .map(col -> ":" + col)
                .collect(Collectors.joining(", "));

        return String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columns, values);
    }
}
