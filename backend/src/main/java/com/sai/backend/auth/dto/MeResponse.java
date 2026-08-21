package com.sai.backend.auth.dto;

public record MeResponse(
		String email,
		String name,
		String phoneNumber,
		String loginId
		) 
{ }
