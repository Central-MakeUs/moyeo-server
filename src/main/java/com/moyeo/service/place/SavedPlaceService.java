package com.moyeo.service.place;

import com.moyeo.domain.member.User;
import com.moyeo.domain.place.SavedPlace;
import com.moyeo.domain.place.SavedPlaceCategory;
import com.moyeo.global.error.MoyeoException;
import com.moyeo.global.security.AuthenticationErrorCode;
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
                findActiveUserForUpdate(userId),
                command.alias(),
                command.category() == null ? SavedPlaceCategory.OTHER : command.category(),
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
        findActiveUserForUpdate(userId);
        SavedPlace place = findOwnedPlace(userId, savedPlaceId);
        place.rename(alias);
        savedPlaceRepository.flush();
        return SavedPlaceResult.from(place);
    }

    @Transactional
    public void delete(Long userId, Long savedPlaceId) {
        findActiveUserForUpdate(userId);
        savedPlaceRepository.delete(findOwnedPlace(userId, savedPlaceId));
    }

    private User findActiveUserForUpdate(Long userId) {
        return userRepository.findActiveByIdForUpdate(userId)
                .orElseThrow(() -> new MoyeoException(AuthenticationErrorCode.AUTHENTICATION_REQUIRED));
    }

    private SavedPlace findOwnedPlace(Long userId, Long savedPlaceId) {
        return savedPlaceRepository.findByIdAndUserId(savedPlaceId, userId)
                .orElseThrow(() -> new MoyeoException(SavedPlaceErrorCode.SAVED_PLACE_NOT_FOUND));
    }
}
