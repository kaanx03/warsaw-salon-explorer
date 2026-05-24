package com.kaandev.salonexplorer.repository;

import com.kaandev.salonexplorer.domain.entity.District;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DistrictRepository extends JpaRepository<District, Long> {
    Optional<District> findBySlug(String slug);
}
