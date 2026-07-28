package com.sai.backend.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.sai.backend.auth.dto.SignupRequest;
import com.sai.backend.user.domain.User;
import com.sai.backend.user.repository.UserRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
	
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	
	@Transactional
	public Long signup(SignupRequest request)
	{
		if(userRepository.existsByLoginId(request.loginId()))
		{
			throw new IllegalArgumentException("이미 존재하는 사용자입니다.");
		}
		
		String encodedPass = passwordEncoder.encode(request.password());
		
		User newUser = new User(
				request.name() ,
				request.loginId(),
				encodedPass, 
				request.phoneNumber(), 
				request.email()
				);
		
		User savedUser = userRepository.save(newUser);
		
		return  savedUser.getId();
		
	}
	
}
