package edu.cit.audioscholar.config;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;

@Configuration
public class DotenvConfig {

	private static final Logger logger = LoggerFactory.getLogger(DotenvConfig.class);

	private final ConfigurableEnvironment environment;

	public DotenvConfig(ConfigurableEnvironment environment) {
		this.environment = environment;
	}

	@PostConstruct
	public void loadDotenv() {
		try {
			// Load .env file from current directory
			Dotenv dotenv = Dotenv.configure().filename(".env") // explicitly specify .env file
					.load();

			Map<String, Object> dotenvProperties = new HashMap<>();

			// Load all environment variables from .env file
			dotenv.entries().forEach(entry -> {
				String key = entry.getKey();
				String value = entry.getValue();
				dotenvProperties.put(key, value);
				logger.debug("Loaded .env property: {} = {}", key, value);
			});

			// Add to Spring Environment
			MapPropertySource propertySource = new MapPropertySource("dotenv", dotenvProperties);
			environment.getPropertySources().addFirst(propertySource);

			logger.info("Successfully loaded {} properties from .env file", dotenvProperties.size());

		} catch (Exception e) {
			logger.warn("Could not load .env file: {}", e.getMessage());
			logger.debug("Detailed error", e);
		}
	}
}
