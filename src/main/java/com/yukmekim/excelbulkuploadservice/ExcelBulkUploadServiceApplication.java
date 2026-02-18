package com.yukmekim.excelbulkuploadservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ExcelBulkUploadServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExcelBulkUploadServiceApplication.class, args);
    }

}
