package com.sai.backend.user.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sai.backend.common.session.SessionConst;
import com.sai.backend.user.dto.SearchUserListByLoginIdRequest;
import com.sai.backend.user.dto.SearchUserListByLoginIdResponse;
import com.sai.backend.user.service.UserService;

@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @MockitoBean
    private UserService userService;
    
    @Test
	public void 정상_검색이면_200() throws Exception {
    	
		String searchLoginId = "hyegyu";
		
		List<SearchUserListByLoginIdResponse> responseList = List.of(
				new SearchUserListByLoginIdResponse(2L, "hyegyu99", "강태호"));
		
		given(userService.searchUser(new SearchUserListByLoginIdRequest(searchLoginId)))
			.willReturn(responseList);
			
		
		mockMvc.perform(
				get("/api/users/search")
				.sessionAttr(SessionConst.LOGIN_USER_ID, 1L)
				.param("loginId", searchLoginId)
				)
		.andExpect(status().isOk())
		.andExpect(jsonPath("$[0].userId").value(2L))
		.andExpect(jsonPath("$[0].loginId").value("hyegyu99"))
		.andExpect(jsonPath("$[0].name").value("강태호"));
	}
	
    @Test
	public void 비로그인_검색_이면_401_에러() throws Exception 
	{
		String searchLoginId = "hyegyu";
		
		mockMvc.perform(
				get("/api/users/search")
				.param("loginId", searchLoginId)
				)
		.andExpect(status().isUnauthorized());

		then(userService).shouldHaveNoInteractions();
		
	}
	
    @Test
	public void 검색아이디_길이_2자미만시_400_에러()  throws Exception 
	{
    	Long loginUserId = 1L;
		String searchLoginId = "h";
		
		mockMvc.perform(
				get("/api/users/search")
				.sessionAttr(SessionConst.LOGIN_USER_ID, loginUserId)
				.param("loginId", searchLoginId)
				)
		.andExpect(status().isBadRequest());
	}

	@Test
	public void 검색아이디_누락시_400_에러() throws Exception {
		mockMvc.perform(
				get("/api/users/search")
				.sessionAttr(SessionConst.LOGIN_USER_ID, 1L))
		.andExpect(status().isBadRequest());
	}

	@Test
	public void 검색결과가_없으면_빈배열_200() throws Exception {
		String searchLoginId = "unknown";

		given(userService.searchUser(new SearchUserListByLoginIdRequest(searchLoginId)))
				.willReturn(List.of());

		mockMvc.perform(
				get("/api/users/search")
				.sessionAttr(SessionConst.LOGIN_USER_ID, 1L)
				.param("loginId", searchLoginId))
		.andExpect(status().isOk())
		.andExpect(jsonPath("$").isArray())
		.andExpect(jsonPath("$").isEmpty());
	}
	
    
}
