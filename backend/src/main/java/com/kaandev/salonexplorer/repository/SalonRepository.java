package com.kaandev.salonexplorer.repository;

import com.kaandev.salonexplorer.domain.entity.Salon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalonRepository extends JpaRepository<Salon, Long> {

    Optional<Salon> findByGooglePlaceId(String googlePlaceId);

    boolean existsByGooglePlaceId(String googlePlaceId);

    long countByIsActiveTrue();
}
