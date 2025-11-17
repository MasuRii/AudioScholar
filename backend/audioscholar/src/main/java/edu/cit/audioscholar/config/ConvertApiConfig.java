package edu.cit.audioscholar.config;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.convertapi.client.Config;

@Configuration
public class ConvertApiConfig {

	private static final Logger logger = LoggerFactory.getLogger(ConvertApiConfig.class);

	@Value("${convertapi.secret:${CONVERTAPI_SECRET:}}")
	private String convertApiSecret;

	@PostConstruct
	public void init() {
		// Direct access to environment variable since @Value annotation is not
		// resolving it
		String secretFromEnv = System.getenv("CONVERTAPI_SECRET");

		logger.info("ConvertAPI secret from @Value: '{}'", convertApiSecret);
		logger.info("ConvertAPI secret from System.getenv: '{}'", secretFromEnv);

		if ((convertApiSecret == null || convertApiSecret.isBlank())
				&& (secretFromEnv == null || secretFromEnv.isBlank())) {
			logger.warn("ConvertAPI secret is not configured. PPTX-to-PDF conversion will not work.");
		} else {
			String secretToUse = secretFromEnv != null && !secretFromEnv.isBlank() ? secretFromEnv : convertApiSecret;
			logger.info("Initializing ConvertAPI with provided secret");
			Config.setDefaultApiCredentials(secretToUse);
		}
	}
}
