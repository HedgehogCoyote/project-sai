package com.sai.backend.user.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sai.backend.user.dto.SearchUserListByLoginIdRequest;
import com.sai.backend.user.dto.SearchUserListByLoginIdResponse;
import com.sai.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	
	public List<SearchUserListByLoginIdResponse> searchUser(
			SearchUserListByLoginIdRequest searchUserListByLoginIdRequest
			)
	{
		
		
		return userRepository
				.findTop10ByLoginIdContainingIgnoreCaseOrderByLoginId(
						searchUserListByLoginIdRequest.loginId().trim())
				.stream()
				.map(user -> new SearchUserListByLoginIdResponse(
						user.getId(), user.getLoginId(), user.getName()))
				.toList();
		
	}
	
}
