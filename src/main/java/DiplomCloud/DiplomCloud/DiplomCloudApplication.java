package DiplomCloud.DiplomCloud;

import DiplomCloud.DiplomCloud.config.FileStorageProperties;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootApplication
@EnableJpaRepositories
@ConfigurationPropertiesScan  // Важно для работы с @Value
public class DiplomCloudApplication {

	public static void main(String[] args) {
		SpringApplication.run(DiplomCloudApplication.class, args);
	}
}
