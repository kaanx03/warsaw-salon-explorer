package com.kaandev.salonexplorer.mapper;

import com.kaandev.salonexplorer.domain.dto.SalonDetailDto;
import com.kaandev.salonexplorer.domain.dto.SalonListItemDto;
import com.kaandev.salonexplorer.domain.dto.SalonPatchRequest;
import com.kaandev.salonexplorer.domain.dto.SalonUpdateRequest;
import com.kaandev.salonexplorer.domain.entity.Salon;
import org.mapstruct.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Mapper(
    componentModel = "spring",
    uses = { DistrictMapper.class, ServiceMapper.class },
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface SalonMapper {

    @Mapping(target = "district", source = "district.name")
    @Mapping(target = "photoUrl", source = "photoUrl", qualifiedByName = "photoProxy")
    SalonListItemDto toListItem(Salon salon);

    @Mapping(target = "photoUrl", source = "photoUrl", qualifiedByName = "photoProxy")
    SalonDetailDto toDetail(Salon salon);

    @Mapping(target = "id",            ignore = true)
    @Mapping(target = "googlePlaceId", ignore = true)
    @Mapping(target = "createdAt",     ignore = true)
    @Mapping(target = "updatedAt",     ignore = true)
    @Mapping(target = "district",      ignore = true)
    @Mapping(target = "services",      ignore = true)
    @Mapping(target = "photoUrl",      ignore = true)
    @Mapping(target = "latitude",      ignore = true)
    @Mapping(target = "longitude",     ignore = true)
    void updateFromRequest(SalonUpdateRequest req, @MappingTarget Salon salon);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",            ignore = true)
    @Mapping(target = "googlePlaceId", ignore = true)
    @Mapping(target = "createdAt",     ignore = true)
    @Mapping(target = "updatedAt",     ignore = true)
    @Mapping(target = "district",      ignore = true)
    @Mapping(target = "services",      ignore = true)
    @Mapping(target = "photoUrl",      ignore = true)
    @Mapping(target = "latitude",      ignore = true)
    @Mapping(target = "longitude",     ignore = true)
    void patchFromRequest(SalonPatchRequest req, @MappingTarget Salon salon);

    @Named("photoProxy")
    default String toPhotoProxyUrl(String photoRef) {
        if (photoRef == null) return null;
        return "/api/v1/photos/" + URLEncoder.encode(photoRef, StandardCharsets.UTF_8);
    }
}
