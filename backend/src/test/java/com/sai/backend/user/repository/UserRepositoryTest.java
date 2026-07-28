package com.sai.backend.user.repository;


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.sai.backend.user.domain.User;

@SpringBootTest
@Transactional
public class UserRepositoryTest {
	
	@Autowired
	private UserRepository userRepository;
	
	@Test
	void saveAndFindUser()
	{
		User testUser = new User("KANG TAEHO", "testuser",
				"Password@@hashed", "010-1234-5678" , "test@example.com");
		User saveUser =  userRepository.save(testUser);
		
		assertNotNull(saveUser.getId());
		assertNotNull(saveUser.getCreatedAt());
		
		User foundUser = userRepository.findByLoginId("testuser").get();
		
		assertNotNull(foundUser);
		assertNotNull(foundUser.getCreatedAt());
		
		assertTrue(userRepository.existsByLoginId("testuser"));
		
	}
	
}
