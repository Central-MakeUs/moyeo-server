package com.moyeo.repository.departure;

import com.moyeo.domain.departure.DeparturePlaceSearch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface DeparturePlaceSearchRepository extends JpaRepository<DeparturePlaceSearch, Long> {

    List<DeparturePlaceSearch> findAllByUserId(Long userId);

    List<DeparturePlaceSearch> findAllByMeetingIdIn(Collection<Long> meetingIds);
}
