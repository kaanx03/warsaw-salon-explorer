package com.kaandev.salonexplorer.mapper;

import com.kaandev.salonexplorer.domain.dto.ServiceDto;
import com.kaandev.salonexplorer.domain.entity.SalonService;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ServiceMapper {
    ServiceDto toDto(SalonService salonService);
}
