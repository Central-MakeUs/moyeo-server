package com.moyeo.repository.place;

import com.moyeo.domain.place.SavedPlace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedPlaceRepository extends JpaRepository<SavedPlace, Long> {

    List<SavedPlace> findAllByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    Optional<SavedPlace> findByIdAndUserId(Long id, Long userId);
}
