# DB Diagram

> Purpose: Keep the current JPA entity/table structure copy-paste ready for [dbdiagram.io](https://dbdiagram.io).
> Update trigger: JPA entity, table name, column, index, unique constraint, or relationship changes.

## DBML

```dbml
Table users {
  id bigint [pk, increment]
  nickname varchar(30) [not null]
  created_at datetime [not null]
  updated_at datetime [not null]
  deleted_at datetime
}

Table login_accounts {
  id bigint [pk, increment]
  user_id bigint [not null, unique]
  login_id varchar(50) [not null, unique]
  password_hash varchar(100) [not null]
  created_at datetime [not null]

  indexes {
    login_id [unique, name: "uk_login_accounts_login_id"]
    user_id [unique, name: "uk_login_accounts_user_id"]
  }
}

Table social_accounts {
  id bigint [pk, increment]
  user_id bigint [not null]
  provider varchar(20) [not null]
  provider_user_id varchar(191) [not null]
  email varchar(255)
  created_at datetime [not null]

  indexes {
    (provider, provider_user_id) [unique, name: "uk_social_accounts_provider_user"]
    (user_id, provider) [unique, name: "uk_social_accounts_user_provider"]
  }
}

Table rooms {
  id bigint [pk, increment]
  host_user_id bigint [not null]
  name varchar(15) [not null]
  description varchar(100)
  max_participants int [not null]
  schedule_mode varchar(20) [not null]
  fixed_schedule_at datetime
  available_start_time time
  available_end_time time
  place_mode varchar(20) [not null]
  place_recommendation_strategy varchar(30)
  fixed_place_name varchar(100)
  fixed_place_address varchar(255)
  deadline_at datetime [not null]
  invite_code varchar(20) [not null, unique]
  created_at datetime [not null]
  updated_at datetime [not null]

  indexes {
    invite_code [unique, name: "uk_rooms_invite_code"]
  }
}

Table room_schedule_candidates {
  id bigint [pk, increment]
  room_id bigint [not null]
  candidate_date date [not null]

  indexes {
    (room_id, candidate_date) [unique, name: "uk_room_schedule_candidates_room_date"]
  }
}

Table room_participants {
  id bigint [pk, increment]
  room_id bigint [not null]
  user_id bigint
  nickname varchar(30) [not null]
  password_hash varchar(100)
  participant_type varchar(20) [not null]
  created_at datetime [not null]

  indexes {
    (room_id, nickname) [unique, name: "uk_room_participants_room_nickname"]
    (room_id, user_id) [unique, name: "uk_room_participants_room_user"]
  }
}

Ref fk_login_accounts_user: login_accounts.user_id - users.id
Ref fk_social_accounts_user: social_accounts.user_id > users.id
Ref fk_rooms_host_user: rooms.host_user_id > users.id
Ref fk_room_schedule_candidates_room: room_schedule_candidates.room_id > rooms.id
Ref fk_room_participants_room: room_participants.room_id > rooms.id
Ref fk_room_participants_user: room_participants.user_id > users.id
```

## Notes

- `users` is the service user table.
- `login_accounts` stores local login credentials separately from the user profile.
- `social_accounts` stores provider identity for Kakao/Apple-style social login.
- `social_accounts.provider_user_id` is the provider-issued user identifier, not CI/DI.
- `rooms` stores the first milestone room creation and invite code base.
- `rooms.schedule_mode` supports `VOTE`, `FIXED`, and `NONE`.
- `rooms.place_mode` supports `FIXED`, `RECOMMEND`, and `NONE`.
- `rooms.place_recommendation_strategy` is used only when `place_mode` is `RECOMMEND`.
- `rooms.deadline_at` is calculated by the server from request `deadlineMinutes`, which is currently accepted in 10-minute units up to 72 hours.
- `rooms.available_start_time` and `rooms.available_end_time` are shared by all schedule voting candidate dates and are currently accepted in 1-hour units.
- `room_schedule_candidates` stores variable-length date candidates for schedule voting.
- `room_participants` stores host and guest participants.
- `room_participants.nickname` is unique only inside a room.
- `room_participants.user_id` is unique only inside a room when a participant is linked to a service user.
- Guest participants do not use `users.id` yet; `room_participants.user_id` is nullable for guest participation.
