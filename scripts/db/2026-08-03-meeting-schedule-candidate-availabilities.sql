create table meeting_schedule_candidate_availabilities (
    id bigint not null auto_increment,
    schedule_candidate_id bigint not null,
    start_time time not null,
    end_time time not null,
    created_at datetime not null,
    primary key (id),
    constraint uk_meeting_schedule_candidate_availabilities_slot
        unique (schedule_candidate_id, start_time, end_time),
    constraint fk_meeting_schedule_candidate_availabilities_candidate
        foreign key (schedule_candidate_id) references meeting_schedule_candidates (id)
);

insert into meeting_schedule_candidate_availabilities (
    schedule_candidate_id,
    start_time,
    end_time,
    created_at
)
select
    availability.schedule_candidate_id,
    availability.start_time,
    availability.end_time,
    availability.created_at
from meeting_participant_schedule_availabilities availability
join meeting_participants participant on participant.id = availability.participant_id
where participant.participant_type = 'HOST';
