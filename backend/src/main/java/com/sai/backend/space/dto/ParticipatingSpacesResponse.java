package com.sai.backend.space.dto;

public record ParticipatingSpacesResponse(
		Long spaceId,
		String title,
		String role,
		Integer spaceMemberCount
		) {

}
