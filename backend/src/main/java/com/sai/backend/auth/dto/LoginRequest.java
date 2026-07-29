package com.sai.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest (
		
		@NotBlank(message = "아이디는 필수 입력사항입니다.")
		@Size(min = 4, max = 25, message = "로그인 아이디는 4자 이상 25자 이하여야합니다.")
		String loginId,
		
		@NotBlank(message = "패스워드는 필수 입력사항입니다.")
		@Size(min = 8, max = 30, message = "패스워드는 8자 이상 30자 이하여야합니다.")
		String password
		
	)
	{
		
	}
