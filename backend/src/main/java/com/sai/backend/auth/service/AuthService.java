package com.sai.backend.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sai.backend.auth.dto.LoginRequest;
import com.sai.backend.auth.dto.MeResponse;
import com.sai.backend.auth.dto.SignupRequest;
import com.sai.backend.auth.exception.DuplicateLoginIdException;
import com.sai.backend.auth.exception.InvalidLoginException;
import com.sai.backend.auth.exception.UserNotFoundException;
import com.sai.backend.user.domain.User;
import com.sai.backend.user.repository.UserRepository;

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
			throw new DuplicateLoginIdException();
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
	
	@Transactional
	public Long login(LoginRequest loginRequest)
	{
		
		User foundUser = userRepository.findByLoginId(loginRequest.loginId())
				.orElseThrow(() ->  new InvalidLoginException());
		
		boolean isPasswordMatched =
				passwordEncoder.matches(
						loginRequest.password(), 
						foundUser.getPasswordHash());
		
		if(!isPasswordMatched)
		{
			throw new InvalidLoginException();
		}
		
		return foundUser.getId();
		
	}
	
	@Transactional
	public MeResponse getMe(Long userId)
	{
		User foundUser = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException());
		
		return new MeResponse(
				foundUser.getEmail(),
				foundUser.getName(),
				foundUser.getPhoneNumber(),
				foundUser.getLoginId()
				);
		
	}
	
	
}
