package com.sai.backend.user.dto;

public record SearchUserListByLoginIdResponse(
		Long userId,
		String loginId,
		String name
		) {}
