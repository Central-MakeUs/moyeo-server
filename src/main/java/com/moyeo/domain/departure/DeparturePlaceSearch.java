package com.moyeo.domain.departure;

import com.moyeo.domain.meeting.Meeting;
import com.moyeo.domain.member.User;
import com.moyeo.departure.DeparturePlaceType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Comment("출발지 검색 실행")
@Check(constraints = "(user_id is not null and meeting_id is null) or (user_id is null and meeting_id is not null)")
@Table(name = "departure_place_searches")
public class DeparturePlaceSearch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("출발지 검색 실행 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_departure_place_searches_user"))
    @Comment("검색한 서비스 사용자 ID. 게스트 검색은 null")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", foreignKey = @ForeignKey(name = "fk_departure_place_searches_meeting"))
    @Comment("게스트 검색이 발생한 모임 ID. 회원 검색은 null")
    private Meeting meeting;

    @Column(nullable = false, length = 100)
    @Comment("외부 검색 API에 전달한 정규화된 검색어")
    private String keyword;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Comment("검색 제공자: KAKAO_LOCAL")
    private DeparturePlaceSearchProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_path", nullable = false, length = 40)
    @Comment("검색 및 fallback 실행 경로")
    private DeparturePlaceSearchExecutionPath executionPath;

    @Column(nullable = false)
    @Comment("검색 실행 기록 일시")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "search", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeparturePlaceSearchCandidate> candidates = new ArrayList<>();

    protected DeparturePlaceSearch() {
    }

    private DeparturePlaceSearch(
            User user,
            Meeting meeting,
            String keyword,
            DeparturePlaceSearchProvider provider,
            DeparturePlaceSearchExecutionPath executionPath
    ) {
        if ((user == null) == (meeting == null)) {
            throw new IllegalArgumentException("검색 실행은 사용자 또는 모임 중 하나에만 연결되어야 합니다.");
        }
        this.user = user;
        this.meeting = meeting;
        this.keyword = keyword;
        this.provider = provider;
        this.executionPath = executionPath;
    }

    public static DeparturePlaceSearch member(
            User user,
            String keyword,
            DeparturePlaceSearchProvider provider,
            DeparturePlaceSearchExecutionPath executionPath
    ) {
        return new DeparturePlaceSearch(user, null, keyword, provider, executionPath);
    }

    public static DeparturePlaceSearch guest(
            Meeting meeting,
            String keyword,
            DeparturePlaceSearchProvider provider,
            DeparturePlaceSearchExecutionPath executionPath
    ) {
        return new DeparturePlaceSearch(null, meeting, keyword, provider, executionPath);
    }

    public void addCandidate(
            int position,
            DeparturePlaceType type,
            String displayName,
            String address,
            String roadAddress,
            String jibunAddress,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        candidates.add(new DeparturePlaceSearchCandidate(
                this,
                position,
                type,
                displayName,
                address,
                roadAddress,
                jibunAddress,
                latitude,
                longitude
        ));
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Meeting getMeeting() {
        return meeting;
    }

    public String getKeyword() {
        return keyword;
    }

    public DeparturePlaceSearchProvider getProvider() {
        return provider;
    }

    public DeparturePlaceSearchExecutionPath getExecutionPath() {
        return executionPath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<DeparturePlaceSearchCandidate> getCandidates() {
        return Collections.unmodifiableList(candidates);
    }
}
