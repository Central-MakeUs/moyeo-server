package com.moyeo.controller.room;

import com.moyeo.global.security.CurrentMember;
import com.moyeo.service.member.AuthenticatedMember;
import com.moyeo.service.room.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
@Tag(name = "Room", description = "모임 생성, 초대 코드 조회, 게스트 참여 API")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "모임 생성",
            description = """
                    로그인한 사용자가 방장이 되어 모임을 생성하고 초대 코드를 발급받습니다.<br>
                    프론트 화면의 STEP 1~4 입력값을 마지막 링크 생성 시 한 번에 전송합니다.
                    <ul>
                      <li>STEP 1 기본 정보: name, description, maxParticipants</li>
                      <li>STEP 2 일정 설정: scheduleMode에 따라 VOTE/FIXED/NONE 중 하나 사용</li>
                      <li>STEP 3 장소 설정: placeMode에 따라 RECOMMEND/FIXED/NONE 중 하나 사용</li>
                      <li>STEP 4 마감 설정: deadlineMinutes를 보내면 서버가 deadlineAt을 계산</li>
                    </ul>
                    scheduleMode와 placeMode는 서로 독립적으로 조합할 수 있습니다.<br>
                    예: scheduleMode=VOTE + placeMode=NONE, scheduleMode=FIXED + placeMode=RECOMMEND<br>
                    사용하지 않는 모드의 필드는 보내지 않거나 null로 보내도 됩니다.<br>
                    예: scheduleMode=VOTE이면 fixedScheduleAt은 사용하지 않고,
                    placeMode=RECOMMEND이면 fixedPlaceName/fixedPlaceAddress는 사용하지 않습니다.
                    """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "STEP 1~4: 일정 투표 + 장소 추천",
                                            description = "후보 날짜들과 공통 시간대를 받고, 참여자 출발지를 기반으로 장소 추천을 진행하는 흐름입니다.",
                                            value = """
                                                    {
                                                      "name": "토요일 저녁 모임",
                                                      "description": "오랜만에 같이 저녁 먹어요.",
                                                      "maxParticipants": 6,
                                                      "scheduleMode": "VOTE",
                                                      "scheduleCandidateDates": [
                                                        "2026-07-04",
                                                        "2026-07-05"
                                                      ],
                                                      "availableStartTime": "18:00",
                                                      "availableEndTime": "22:00",
                                                      "placeMode": "RECOMMEND",
                                                      "placeRecommendationStrategy": "MIDDLE_POINT",
                                                      "deadlineMinutes": 1440
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "STEP 1~4: 일정 확정 + 장소 확정",
                                            description = "모임장이 일정과 장소를 이미 정한 상태로 방을 만드는 흐름입니다.",
                                            value = """
                                                    {
                                                      "name": "강남 점심 모임",
                                                      "description": "점심 먹고 카페까지 가요.",
                                                      "maxParticipants": 4,
                                                      "scheduleMode": "FIXED",
                                                      "fixedScheduleAt": "2026-07-04T12:00:00",
                                                      "placeMode": "FIXED",
                                                      "fixedPlaceName": "강남역",
                                                      "fixedPlaceAddress": "서울 강남구 강남대로 지하 396",
                                                      "deadlineMinutes": 180
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "STEP 1~4: 일정/장소 건너뛰기",
                                            description = "일정과 장소를 아직 정하지 않고 기본 정보와 마감만 설정하는 흐름입니다.",
                                            value = """
                                                    {
                                                      "name": "번개 모임",
                                                      "description": "일정과 장소는 나중에 정해요.",
                                                      "maxParticipants": 8,
                                                      "scheduleMode": "NONE",
                                                      "placeMode": "NONE",
                                                      "deadlineMinutes": 720
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "모임 생성 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청값 검증 실패",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "COMMON_VALIDATION_FAILED",
                              "status": 400
                            }
                            """))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Access Token 없음, 만료 또는 유효하지 않음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "AUTHENTICATION_REQUIRED",
                              "status": 401
                            }
                            """))
            )
    })
    public CreateRoomResponse createRoom(
            @Parameter(hidden = true) @CurrentMember AuthenticatedMember member,
            @Valid @RequestBody CreateRoomRequest request
    ) {
        return CreateRoomResponse.from(roomService.createRoom(member, request.toCommand()));
    }

    @GetMapping("/invitations/{inviteCode}")
    @Operation(
            summary = "초대 코드로 모임 조회",
            description = "초대 링크 진입 화면에서 사용할 모임 기본 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "초대 코드 모임 조회 성공"),
            @ApiResponse(
                    responseCode = "404",
                    description = "초대 코드에 해당하는 모임 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "ROOM_INVITATION_NOT_FOUND",
                              "status": 404
                            }
                            """))
            )
    })
    public RoomInvitationResponse getInvitation(@PathVariable String inviteCode) {
        return RoomInvitationResponse.from(roomService.getInvitation(inviteCode));
    }

    @PostMapping("/invitations/{inviteCode}/guests")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "게스트 모임 참여",
            description = "초대 코드로 들어온 게스트가 닉네임과 비밀번호를 입력해 모임에 참여합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "게스트 참여 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청값 검증 실패",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "COMMON_VALIDATION_FAILED",
                              "status": 400
                            }
                            """))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "초대 코드에 해당하는 모임 없음",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "code": "ROOM_INVITATION_NOT_FOUND",
                              "status": 404
                            }
                            """))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "닉네임 중복, 모임 인원 초과 또는 참여 마감",
                    content = @Content(examples = {
                            @ExampleObject(
                                    name = "모임 참여 마감",
                                    value = """
                                            {
                                              "code": "ROOM_PARTICIPATION_CLOSED",
                                              "status": 409
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "모임 인원 초과",
                                    value = """
                                            {
                                              "code": "ROOM_PARTICIPANT_LIMIT_EXCEEDED",
                                              "status": 409
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "모임 안 닉네임 중복",
                                    value = """
                                            {
                                              "code": "DUPLICATE_ROOM_PARTICIPANT_NICKNAME",
                                              "status": 409
                                            }
                                            """
                            )
                    })
            )
    })
    public GuestJoinResponse joinGuest(
            @PathVariable String inviteCode,
            @Valid @RequestBody GuestJoinRequest request
    ) {
        return GuestJoinResponse.from(roomService.joinGuest(inviteCode, request.nickname(), request.password()));
    }
}
