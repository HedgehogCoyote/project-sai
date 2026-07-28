package com.sai.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest (
	
	@NotBlank(message = "이름은 필수 입니다.")
	String name,
	
	@NotBlank(message = "아이디는 필수 입력사항입니다.")
	@Size(min = 4, max = 25, message = "로그인 아이디는 4자 이상 25자 이하여야합니다.")
	String loginId,
	
	@NotBlank(message = "패스워드는 필수 입력사항입니다.")
	@Size(min = 8, max = 30, message = "패스워드는 8자 이상 30자 이하여야합니다.")
	String password,
	
	@NotBlank(message = "핸드폰 번호는 필수 입력사항입니다.")
	
	@Pattern(
			regexp = "^01[1234567890]-?\\d{4}-?\\d{4}$",
			message = "전화번호 형식이 올바르지 않습니다."
			)
	
	String phoneNumber,
	
	@NotBlank(message = "이메일은 필수 입력사항입니다.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	String email
)
{
	
}
