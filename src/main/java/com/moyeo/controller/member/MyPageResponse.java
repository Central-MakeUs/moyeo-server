package com.moyeo.controller.member;

import com.moyeo.controller.auth.ProfileResponse;
import com.moyeo.controller.place.SavedPlaceResponse;
import com.moyeo.service.member.AuthenticatedMember;
import com.moyeo.service.place.SavedPlaceResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "마이페이지 조회 응답입니다. 회원 기본 정보와 저장 출발지 목록을 함께 반환합니다.")
public record MyPageResponse(
        @Schema(description = "회원 기본 닉네임", example = "모여") String nickname,
        @Schema(description = "회원 프로필 표시 정보") ProfileResponse profile,
        @Schema(description = "저장 일시 최신순 출발지 목록") List<SavedPlaceResponse> places
) {

    public static MyPageResponse from(AuthenticatedMember member, List<SavedPlaceResult> places) {
        return new MyPageResponse(
                member.nickname(),
                ProfileResponse.color(member.profileColor()),
                places.stream().map(SavedPlaceResponse::from).toList()
        );
    }
}
