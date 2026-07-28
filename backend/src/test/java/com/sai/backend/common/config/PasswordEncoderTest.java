package com.sai.backend.common.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder; 

@SpringBootTest
public class PasswordEncoderTest {

	@Autowired
	private PasswordEncoder passwordEncorder;
	
	
	@Test
	public void encodeAndMathPassword()
	{
		String rawPassword = "MyPassWord";
		String encodedPassword = passwordEncorder.encode(rawPassword);
		
		assertNotEquals(rawPassword, encodedPassword);
		
		assertTrue(passwordEncorder.matches(rawPassword, encodedPassword));
		
		assertFalse(passwordEncorder.matches("ABC", encodedPassword));
		
	}
}
