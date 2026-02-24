package com.yukmekim.excelbulkuploadservice.batch;

import com.yukmekim.excelbulkuploadservice.entity.Product;
import com.yukmekim.excelbulkuploadservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Spring Batch ItemWriter 구현체.
 * RepositoryItemWriter(save() 개별 호출) 대신 saveAll()을 사용하여
 * Chunk 단위로 한 번의 배치 insert를 수행, DB 쿼리 횟수를 최소화한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductItemWriter implements ItemWriter<Product> {

    private final ProductRepository productRepository;

    @Override
    public void write(Chunk<? extends Product> chunk) {
        log.debug("Writing chunk of {} products", chunk.size());
        productRepository.saveAll(chunk.getItems());
    }
}
