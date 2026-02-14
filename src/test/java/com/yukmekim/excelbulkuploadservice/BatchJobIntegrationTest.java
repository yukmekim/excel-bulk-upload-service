package com.yukmekim.excelbulkuploadservice;

import com.yukmekim.excelbulkuploadservice.batch.BatchConfig;
import com.yukmekim.excelbulkuploadservice.entity.Product;
import com.yukmekim.excelbulkuploadservice.repository.ProductRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@SpringBatchTest
class BatchJobIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job productUploadJob;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setup() {
        productRepository.deleteAll();
    }

    // Explicitly set the job to test utils to avoid ambiguity if multiple jobs
    // existed
    @Autowired
    public void setJobLauncherTestUtils(JobLauncherTestUtils jobLauncherTestUtils) {
        this.jobLauncherTestUtils = jobLauncherTestUtils;
        this.jobLauncherTestUtils.setJob(productUploadJob);
    }

    @Test
    void shouldExecuteBatchJobSuccessfully() throws Exception {
        // 1. Create a Test Excel File
        File testFile = File.createTempFile("batch-test", ".xlsx");
        createTestExcelFile(testFile);

        // 2. Set Job Parameters
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("filePath", testFile.getAbsolutePath())
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        // 3. Launch Job
        // Using direct launch as TestUtils sometimes tricky with scoped beans in Boot 3
        JobExecution jobExecution = jobLauncher.run(productUploadJob, jobParameters);

        // 4. Verify Job Status
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // 5. Verify Data in DB
        List<Product> products = productRepository.findAll();
        assertThat(products).hasSize(2);

        Product p1 = products.stream().filter(p -> p.getName().equals("Batch Product 1")).findFirst().orElseThrow();
        assertThat(p1.getPrice()).isEqualByComparingTo(new BigDecimal("50.00"));

        // Cleanup
        testFile.delete();
    }

    private void createTestExcelFile(File file) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Products");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Name");
        header.createCell(1).setCellValue("Category");
        header.createCell(2).setCellValue("Price");
        header.createCell(3).setCellValue("Stock Quantity");
        header.createCell(4).setCellValue("Description");

        Row row1 = sheet.createRow(1);
        row1.createCell(0).setCellValue("Batch Product 1");
        row1.createCell(1).setCellValue("Batch Category");
        row1.createCell(2).setCellValue(50.00);
        row1.createCell(3).setCellValue(10);
        row1.createCell(4).setCellValue("Description 1");

        Row row2 = sheet.createRow(2);
        row2.createCell(0).setCellValue("Batch Product 2");
        row2.createCell(1).setCellValue("Batch Category");
        row2.createCell(2).setCellValue(100.00);
        row2.createCell(3).setCellValue(20);
        row2.createCell(4).setCellValue("Description 2");

        FileOutputStream fos = new FileOutputStream(file);
        workbook.write(fos);
        workbook.close();
        fos.close();
    }
}
