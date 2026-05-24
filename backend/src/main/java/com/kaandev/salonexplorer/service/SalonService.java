package com.kaandev.salonexplorer.service;

import com.kaandev.salonexplorer.domain.dto.PagedResponse;
import com.kaandev.salonexplorer.domain.dto.SalonDetailDto;
import com.kaandev.salonexplorer.domain.dto.SalonListItemDto;
import com.kaandev.salonexplorer.domain.dto.SalonPatchRequest;
import com.kaandev.salonexplorer.domain.dto.SalonUpdateRequest;
import com.kaandev.salonexplorer.domain.entity.Salon;
import com.kaandev.salonexplorer.domain.specification.SalonSpecifications;
import com.kaandev.salonexplorer.exception.ResourceNotFoundException;
import com.kaandev.salonexplorer.mapper.SalonMapper;
import com.kaandev.salonexplorer.repository.DistrictRepository;
import com.kaandev.salonexplorer.repository.SalonRepository;
import com.kaandev.salonexplorer.repository.SalonServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class SalonService {

    private final SalonRepository salonRepository;
    private final DistrictRepository districtRepository;
    private final SalonServiceRepository salonServiceRepository;
    private final SalonMapper mapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PagedResponse<SalonListItemDto> list(
        String districtSlug,
        String serviceName,
        BigDecimal minRating,
        Short maxPriceLevel,
        String search,
        Pageable pageable
    ) {
        Specification<Salon> spec = Specification.allOf(
            SalonSpecifications.isActive(),
            SalonSpecifications.hasDistrictSlug(districtSlug),
            SalonSpecifications.hasService(serviceName),
            SalonSpecifications.minRating(minRating),
            SalonSpecifications.maxPriceLevel(maxPriceLevel),
            SalonSpecifications.nameContains(search)
        );

        Page<SalonListItemDto> page = salonRepository.findAll(spec, pageable)
            .map(mapper::toListItem);

        return PagedResponse.from(page);
    }

    @Cacheable(value = "salonDetail", key = "#id")
    @Transactional(readOnly = true)
    public SalonDetailDto getById(Long id) {
        Salon salon = salonRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Salon not found: " + id));
        return mapper.toDetail(salon);
    }

    @CacheEvict(value = "salonDetail", key = "#id")
    @Transactional
    public SalonDetailDto update(Long id, SalonUpdateRequest request) {
        Salon salon = salonRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Salon not found: " + id));

        var before = mapper.toDetail(salon);
        mapper.updateFromRequest(request, salon);
        applyDistrict(salon, request.districtId());
        applyServices(salon, request.serviceIds());

        Salon saved = salonRepository.save(salon);
        var after = mapper.toDetail(saved);
        auditService.logUpdate("Salon", id, before, after);
        log.info("Updated salon id={}", id);
        return after;
    }

    @CacheEvict(value = "salonDetail", key = "#id")
    @Transactional
    public SalonDetailDto patch(Long id, SalonPatchRequest request) {
        Salon salon = salonRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Salon not found: " + id));

        var before = mapper.toDetail(salon);
        mapper.patchFromRequest(request, salon);
        if (request.districtId() != null) applyDistrict(salon, request.districtId());
        if (request.serviceIds()  != null) applyServices(salon, request.serviceIds());

        Salon saved = salonRepository.save(salon);
        var after = mapper.toDetail(saved);
        auditService.logUpdate("Salon", id, before, after);
        log.info("Patched salon id={}", id);
        return after;
    }

    @CacheEvict(value = "salonDetail", key = "#id")
    @Transactional
    public void softDelete(Long id) {
        Salon salon = salonRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Salon not found: " + id));
        salon.setIsActive(false);
        salonRepository.save(salon);
        auditService.logDelete("Salon", id);
        log.info("Soft-deleted salon id={}", id);
    }

    private void applyDistrict(Salon salon, Long districtId) {
        if (districtId == null) {
            salon.setDistrict(null);
            return;
        }
        var district = districtRepository.findById(districtId)
            .orElseThrow(() -> new ResourceNotFoundException("District not found: " + districtId));
        salon.setDistrict(district);
    }

    private void applyServices(Salon salon, Set<Long> serviceIds) {
        if (serviceIds == null) return;
        Set<com.kaandev.salonexplorer.domain.entity.SalonService> services =
            new HashSet<>(salonServiceRepository.findAllById(serviceIds));
        if (services.size() != serviceIds.size()) {
            throw new ResourceNotFoundException("One or more services not found");
        }
        salon.setServices(services);
    }
}
