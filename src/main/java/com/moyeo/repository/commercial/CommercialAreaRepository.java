package com.moyeo.repository.commercial;

import com.moyeo.domain.commercial.CommercialAreaEntity;
import com.moyeo.domain.commercial.CommercialAreaSource;
import com.moyeo.domain.commercial.CommercialAreaType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CommercialAreaRepository extends JpaRepository<CommercialAreaEntity, Long> {

    List<CommercialAreaEntity> findAllBySourceAndAreaTypeInOrderByExternalCodeAsc(
            CommercialAreaSource source,
            Collection<CommercialAreaType> areaTypes
    );

    List<CommercialAreaEntity> findAllBySourceInAndAreaTypeInOrderBySourceAscExternalCodeAsc(
            Collection<CommercialAreaSource> sources,
            Collection<CommercialAreaType> areaTypes
    );

    long countBySource(CommercialAreaSource source);
}
