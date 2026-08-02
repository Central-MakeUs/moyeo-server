package com.moyeo.controller.meeting;

import com.moyeo.service.meeting.ScheduleViewResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "일정 조율 현황 조회 응답")
public record ScheduleViewResponse(
        @Schema(description = "모임 ID", example = "1")
        Long meetingId,

        @Schema(description = "일정 참여 입력 유형", example = "DATE_AND_TIME", allowableValues = {"DATE_ONLY", "DATE_AND_TIME", "NONE"})
        String scheduleInputType,

        @Schema(description = "적용된 정렬 방식", example = "EARLIEST_DATE", allowableValues = {"LONGEST_MEETING", "EARLIEST_DATE"})
        String sort,

        @Schema(description = "현재 참여 인원. 방장을 포함합니다.", example = "4")
        long participantCount,

        @Schema(description = "2명 이상 참여 가능한 일정 후보 목록입니다. 가능 인원이 많은 후보부터 정렬 방식에 따라 최대 5개를 반환합니다.")
        List<CandidateResponse> candidates,

        @Schema(description = "일정 응답 현황 블록입니다. DATE_ONLY는 날짜별 블록이며 시작·종료 시간은 null입니다. DATE_AND_TIME은 같은 가능 참여자 집합의 연속 1시간 슬롯을 합쳐 반환합니다. 클라이언트는 availableParticipantCount / participantCount 비율로 화면 색상을 표시합니다.")
        List<AvailabilityStatusResponse> availabilityStatuses
) {

    public static ScheduleViewResponse from(ScheduleViewResult result) {
        return new ScheduleViewResponse(
                result.meetingId(),
                result.scheduleInputType(),
                result.sort(),
                result.participantCount(),
                result.candidates().stream().map(CandidateResponse::from).toList(),
                result.availabilityStatuses().stream().map(AvailabilityStatusResponse::from).toList()
        );
    }

    @Schema(description = "일정 후보")
    public record CandidateResponse(
            @Schema(description = "후보 날짜", example = "2026-07-12")
            LocalDate candidateDate,

            @Schema(description = "시작 시간. DATE_ONLY에서는 null입니다.", example = "18:00")
            LocalTime startTime,

            @Schema(description = "종료 시간. DATE_ONLY에서는 null입니다.", example = "19:00")
            LocalTime endTime,

            @Schema(description = "해당 날짜 또는 시간에 참여 가능한 인원 수", example = "3")
            long availableParticipantCount,

            @Schema(description = "해당 후보 시간에 가능한 참여자 목록입니다. 링크로 조회한 비로그인 사용자에게도 공개됩니다.")
            List<AvailableParticipantResponse> availableParticipants
    ) {

        private static CandidateResponse from(ScheduleViewResult.Candidate candidate) {
            return new CandidateResponse(
                    candidate.candidateDate(),
                    candidate.startTime(),
                    candidate.endTime(),
                    candidate.availableParticipantCount(),
                    candidate.availableParticipants().stream().map(AvailableParticipantResponse::from).toList()
            );
        }
    }

    @Schema(description = "일정 응답 현황 블록")
    public record AvailabilityStatusResponse(
            LocalDate candidateDate,
            LocalTime startTime,
            LocalTime endTime,
            long availableParticipantCount
    ) {
        private static AvailabilityStatusResponse from(ScheduleViewResult.AvailabilityStatus status) {
            return new AvailabilityStatusResponse(
                    status.candidateDate(),
                    status.startTime(),
                    status.endTime(),
                    status.availableParticipantCount()
            );
        }
    }

    @Schema(description = "일정 후보 참여 가능자")
    public record AvailableParticipantResponse(
            Long participantId,
            @Schema(description = "연결된 서비스 사용자 ID입니다. 게스트 참여자는 null입니다.", example = "42", nullable = true)
            Long userId,
            String nickname
    ) {
        private static AvailableParticipantResponse from(ScheduleViewResult.AvailableParticipant participant) {
            return new AvailableParticipantResponse(participant.participantId(), participant.userId(), participant.nickname());
        }
    }
}
