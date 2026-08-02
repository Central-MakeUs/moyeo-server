package com.moyeo.repository.commercial;

import com.moyeo.domain.commercial.CommercialAreaStationLineEntity;
import com.moyeo.domain.commercial.CommercialAreaSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommercialAreaStationLineRepository extends JpaRepository<CommercialAreaStationLineEntity, Long> {

    long countByCommercialArea_Source(CommercialAreaSource source);

    @Query("""
            select stationLine
            from CommercialAreaStationLineEntity stationLine
            join fetch stationLine.commercialArea commercialArea
            where commercialArea.source = :source
              and commercialArea.externalCode in :areaCodes
            order by commercialArea.externalCode, stationLine.stationName, stationLine.lineName
            """)
    List<CommercialAreaStationLineEntity> findAllForCommercialAreaCodes(
            @Param("source") CommercialAreaSource source,
            @Param("areaCodes") Collection<String> areaCodes
    );
}
