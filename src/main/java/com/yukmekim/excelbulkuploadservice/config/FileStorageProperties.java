package com.yukmekim.excelbulkuploadservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "file")
@Getter
@Setter
public class FileStorageProperties {

    private StorageType type = StorageType.LOCAL; // Default to LOCAL

    private Local local = new Local();
    private S3 s3 = new S3();

    @Getter
    @Setter
    public static class Local {
        private String uploadDir = "upload-dir";
    }

    @Getter
    @Setter
    public static class S3 {
        private String bucket;
        private String region;
        private String accessKey;
        private String secretKey;
    }

    public enum StorageType {
        LOCAL, S3, CDN
    }
}
