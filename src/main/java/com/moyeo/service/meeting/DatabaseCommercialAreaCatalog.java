package com.moyeo.service.meeting;

import com.moyeo.domain.commercial.CommercialAreaSource;
import com.moyeo.domain.commercial.CommercialAreaType;
import com.moyeo.repository.commercial.CommercialAreaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatabaseCommercialAreaCatalog implements CommercialAreaCatalog {

    private final CommercialAreaRepository commercialAreaRepository;

    public DatabaseCommercialAreaCatalog(CommercialAreaRepository commercialAreaRepository) {
        this.commercialAreaRepository = commercialAreaRepository;
    }

    @Override
    public List<CommercialArea> findAll() {
        return commercialAreaRepository.findAllBySourceAndAreaTypeInOrderByExternalCodeAsc(
                        CommercialAreaSource.SEOUL_COMMERCIAL_ANALYSIS,
                        List.of(CommercialAreaType.DEVELOPMENT, CommercialAreaType.TOURIST_SPECIAL)
                )
                .stream()
                .map(area -> new CommercialArea(
                        area.getExternalCode(),
                        area.getAreaName(),
                        area.getAreaType().getDisplayName(),
                        area.getLatitude(),
                        area.getLongitude(),
                        area.getDistrictName(),
                        area.getAdministrativeDongName()
                ))
                .toList();
    }
}
