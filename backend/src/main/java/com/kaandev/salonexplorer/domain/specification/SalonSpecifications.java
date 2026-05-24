package com.kaandev.salonexplorer.domain.specification;

import com.kaandev.salonexplorer.domain.entity.Salon;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class SalonSpecifications {

    private SalonSpecifications() {}

    public static Specification<Salon> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }

    public static Specification<Salon> hasDistrictSlug(String slug) {
        if (slug == null || slug.isBlank()) return null;
        return (root, query, cb) -> cb.equal(root.get("district").get("slug"), slug.toLowerCase());
    }

    public static Specification<Salon> hasService(String serviceName) {
        if (serviceName == null || serviceName.isBlank()) return null;
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Object, Object> services = root.join("services");
            return cb.equal(cb.lower(services.get("name")), serviceName.toLowerCase());
        };
    }

    public static Specification<Salon> minRating(BigDecimal min) {
        if (min == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("rating"), min);
    }

    public static Specification<Salon> maxPriceLevel(Short max) {
        if (max == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("priceLevel"), max);
    }

    public static Specification<Salon> nameContains(String search) {
        if (search == null || search.isBlank()) return null;
        return (root, query, cb) -> cb.like(
            cb.lower(root.get("name")),
            "%" + search.toLowerCase() + "%"
        );
    }
}
