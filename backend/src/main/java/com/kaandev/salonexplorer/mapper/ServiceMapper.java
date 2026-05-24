package com.kaandev.salonexplorer.mapper;

import com.kaandev.salonexplorer.domain.dto.ServiceDto;
import com.kaandev.salonexplorer.domain.entity.SalonService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ServiceMapper {

    @Mapping(target = "category", expression = "java(salonService.getCategory().name())")
    ServiceDto toDto(SalonService salonService);
}
