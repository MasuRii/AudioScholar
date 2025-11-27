package edu.cit.audioscholar;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.google.cloud.firestore.Firestore;

@SpringBootTest
class AudioscholarApplicationTests {

	@MockitoBean
	private Firestore firestore;

	@Test
	void contextLoads() {
	}

}
