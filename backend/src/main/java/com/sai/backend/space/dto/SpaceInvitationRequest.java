package com.sai.backend.space.dto;

import jakarta.validation.constraints.NotNull;

public record SpaceInvitationRequest(
		@NotNull
		Long spaceId,
		@NotNull
		Long inviteeUserId
		) 
{
}
