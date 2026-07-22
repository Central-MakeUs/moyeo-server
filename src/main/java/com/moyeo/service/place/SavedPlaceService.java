package com.moyeo.service.place;

import com.moyeo.domain.place.SavedPlace;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.repository.member.UserRepository;
import com.moyeo.repository.place.SavedPlaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SavedPlaceService {

    private final SavedPlaceRepository savedPlaceRepository;
    private final UserRepository userRepository;

    public SavedPlaceService(SavedPlaceRepository savedPlaceRepository, UserRepository userRepository) {
        this.savedPlaceRepository = savedPlaceRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public SavedPlaceResult save(Long userId, SavePlaceCommand command) {
        SavedPlace place = new SavedPlace(
                userRepository.getReferenceById(userId),
                command.alias(),
                command.type(),
                command.displayName(),
                command.address(),
                command.roadAddress(),
                command.jibunAddress(),
                command.latitude(),
                command.longitude()
        );
        return SavedPlaceResult.from(savedPlaceRepository.save(place));
    }

    @Transactional(readOnly = true)
    public List<SavedPlaceResult> findAll(Long userId) {
        return savedPlaceRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(SavedPlaceResult::from)
                .toList();
    }

    @Transactional
    public SavedPlaceResult rename(Long userId, Long savedPlaceId, String alias) {
        SavedPlace place = findOwnedPlace(userId, savedPlaceId);
        place.rename(alias);
        savedPlaceRepository.flush();
        return SavedPlaceResult.from(place);
    }

    @Transactional
    public void delete(Long userId, Long savedPlaceId) {
        savedPlaceRepository.delete(findOwnedPlace(userId, savedPlaceId));
    }

    private SavedPlace findOwnedPlace(Long userId, Long savedPlaceId) {
        return savedPlaceRepository.findByIdAndUserId(savedPlaceId, userId)
                .orElseThrow(() -> new MoyeoException(SavedPlaceErrorCode.SAVED_PLACE_NOT_FOUND));
    }
}
