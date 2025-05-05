package DiplomCloud.DiplomCloud.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.storage")
public class FileStorageProperties {
    private String location;
    private String maxFileSize;
}
