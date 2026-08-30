package com.sai.backend.space.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSpaceRequest(
		@NotBlank
		@Size(min = 1, max = 50, message = "공간의 제목은 1자에서 30자 이내여야 합니다.")
		String title	
		) {
}
