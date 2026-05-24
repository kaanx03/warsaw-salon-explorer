package com.kaandev.salonexplorer.repository;

import com.kaandev.salonexplorer.domain.entity.SalonService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalonServiceRepository extends JpaRepository<SalonService, Long> {
    Optional<SalonService> findByName(String name);
}
