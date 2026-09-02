package com.sai.backend.space.dto;

import jakarta.validation.constraints.NotNull;

public record JoinSpaceRequest(
		
		@NotNull(message = "초대 ID를 설정해주세요.")
		Long invitationId
		) 
{

}
