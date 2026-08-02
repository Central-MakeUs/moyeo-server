package com.moyeo.controller.meeting;

import com.moyeo.service.meeting.PlaceViewResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "장소 조율 현황 조회 응답")
public record PlaceViewResponse(
        @Schema(description = "모임 ID", example = "1")
        Long meetingId,

        @Schema(description = "장소 추천 방식입니다. 장소 조율 모임이 아니면 null입니다.", example = "MIDDLE_POINT", allowableValues = {"MIDDLE_POINT", "RANDOM"})
        String placeRecommendationStrategy,

        @Schema(description = "추천 산출 방식입니다. MIDDLE_POINT는 좌표가 있으면 STRAIGHT_LINE_PREVIEW를, 좌표가 하나도 없으면 COORDINATES_PENDING을 반환합니다. RANDOM은 RANDOM_CATALOG_PREVIEW를 반환하고, 장소 조율 모임이 아니면 null입니다.", example = "STRAIGHT_LINE_PREVIEW", allowableValues = {"STRAIGHT_LINE_PREVIEW", "COORDINATES_PENDING", "RANDOM_CATALOG_PREVIEW"})
        String recommendationBasis,

        @Schema(description = "참여자 출발지 좌표의 단순 평균 지점입니다. MIDDLE_POINT에서 출발지가 하나 이상 있을 때만 반환하고, RANDOM 또는 장소 조율 모임이 아니면 null입니다.")
        CoordinateResponse center,

        @Schema(description = "현재 참여 인원. 방장을 포함합니다.", example = "4")
        long participantCount,

        @Schema(description = "참여자별 출발지 정보")
        List<ParticipantDepartureResponse> participants,

        @Schema(description = "추천 상권 목록. MIDDLE_POINT는 예비 후보를 최대 3개, RANDOM은 최대 5개 반환하며 추천이 없으면 빈 배열입니다.")
        List<RecommendationResponse> recommendations
) {

    public static PlaceViewResponse from(PlaceViewResult result) {
        return new PlaceViewResponse(
                result.meetingId(),
                result.placeRecommendationStrategy(),
                result.recommendationBasis(),
                result.center() != null ? CoordinateResponse.from(result.center()) : null,
                result.participantCount(),
                result.participants().stream().map(ParticipantDepartureResponse::from).toList(),
                result.recommendations().stream().map(RecommendationResponse::from).toList()
        );
    }

    @Schema(description = "좌표")
    public record CoordinateResponse(
            @Schema(description = "위도", example = "37.5344715")
            BigDecimal latitude,

            @Schema(description = "경도", example = "126.9726696")
            BigDecimal longitude
    ) {

        private static CoordinateResponse from(PlaceViewResult.Coordinate coordinate) {
            return new CoordinateResponse(coordinate.latitude(), coordinate.longitude());
        }
    }

    @Schema(description = "참여자 출발지 정보")
    public record ParticipantDepartureResponse(
            @Schema(description = "모임 참여자 ID", example = "1")
            Long participantId,

            @Schema(description = "연결된 서비스 사용자 ID입니다. 게스트 참여자는 null입니다.", example = "42", nullable = true)
            Long userId,

            @Schema(description = "모임 안에서 표시할 닉네임", example = "moyeo1")
            String nickname,

            @Schema(description = "참여자 유형", example = "HOST", allowableValues = {"HOST", "MEMBER", "GUEST"})
            String participantType,

            @Schema(description = "연결된 서비스 사용자의 탈퇴 여부. 게스트는 항상 false입니다.", example = "false")
            boolean withdrawn,

            @Schema(description = "출발지 표시 이름입니다. 요청에서 name을 생략하면 출발지 주소를 반환합니다.", example = "회사")
            String departureName,

            @Schema(description = "출발지 주소. 입력하지 않았으면 null입니다.", example = "서울 강남구 테헤란로 123")
            String departureAddress,

            @Schema(description = "이동 수단입니다. 입력하지 않았으면 null입니다.", example = "PUBLIC_TRANSIT", allowableValues = {"PUBLIC_TRANSIT", "CAR"})
            String transportationMode
    ) {

        private static ParticipantDepartureResponse from(PlaceViewResult.ParticipantDeparture participant) {
            return new ParticipantDepartureResponse(
                    participant.participantId(),
                    participant.userId(),
                    participant.nickname(),
                    participant.participantType(),
                    participant.withdrawn(),
                    participant.departureName(),
                    participant.departureAddress(),
                    participant.transportationMode()
            );
        }
    }

    @Schema(description = "추천 상권")
    public record RecommendationResponse(
            @Schema(description = "추천 순위", example = "1")
            int rank,

            @Schema(description = "상권 코드", example = "1001491")
            String areaCode,

            @Schema(description = "상권명", example = "삼각지역")
            String areaName,

            @Schema(description = "상권 분류명", example = "관광특구", allowableValues = {"발달상권", "관광특구"})
            String categoryName,

            @Schema(description = "상권 중심 위도", example = "37.5344715")
            BigDecimal latitude,

            @Schema(description = "상권 중심 경도", example = "126.9726696")
            BigDecimal longitude,

            @Schema(description = "자치구명", example = "용산구")
            String guName,

            @Schema(description = "행정동명", example = "한강로동")
            String dongName,

            @Schema(description = "참여자 출발지에서 상권까지의 평균 직선거리 미터. 랜덤 추천이면 null입니다.", example = "3200")
            Long averageStraightDistanceMeters,

            @Schema(description = "정원이 찬 뒤 최초 조회에서 저장한 참여자 평균 실제 이동시간(초)입니다. 직선거리 미리보기에서는 null입니다.", example = "1200")
            Long averageTravelTimeSeconds,

            @Schema(description = "정원이 찬 뒤 최초 조회에서 저장한 참여자 최대 실제 이동시간(초)입니다. 직선거리 미리보기에서는 null입니다.", example = "1800")
            Long maxTravelTimeSeconds,

            @Schema(description = "상권과 매핑된 지하철역 정보입니다. 매핑이 없으면 null이며, 거리·좌표는 노출하지 않습니다.")
            StationResponse station
    ) {

        private static RecommendationResponse from(PlaceViewResult.Recommendation recommendation) {
            return new RecommendationResponse(
                    recommendation.rank(),
                    recommendation.areaCode(),
                    recommendation.areaName(),
                    recommendation.categoryName(),
                    recommendation.latitude(),
                    recommendation.longitude(),
                    recommendation.guName(),
                    recommendation.dongName(),
                    recommendation.averageStraightDistanceMeters(),
                    recommendation.averageTravelTimeSeconds(),
                    recommendation.maxTravelTimeSeconds(),
                    StationResponse.from(recommendation.station())
            );
        }
    }

    @Schema(description = "상권과 매핑된 지하철역과 호선")
    public record StationResponse(
            @Schema(description = "지하철역명", example = "강남역")
            String name,

            @Schema(description = "해당 역의 호선명 목록", example = "[\"2호선\", \"신분당선\"]")
            List<String> lineNames
    ) {

        private static StationResponse from(PlaceViewResult.Station station) {
            if (station == null) {
                return null;
            }
            return new StationResponse(station.name(), station.lineNames());
        }
    }
}
