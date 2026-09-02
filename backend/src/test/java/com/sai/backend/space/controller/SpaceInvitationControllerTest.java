package com.sai.backend.space.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sai.backend.common.session.SessionConst;
import com.sai.backend.space.service.SpaceInvitationService;

@WebMvcTest(SpaceInvitationController.class)
public class SpaceInvitationControllerTest {

	@Autowired 
	MockMvc mockMvc;
	 
	@MockitoBean
	SpaceInvitationService spaceInvitationService;
	
    @Test
    void 참여_초대_아이디_누락() throws Exception
    {
    	Long userId = 1L; 
    	
    	
    	mockMvc.perform(post("/api/invitations/join")
    		.sessionAttr(SessionConst.LOGIN_USER_ID, userId )
    		.contentType(MediaType.APPLICATION_JSON)
    		.content("{}")
    			)
    		.andExpect(status().isBadRequest())
    		.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
    		;
    	
    	
    }
	
}
