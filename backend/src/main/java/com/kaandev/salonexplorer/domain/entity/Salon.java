package com.kaandev.salonexplorer.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "salons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Salon extends BaseEntity {

    @Column(name = "google_place_id", nullable = false, unique = true, length = 255)
    private String googlePlaceId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id")
    private District district;

    @Column(length = 50)
    private String phone;

    @Column(length = 500)
    private String website;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(name = "price_level")
    private Short priceLevel;

    @Column(name = "photo_url", columnDefinition = "TEXT")
    private String photoUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "opening_hours", columnDefinition = "TEXT")
    private String openingHours;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "salon_services",
        joinColumns = @JoinColumn(name = "salon_id"),
        inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    @Builder.Default
    private Set<SalonService> services = new HashSet<>();

    @OneToMany(mappedBy = "salon", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ServiceOffering> serviceOfferings = new ArrayList<>();
}
