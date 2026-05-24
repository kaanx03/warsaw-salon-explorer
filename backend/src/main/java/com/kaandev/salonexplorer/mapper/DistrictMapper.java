package com.kaandev.salonexplorer.mapper;

import com.kaandev.salonexplorer.domain.dto.DistrictDto;
import com.kaandev.salonexplorer.domain.entity.District;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DistrictMapper {
    DistrictDto toDto(District district);
}
