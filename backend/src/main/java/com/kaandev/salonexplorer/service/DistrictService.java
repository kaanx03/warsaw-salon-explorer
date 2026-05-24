package com.kaandev.salonexplorer.service;

import com.kaandev.salonexplorer.domain.dto.DistrictDto;
import com.kaandev.salonexplorer.mapper.DistrictMapper;
import com.kaandev.salonexplorer.repository.DistrictRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DistrictService {

    private final DistrictRepository repository;
    private final DistrictMapper mapper;

    @Transactional(readOnly = true)
    public List<DistrictDto> findAll() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }
}
