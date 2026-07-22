package com.moyeo.controller.place;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "내 장소 별칭 수정 요청")
public record RenameSavedPlaceRequest(
        @Schema(description = "새 장소 별칭", example = "새 회사", maxLength = 30)
        @NotBlank @Size(max = 30) String alias
) {

    public RenameSavedPlaceRequest {
        alias = alias == null ? null : alias.strip();
    }
}
