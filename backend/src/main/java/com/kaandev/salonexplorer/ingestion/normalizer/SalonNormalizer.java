package com.kaandev.salonexplorer.ingestion.normalizer;

import com.kaandev.salonexplorer.domain.entity.Salon;
import com.kaandev.salonexplorer.ingestion.client.dto.PlaceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SalonNormalizer {

    private final PhoneNormalizer phoneNormalizer;
    private final DistrictResolver districtResolver;

    private static final Map<String, Short> PRICE_LEVEL_MAP = Map.of(
        "PRICE_LEVEL_INEXPENSIVE",    (short) 1,
        "PRICE_LEVEL_MODERATE",       (short) 2,
        "PRICE_LEVEL_EXPENSIVE",      (short) 3,
        "PRICE_LEVEL_VERY_EXPENSIVE", (short) 4
    );

    public Salon toEntity(PlaceDto place) {
        var salon = Salon.builder()
            .googlePlaceId(place.id())
            .name(place.displayName().text())
            .address(place.formattedAddress())
            .phone(phoneNormalizer.normalize(place.internationalPhoneNumber()))
            .website(truncate(place.websiteUri(), 500))
            .latitude(toBigDecimal(place.location() != null ? place.location().latitude() : null, 7))
            .longitude(toBigDecimal(place.location() != null ? place.location().longitude() : null, 7))
            .rating(toBigDecimal(place.rating(), 1))
            .reviewCount(place.userRatingCount() != null ? place.userRatingCount() : 0)
            .priceLevel(mapPriceLevel(place.priceLevel()))
            .photoUrl(extractFirstPhotoRef(place))
            .isActive(true)
            .build();

        salon.setDistrict(districtResolver.resolve(place));
        return salon;
    }

    public void applyUpdates(Salon existing, PlaceDto place) {
        existing.setName(place.displayName().text());
        existing.setAddress(place.formattedAddress());
        existing.setPhone(phoneNormalizer.normalize(place.internationalPhoneNumber()));
        existing.setWebsite(truncate(place.websiteUri(), 500));
        existing.setRating(toBigDecimal(place.rating(), 1));
        existing.setReviewCount(place.userRatingCount() != null ? place.userRatingCount() : 0);
        existing.setPriceLevel(mapPriceLevel(place.priceLevel()));
        existing.setPhotoUrl(extractFirstPhotoRef(place));
        existing.setDistrict(districtResolver.resolve(place));
    }

    private Short mapPriceLevel(String googleLevel) {
        if (googleLevel == null) return null;
        return PRICE_LEVEL_MAP.get(googleLevel);
    }

    private BigDecimal toBigDecimal(Double value, int scale) {
        if (value == null) return null;
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private String extractFirstPhotoRef(PlaceDto place) {
        if (place.photos() == null || place.photos().isEmpty()) return null;
        return place.photos().get(0).name();
    }
}
