package com.kaandev.salonexplorer.service;

import com.kaandev.salonexplorer.domain.dto.ServiceDto;
import com.kaandev.salonexplorer.mapper.ServiceMapper;
import com.kaandev.salonexplorer.repository.SalonServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceCatalogService {

    private final SalonServiceRepository repository;
    private final ServiceMapper mapper;

    @Cacheable("services")
    @Transactional(readOnly = true)
    public List<ServiceDto> findAll() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }
}
