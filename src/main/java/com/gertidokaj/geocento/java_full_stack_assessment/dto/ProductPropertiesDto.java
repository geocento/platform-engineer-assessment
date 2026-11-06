package com.gertidokaj.geocento.java_full_stack_assessment.dto;

import com.gertidokaj.geocento.java_full_stack_assessment.model.Product;
import com.gertidokaj.geocento.java_full_stack_assessment.model.ProductMetadata;

import java.time.OffsetDateTime;

public record ProductPropertiesDto(
        String id,
        String name,
        OffsetDateTime acquisitionDate,
        String provider,
        String resolution,
        String thumbnailUrl,
        ProductMetadata metadata
) {
    public static ProductPropertiesDto from(Product p) {
        return new ProductPropertiesDto(
                p.getId(),
                p.getName(),
                p.getAcquisitionDate(),
                p.getProvider(),
                p.getResolution(),
                p.getThumbnailUrl(),
                p.getMetadata()
        );
    }
}
