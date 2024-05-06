package vn.bnh.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;

@Configuration
public class AppConfig {
    @Bean
    public DatabaseConfiguration databaseConfiguration() throws IOException {
        return new ObjectMapper().readValue(new File(System.getProperty("user.dir") + "/config.json"), DatabaseConfiguration.class);
    }
}
