package edu.cit.audioscholar;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.google.cloud.firestore.Firestore;

@SpringBootTest
class AudioscholarApplicationTests {

	@MockBean
	private Firestore firestore;

	@Test
	void contextLoads() {
	}

}
