package com.yukmekim.excelbulkuploadservice.batch;

import com.yukmekim.excelbulkuploadservice.dto.ProductUploadDto;
import com.yukmekim.excelbulkuploadservice.entity.Product;
import com.yukmekim.excelbulkuploadservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.data.RepositoryItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class BatchConfig {

    private final ProductRepository productRepository;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final UploadFileCleanupListener uploadFileCleanupListener;

    @Bean
    public Job productUploadJob(Step productUploadStep) {
        return new JobBuilder("productUploadJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(uploadFileCleanupListener)
                .start(productUploadStep)
                .build();
    }

    @Bean
    public Step productUploadStep(ItemReader<ProductUploadDto> excelReader) {
        return new StepBuilder("productUploadStep", jobRepository)
                .<ProductUploadDto, Product>chunk(100, transactionManager)
                .reader(excelReader)
                .processor(productProcessor())
                .writer(productWriter())
                .build();
    }

    @Bean
    @StepScope
    public ExcelItemReader excelReader(@Value("#{jobParameters['filePath']}") String filePath) {
        return new ExcelItemReader(filePath);
    }

    @Bean
    public ProductProcessor productProcessor() {
        return new ProductProcessor();
    }

    @Bean
    public RepositoryItemWriter<Product> productWriter() {
        RepositoryItemWriter<Product> writer = new RepositoryItemWriter<>();
        writer.setRepository(productRepository);
        writer.setMethodName("save");
        return writer;
    }
}
