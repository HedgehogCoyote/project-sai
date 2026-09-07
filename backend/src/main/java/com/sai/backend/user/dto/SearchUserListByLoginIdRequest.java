package com.sai.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SearchUserListByLoginIdRequest(
		@NotBlank(message = "검색어를 입력해주세요.")
        @Size(min = 2, max = 20, message = "검색어는 2자 이상 20자 이하로 입력해주세요.")
		String loginId
		) {

}
