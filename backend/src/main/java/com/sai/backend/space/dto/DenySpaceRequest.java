package com.sai.backend.space.dto;

import jakarta.validation.constraints.NotNull;

public record DenySpaceRequest(
		@NotNull(message = "초대 ID를 설정해주세요.")
		Long invitationId
		) {

}
