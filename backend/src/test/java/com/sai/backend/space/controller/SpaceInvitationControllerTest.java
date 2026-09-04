package com.sai.backend.space.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sai.backend.common.session.SessionConst;
import com.sai.backend.space.domain.SpaceInvitationStatus;
import com.sai.backend.space.dto.DenySpaceRequest;
import com.sai.backend.space.dto.InvitationResponse;
import com.sai.backend.space.dto.JoinSpaceRequest;
import com.sai.backend.space.exception.SpaceInvitationNotFoundException;
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
    
    // Unathorized 요청 
    
    @Test 
    void 비로그인_보낸_초대_목록_조회() throws Exception {
    	
    	mockMvc.perform(get("/api/invitations/sent"))
    	.andExpect(status().isUnauthorized());
    	
    }
	
    @Test 
    void 비로그인_받은_초대_목록_조회() throws Exception {
    	mockMvc.perform(get("/api/invitations/received"))
        	.andExpect(status().isUnauthorized());
        	
    }
    
    
    // 정상 요청

    
    @Test
    void 정상ID에서_보낸_초대_목록_조회() throws Exception {
    	
    	
    	Long userId = 2L;
    	
    	List<InvitationResponse> invitationResponseList =
    			new ArrayList<InvitationResponse>();
    	
    	invitationResponseList.add(new InvitationResponse(
    			1L,
    			1L,
    			"My Little Space",
    			1L,
    			"태호",
    			2L,
    			"호태",
    			SpaceInvitationStatus.PENDING
    			));
    	
    	given(spaceInvitationService.getSentInvitationList(userId))
    		.willReturn(invitationResponseList);
    		
    	
    	
    	mockMvc.perform(get("/api/invitations/sent")
    			.sessionAttr(SessionConst.LOGIN_USER_ID, userId))
        	.andExpect(status().isOk())
        	.andExpect(jsonPath("$[0].invitationId").value(1L))
        	.andExpect(jsonPath("$[0].spaceId").value(1L))
        	.andExpect(jsonPath("$[0].spaceName").value("My Little Space"))
        	.andExpect(jsonPath("$[0].inviterId").value(1L))
        	.andExpect(jsonPath("$[0].inviterName").value("태호"))
        	.andExpect(jsonPath("$[0].inviteeId").value(2L))
        	.andExpect(jsonPath("$[0].inviteeName").value("호태"))
        	.andExpect(jsonPath("$[0].invitationStatus").value("PENDING"));
    	
    	
    }
    
    
    @Test
    void 정상ID에서_받은_초대_목록_조회() throws Exception {
    	Long userId = 1L;
    	
    	List<InvitationResponse> invitationResponseList =
    			new ArrayList<InvitationResponse>();
    	
    	invitationResponseList.add(new InvitationResponse(
    			1L,
    			1L,
    			"My Little Space",
    			1L,
    			"태호",
    			2L,
    			"호태",
    			SpaceInvitationStatus.PENDING
    			));
    	
    	given(spaceInvitationService.getReceivedInvitationList(userId))
    		.willReturn(invitationResponseList);
    		
    	
    	
    	mockMvc.perform(get("/api/invitations/received")
    			.sessionAttr(SessionConst.LOGIN_USER_ID, 1L))
        	.andExpect(status().isOk())
        	.andExpect(jsonPath("$[0].invitationId").value(1L))
        	.andExpect(jsonPath("$[0].spaceId").value(1L))
        	.andExpect(jsonPath("$[0].spaceName").value("My Little Space"))
        	.andExpect(jsonPath("$[0].inviterId").value(1L))
        	.andExpect(jsonPath("$[0].inviterName").value("태호"))
        	.andExpect(jsonPath("$[0].inviteeId").value(2L))
        	.andExpect(jsonPath("$[0].inviteeName").value("호태"))
        	.andExpect(jsonPath("$[0].invitationStatus").value("PENDING"));
    	
    	
    }
    
 
    
    // 정상 승락 
    @Test 
    void 정상ID에서_수락_요청() throws Exception 
    {
    	Long userId = 1L;
    	
    	JoinSpaceRequest joinSpaceRequest 
    		= new JoinSpaceRequest(1L);
    	
    	given(spaceInvitationService.joinSpace(userId, joinSpaceRequest))
    		.willReturn(1L);
    	
    	mockMvc.perform(post("/api/invitations/join")
    			.sessionAttr(SessionConst.LOGIN_USER_ID, userId)
    			.contentType(MediaType.APPLICATION_JSON)
    			.content(
    					"""
    						{
    							"invitationId" : 1
    						}
    					"""
    					)
    			
    			)
    		.andExpect(status().isCreated())
    		.andExpect(content().string("1"));
    	
    }
    
    @Test 
    void 정상ID에서_거절_요청() throws Exception 
    {
    	Long userId = 1L;
    	
    	DenySpaceRequest denySpaceRequest 
    		= new DenySpaceRequest(1L);
    	
    	given(spaceInvitationService.denySpaceInvitation(userId, denySpaceRequest.invitationId()))
    		.willReturn(1L);
    	
    	mockMvc.perform(post("/api/invitations/deny")
    			.sessionAttr(SessionConst.LOGIN_USER_ID, userId)
    			.contentType(MediaType.APPLICATION_JSON)
    			.content(
    					"""
    						{
    							"invitationId" : 1
    						}
    					"""
    					)
    			
    			)
    		.andExpect(status().isOk())
    		.andExpect(content().string("1"));
    }
    
    // 비로그인 승락 
    @Test 
    void 비로그인_수락_요청() throws Exception 
    {
    	mockMvc.perform(post("/api/invitations/join")
        		.contentType(MediaType.APPLICATION_JSON)
        		.content("""
        					{
        						"invitationId" : 1
        					}
        				"""))
        	.andExpect(status().isUnauthorized());
    }
    
    @Test 
    void 비로그인_거절_요청() throws Exception 
    {
    	mockMvc.perform(post("/api/invitations/deny")
        		.contentType(MediaType.APPLICATION_JSON)
        		.content("""
        				{
        					"invitationId" : 1
        				}
        				"""))
        	.andExpect(status().isUnauthorized());
    }
    

    
    @Test 
    void 잘못된_초대ID로_수락하면_404에러() throws Exception 
    {
    	Long userId = 1L;
    	
    	JoinSpaceRequest joinSpaceRequest 
    		= new JoinSpaceRequest(1L);
    	
    	given(spaceInvitationService.joinSpace(userId, joinSpaceRequest))
    		.willThrow(new SpaceInvitationNotFoundException());
    	
    	mockMvc.perform(post("/api/invitations/join")
    			.sessionAttr(SessionConst.LOGIN_USER_ID, userId)
    			.contentType(MediaType.APPLICATION_JSON)
    			.content(
    					"""
    						{
    							"invitationId" : 1
    						}
    					"""
    					)
    			
    			)
    		.andExpect(status().isNotFound());
    	
    }
}
