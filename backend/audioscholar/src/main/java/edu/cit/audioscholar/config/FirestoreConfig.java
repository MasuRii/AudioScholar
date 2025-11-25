package edu.cit.audioscholar.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.FirestoreClient;

@Configuration
public class FirestoreConfig {

	private static final Logger logger = LoggerFactory.getLogger(FirestoreConfig.class);

	@Bean
	public Firestore firestore() {
		// Use the existing FirebaseApp instead of creating a new one
		FirebaseApp firebaseApp = FirebaseApp.getInstance();

		// Create Firestore instance using the existing FirebaseApp
		Firestore firestore = FirestoreClient.getFirestore(firebaseApp);
		logger.info("Firestore instance created successfully using existing Firebase Admin SDK.");

		return firestore;
	}
}
