package com.sai.backend.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sai.backend.user.domain.User;
import com.sai.backend.user.dto.SearchUserListByLoginIdRequest;
import com.sai.backend.user.dto.SearchUserListByLoginIdResponse;
import com.sai.backend.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private UserService userService;

	@Test
	public void 로그인아이디로_사용자목록을_조회한다() {
		String searchLoginId = "hyegyu";
		User foundUser = new User(
				"강태호", "hyegyu99", "encoded-password",
				"010-1234-5678", "hyegyu@example.com");

		given(userRepository
				.findTop10ByLoginIdContainingIgnoreCaseOrderByLoginId(searchLoginId))
				.willReturn(List.of(foundUser));

		List<SearchUserListByLoginIdResponse> result = userService.searchUser(
				new SearchUserListByLoginIdRequest(searchLoginId));

		assertEquals(1, result.size());
		assertEquals("hyegyu99", result.get(0).loginId());
		assertEquals("강태호", result.get(0).name());
	}

	@Test
	public void 검색결과가_없으면_빈목록을_반환한다() {
		String searchLoginId = "unknown";

		given(userRepository
				.findTop10ByLoginIdContainingIgnoreCaseOrderByLoginId(searchLoginId))
				.willReturn(List.of());

		List<SearchUserListByLoginIdResponse> result = userService.searchUser(
				new SearchUserListByLoginIdRequest(searchLoginId));

		assertEquals(List.of(), result);
	}
}
