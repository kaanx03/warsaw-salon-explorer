package com.kaandev.salonexplorer.repository;

import com.kaandev.salonexplorer.domain.entity.ServiceOffering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long> {

    List<ServiceOffering> findBySalonIdOrderByCategory(Long salonId);

    @Modifying
    @Query("DELETE FROM ServiceOffering o WHERE o.salon.id = :salonId")
    void deleteBySalonId(Long salonId);
}
