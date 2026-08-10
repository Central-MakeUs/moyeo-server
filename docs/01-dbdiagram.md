# DB Diagram

> Purpose: Keep the current JPA entity/table structure copy-paste ready for [dbdiagram.io](https://dbdiagram.io).
> Update trigger: JPA entity, table name, column, index, unique constraint, or relationship changes.

## DBML

```dbml
Table users {
  profile_color varchar(20) [not null, note: "회원 기본 프로필 색상: GRAY/RED/PURPLE/ORANGE"]
  id bigint [pk, increment, note: "서비스 사용자 ID"]
  nickname varchar(30) [note: "사용자 기본 닉네임. null이면 소셜 가입 후 온보딩 미완료 또는 탈퇴 상태"]
  created_at datetime [not null, note: "사용자 생성 일시"]
  updated_at datetime [not null, note: "사용자 정보 수정 일시"]
  deleted_at datetime [note: "사용자 탈퇴/삭제 일시. null이면 활성 상태"]
}

Table social_accounts {
  id bigint [pk, increment, note: "소셜 계정 연결 ID"]
  user_id bigint [not null, note: "연결된 서비스 사용자 ID"]
  provider varchar(20) [not null, note: "소셜 로그인 제공자"]
  provider_user_id varchar(191) [not null, note: "소셜 제공자가 발급한 사용자 식별자"]
  email varchar(255) [note: "소셜 제공자로부터 받은 이메일"]
  provider_refresh_token_ciphertext varchar(2048) [note: "서버 키로 암호화한 제공자 refresh token. 현재 Apple만 저장"]
  created_at datetime [not null, note: "소셜 계정 연결 생성 일시"]

  indexes {
    (provider, provider_user_id) [unique, name: "uk_social_accounts_provider_user"]
    (user_id, provider) [unique, name: "uk_social_accounts_user_provider"]
  }
}

Table saved_places {
  id bigint [pk, increment, note: "회원 저장 장소 ID"]
  user_id bigint [not null, note: "장소를 저장한 서비스 사용자 ID"]
  alias varchar(30) [not null, note: "회원이 입력한 장소 별칭"]
  category varchar(20) [not null, default: 'OTHER', note: "회원 지정 장소 용도: HOME/WORK/OTHER. 요청 생략 시 OTHER"]
  type varchar(20) [not null, note: "검색 결과 유형: STATION/ADDRESS/PLACE"]
  display_name varchar(255) [not null, note: "검색 결과의 원본 표시명"]
  address varchar(255) [not null, note: "대표 주소"]
  road_address varchar(255) [note: "도로명주소"]
  jibun_address varchar(255) [note: "지번주소"]
  latitude decimal(18,15) [not null, note: "WGS84 위도"]
  longitude decimal(18,15) [not null, note: "WGS84 경도"]
  created_at datetime [not null, note: "장소 저장 일시"]
  updated_at datetime [not null, note: "저장 장소 수정 일시"]

  indexes {
    (user_id, created_at, id) [name: "idx_saved_places_user_created"]
  }
}

Table feedbacks {
  id bigint [pk, increment, note: "피드백 ID"]
  user_id bigint [not null, note: "피드백을 제출한 서비스 사용자 ID"]
  content varchar(1000) [not null, note: "사용자가 제출한 피드백 내용. 1,000자는 임시 MVP 제한"]
  created_at datetime [not null, note: "피드백 제출 일시"]

  indexes {
    (user_id, created_at, id) [name: "idx_feedbacks_user_created"]
  }
}

Table commercial_areas {
  id bigint [pk, increment, note: "추천 후보 상권 내부 ID"]
  source varchar(40) [not null, note: "상권 데이터 출처"]
  external_code varchar(30) [not null, note: "출처가 부여한 상권 코드"]
  area_type varchar(30) [not null, note: "추천 대상 상권 유형: DEVELOPMENT/TOURIST_SPECIAL"]
  area_name varchar(255) [not null, note: "상권명"]
  latitude decimal(10,7) [not null, note: "상권 중심 WGS84 위도"]
  longitude decimal(10,7) [not null, note: "상권 중심 WGS84 경도"]
  district_code varchar(10) [note: "시군구 코드"]
  district_name varchar(40) [note: "시군구명"]
  administrative_dong_code varchar(12) [note: "행정동 코드"]
  administrative_dong_name varchar(40) [note: "행정동명"]

  indexes {
    (source, external_code) [unique, name: "uk_commercial_areas_source_external_code"]
    (source, area_type) [name: "idx_commercial_areas_source_type"]
  }
}

Table commercial_area_station_lines {
  id bigint [pk, increment, note: "추천 상권별 지하철역·호선 매핑 ID"]
  commercial_area_id bigint [not null, note: "추천 상권 내부 ID"]
  station_name varchar(100) [not null, note: "카카오 검증 역명"]
  line_name varchar(100) [not null, note: "카카오 검증 호선명"]
  station_address varchar(255) [not null, note: "카카오 검증 역 주소"]
  station_latitude decimal(18,15) [not null, note: "카카오 검증 역 WGS84 위도"]
  station_longitude decimal(18,15) [not null, note: "카카오 검증 역 WGS84 경도"]
  distance_meters int [not null, note: "상권 중심과 역 좌표의 직선거리 미터"]

  indexes {
    (commercial_area_id, station_name, line_name) [unique, name: "uk_commercial_area_station_lines_area_station_line"]
  }
}

Table meetings {
  id bigint [pk, increment, note: "모임 ID"]
  host_user_id bigint [not null, note: "모임을 만든 방장 사용자 ID"]
  name varchar(15) [not null, note: "모임 이름"]
  description varchar(100) [note: "모임 설명"]
  max_participants int [not null, note: "최대 참여 인원. 방장 포함. 비방장 회원 탈퇴·나가기 시 1명까지 감소"]
  planning_type varchar(30) [not null, note: "모임 생성 유형: SCHEDULE_ONLY/PLACE_ONLY/SCHEDULE_AND_PLACE"]
  schedule_mode varchar(20) [not null, note: "일정 설정 방식: VOTE/FIXED/NONE"]
  schedule_input_type varchar(20) [not null, note: "일정 참여 입력 유형: DATE_ONLY/DATE_AND_TIME/NONE"]
  fixed_schedule_at datetime [note: "확정 일정. schedule_mode가 FIXED일 때 사용"]
  available_start_time time [note: "일정 투표 공통 시작 시간. schedule_mode가 VOTE일 때 사용"]
  available_end_time time [note: "일정 투표 공통 종료 시간. schedule_mode가 VOTE일 때 사용"]
  place_mode varchar(20) [not null, note: "장소 설정 방식: FIXED/RECOMMEND/NONE"]
  place_recommendation_strategy varchar(30) [note: "장소 추천 방식. place_mode가 RECOMMEND이면 현재 MVP 생성 플로우에서 서버가 MIDDLE_POINT로 저장"]
  fixed_place_name varchar(100) [note: "확정 장소 이름. place_mode가 FIXED일 때 사용"]
  fixed_place_address varchar(255) [note: "확정 장소 주소. place_mode가 FIXED일 때 사용"]
  cover_image_key varchar(500) [note: "S3에 저장하는 모임 커버 이미지 객체 키"]
  deadline_at datetime [note: "모임 참여/응답 마감 일시. null이면 마감 없음"]
  invite_code varchar(20) [not null, unique, note: "초대 링크에 사용하는 고유 코드"]
  meeting_status varchar(20) [not null, note: "모임 상태: PLANNING/CONFIRMED"]
  confirmed_at datetime [note: "방장이 최종 확정한 일시"]
  confirmed_schedule_date date [note: "확정 일정 날짜"]
  confirmed_start_time time [note: "DATE_AND_TIME 확정 시작 시간"]
  confirmed_end_time time [note: "DATE_AND_TIME 확정 종료 시간"]
  confirmed_place_name varchar(255) [note: "확정 상권명 스냅샷"]
  confirmed_place_address varchar(255) [note: "확정 장소 주소 스냅샷. null 허용"]
  confirmed_place_latitude decimal(10,7) [note: "확정 장소 위도 스냅샷"]
  confirmed_place_longitude decimal(10,7) [note: "확정 장소 경도 스냅샷"]
  confirmed_commercial_area_code varchar(30) [note: "확정한 상권 후보 코드"]
  created_at datetime [not null, note: "모임 생성 일시"]
  updated_at datetime [not null, note: "모임 수정 일시"]

  indexes {
    invite_code [unique, name: "uk_meetings_invite_code"]
  }
}

Table meeting_cover_cleanup_tasks {
  id bigint [pk, increment, note: "모임 커버 이미지 정리 작업 ID"]
  object_key varchar(500) [not null, unique, note: "삭제할 S3 객체 키"]
  attempt_count int [not null, note: "S3 삭제 실패 횟수"]
  created_at datetime [not null, note: "정리 작업 생성 일시"]
  last_attempted_at datetime [note: "마지막 S3 삭제 시도 일시"]

  indexes {
    object_key [unique, name: "uk_meeting_cover_cleanup_tasks_object_key"]
  }
}

Table meeting_schedule_candidates {
  id bigint [pk, increment, note: "일정 후보 ID"]
  meeting_id bigint [not null, note: "일정 후보가 속한 모임 ID"]
  candidate_date date [not null, note: "일정 투표 후보 날짜"]

  indexes {
    (meeting_id, candidate_date) [unique, name: "uk_meeting_schedule_candidates_meeting_date"]
  }
}

Table meeting_schedule_candidate_availabilities {
  id bigint [pk, increment, note: "모임 후보 날짜 선택 가능 시간 스냅샷 ID"]
  schedule_candidate_id bigint [not null, note: "연결된 모임 후보 날짜 ID"]
  start_time time [not null, note: "참여자 선택 가능 시작 시간"]
  end_time time [not null, note: "참여자 선택 가능 종료 시간"]
  created_at datetime [not null, note: "스냅샷 생성 일시"]

  indexes {
    (schedule_candidate_id, start_time, end_time) [unique, name: "uk_meeting_schedule_candidate_availabilities_slot"]
  }
}

Table meeting_place_recommendation_snapshots {
  id bigint [pk, increment, note: "Full-meeting first actual-travel-time recommendation snapshot ID"]
  meeting_id bigint [not null, note: "Meeting that owns the snapshot"]
  rank int [not null, note: "Recommendation order starting at 1"]
  area_code varchar(30) [not null, note: "Commercial area source code snapshot"]
  area_name varchar(255) [not null, note: "Commercial area name snapshot"]
  category_name varchar(30) [not null, note: "Commercial area category snapshot"]
  latitude decimal(10,7) [not null, note: "Commercial area latitude snapshot"]
  longitude decimal(10,7) [not null, note: "Commercial area longitude snapshot"]
  gu_name varchar(40) [note: "District name snapshot"]
  dong_name varchar(40) [note: "Administrative dong name snapshot"]
  average_straight_distance_meters bigint [note: "Preliminary average straight-line distance"]
  average_travel_time_seconds bigint [not null, note: "Average actual travel time"]
  max_travel_time_seconds bigint [not null, note: "Maximum actual travel time"]

  indexes {
    (meeting_id, rank) [unique, name: "uk_meeting_place_recommendation_snapshots_meeting_rank"]
  }
}

Table meeting_participant_schedule_availabilities {
  id bigint [pk, increment, note: "참여자 일정 가능 시간 ID"]
  participant_id bigint [not null, note: "일정 가능 시간을 입력한 참여자 ID"]
  schedule_candidate_id bigint [not null, note: "일정 후보 날짜 ID"]
  start_time time [not null, note: "가능 시간 시작"]
  end_time time [not null, note: "가능 시간 종료"]
  created_at datetime [not null, note: "일정 가능 시간 생성 일시"]

  indexes {
    (participant_id, schedule_candidate_id, start_time, end_time) [unique, name: "uk_meeting_participant_schedule_availabilities_slot"]
  }
}

Table meeting_participant_schedule_date_availabilities {
  id bigint [pk, increment, note: "참여자 일정 가능 날짜 ID"]
  participant_id bigint [not null, note: "가능 날짜를 입력한 참여자 ID"]
  schedule_candidate_id bigint [not null, note: "선택한 일정 후보 날짜 ID"]
  created_at datetime [not null, note: "가능 날짜 생성 일시"]

  indexes {
    (participant_id, schedule_candidate_id) [unique, name: "uk_meeting_participant_schedule_date_availabilities_date"]
  }
}

Table meeting_participants {
  id bigint [pk, increment, note: "모임 참여자 ID"]
  meeting_id bigint [not null, note: "참여한 모임 ID"]
  user_id bigint [note: "연결된 서비스 사용자 ID. 게스트는 null"]
  nickname varchar(30) [not null, note: "모임 안에서 표시할 닉네임"]
  password_hash varchar(100) [note: "참여 비밀번호 해시. 방장은 null"]
  participant_type varchar(20) [not null, note: "참여자 타입: HOST/MEMBER/GUEST"]
  departure_name varchar(30) [note: "방장 또는 참여자 출발지 선택 표시 이름. 요청에서 생략 가능"]
  departure_address varchar(255) [note: "방장 또는 참여자 출발지 주소. 중간지점 추천에서 사용"]
  departure_latitude decimal(10,7) [note: "방장 또는 참여자 출발지 위도. 중간지점 추천에서 사용"]
  departure_longitude decimal(10,7) [note: "방장 또는 참여자 출발지 경도. 중간지점 추천에서 사용"]
  transportation_mode varchar(20) [note: "중간지점 추천에 사용할 이동수단: PUBLIC_TRANSIT/CAR"]
  created_at datetime [not null, note: "참여 생성 일시"]

  indexes {
    (meeting_id, user_id) [unique, name: "uk_meeting_participants_meeting_user"]
  }
}

Ref fk_social_accounts_user: social_accounts.user_id > users.id
Ref fk_saved_places_user: saved_places.user_id > users.id
Ref fk_feedbacks_user: feedbacks.user_id > users.id
Ref fk_commercial_area_station_lines_area: commercial_area_station_lines.commercial_area_id > commercial_areas.id
Ref fk_meetings_host_user: meetings.host_user_id > users.id
Ref fk_meeting_schedule_candidates_meeting: meeting_schedule_candidates.meeting_id > meetings.id
Ref fk_meeting_schedule_candidate_availabilities_candidate: meeting_schedule_candidate_availabilities.schedule_candidate_id > meeting_schedule_candidates.id
Ref fk_meeting_place_recommendation_snapshots_meeting: meeting_place_recommendation_snapshots.meeting_id > meetings.id
Ref fk_meeting_participants_meeting: meeting_participants.meeting_id > meetings.id
Ref fk_meeting_participants_user: meeting_participants.user_id > users.id
Ref fk_meeting_participant_schedule_availabilities_participant: meeting_participant_schedule_availabilities.participant_id > meeting_participants.id
Ref fk_meeting_participant_schedule_availabilities_candidate: meeting_participant_schedule_availabilities.schedule_candidate_id > meeting_schedule_candidates.id
Ref fk_meeting_participant_schedule_date_availabilities_participant: meeting_participant_schedule_date_availabilities.participant_id > meeting_participants.id
Ref fk_meeting_participant_schedule_date_availabilities_candidate: meeting_participant_schedule_date_availabilities.schedule_candidate_id > meeting_schedule_candidates.id
```

## Notes

- Final-confirmation implementation adds `meetings.meeting_status` (`PLANNING`/
  `CONFIRMED`), `confirmed_at`, selected schedule date/start/end fields, and
  nullable confirmed commercial-area snapshot fields (name, address, coordinates,
  and area code). Confirmation preserves `deadline_at`.

- `users` is the service user table.
- `users.nickname` is null while a newly registered social user has not
  completed nickname onboarding or after the user has withdrawn. Active-user
  lookup distinguishes these states through `users.deleted_at`; a separate
  onboarding flag is not stored.
- `social_accounts` stores provider identity for Kakao/Apple-style social login.
  Apple refresh tokens are stored only as AES-256-GCM ciphertext bound to the
  Apple provider user ID; Kakao rows keep this field null.
- `social_accounts.provider_user_id` is the provider-issued user identifier, not CI/DI.
- Social accounts are never merged automatically by email.
- `saved_places` stores member-owned place snapshots. It allows duplicates, has no count limit, and is listed by newest `created_at` with `id` as a tie-breaker.
- `saved_places.category` stores the member-selected icon category: `HOME`, `WORK`, or `OTHER`. Existing saved places are migrated as `OTHER`.
- `saved_places.alias` is the only mutable place field; replacing the selected location creates a new saved place.
- `feedbacks` stores authenticated members' submitted feedback with the submitting user, content, and submission time. Members can retrieve only their own history; feedback is deleted when its submitting member withdraws; no operator-facing feedback API is defined yet.
- `commercial_areas` stores source-owned recommendation candidates independently from meetings. The initial seed contains only Seoul development areas and tourist-special areas from `SEOUL_COMMERCIAL_ANALYSIS`; later regional sources can use the same table through a different `source` value and source-owned code.
- `commercial_areas.latitude` and `commercial_areas.longitude` are WGS84 center coordinates converted once during import from the source coordinate system, so route-provider calls do not transform coordinates at request time.
- `commercial_area_station_lines` stores the verified Seoul subway station and line rows for a commercial area. One commercial area may have multiple rows for transfer lines; `distance_meters` is the straight-line distance between the commercial-area center and the station coordinate, not travel time.
- `meetings` stores the first milestone meeting creation and invite code base.
- `meetings.planning_type` stores the FAB-selected creation type: `SCHEDULE_ONLY`, `PLACE_ONLY`, or `SCHEDULE_AND_PLACE`.
- `meetings.schedule_mode` supports `VOTE`, `FIXED`, and `NONE`.
- `meetings.schedule_input_type` explicitly stores whether schedule participation selects dates only (`DATE_ONLY`), date/time ranges (`DATE_AND_TIME`), or no schedule (`NONE`); clients and the server do not infer this from nullable time columns.
- `meetings.place_mode` supports `FIXED`, `RECOMMEND`, and `NONE`.
- `meetings.place_recommendation_strategy` stores the recommendation strategy when `place_mode` is `RECOMMEND`; the current MVP creation flow stores `MIDDLE_POINT` server-side, while retaining the column for a later product-approved strategy change.
- `meetings.cover_image_key` stores the S3 object key for the resized optional meeting cover image; the original upload is not retained.
- `meeting_cover_cleanup_tasks` keeps failed cover deletions durable across S3
  failures and process restarts for account withdrawal, cover replacement,
  and cover deletion by storing the task in the same transaction as the local
  change. Transaction-rollback callbacks also queue failed immediate deletions.
  A successful idempotent S3 deletion removes the task; failed attempts remain
  for scheduled retry.
- `meetings.deadline_at` is calculated by the server from request `deadlineMinutes` when `noDeadline` is false or omitted. `noDeadline=true` stores null and means there is no participation/response deadline. A present `deadlineMinutes` is accepted in 10-minute units from 10 minutes up to 7 days 23 hours.
- `meetings.available_start_time` and `meetings.available_end_time` are used only for `DATE_AND_TIME`, are shared by all schedule voting candidate dates, and are currently accepted in 1-hour units. They remain null for `DATE_ONLY` and `NONE`.
- `meeting_schedule_candidates` stores variable-length date candidates for schedule voting.
- `meeting_schedule_candidate_availabilities` stores the host-selected time ranges copied at `DATE_AND_TIME` meeting creation. Invite lookup exposes only candidate dates with these snapshots, so later changes to the host's personal response do not change the participant selection range.
- `meeting_place_recommendation_snapshots` stores the first actual-travel-time
  recommendation result once a place-recommendation meeting reaches capacity.
  The snapshot keeps the returned place data, preliminary straight-line distance,
  and average/maximum travel time so later place-view requests do not call Kakao
  again.
- `meeting_participant_schedule_availabilities` stores participant-selected availability slots. For `DATE_AND_TIME`, meeting creation saves the host-selected ranges in the same transaction as the meeting and host row.
- `meeting_participant_schedule_date_availabilities` stores the selected dates for `DATE_ONLY`. Meeting creation also saves the host's candidate dates as the host's available dates.
- `meeting_participants` stores host, logged-in member, and guest participants. Creation inserts the host row and its required schedule/departure response data atomically.
- `meeting_participants.departure_name`, `departure_address`, `departure_latitude`, `departure_longitude`, and `transportation_mode` store host and participant departure snapshots for place coordination.
- Guest `meeting_participants.nickname` duplication is rejected only against other guests in the same meeting by the join application logic; the table does not keep a general nickname unique constraint.
- `meeting_participants.user_id` is unique only inside a meeting when a participant is linked to a service user.
- Logged-in member participants use `users.id`; guest participants keep `meeting_participants.user_id` null.
