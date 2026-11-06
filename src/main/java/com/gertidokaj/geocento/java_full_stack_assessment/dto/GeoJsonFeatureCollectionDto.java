package com.gertidokaj.geocento.java_full_stack_assessment.dto;

import com.gertidokaj.geocento.java_full_stack_assessment.model.Product;

import java.util.List;

public record GeoJsonFeatureCollectionDto(
        String type,
        List<GeoJsonFeatureDto> features
) {
    public static final String TYPE = "FeatureCollection";

    public static GeoJsonFeatureCollectionDto fromProducts(List<Product> products) {
        List<GeoJsonFeatureDto> features = products.stream()
                .map(GeoJsonFeatureDto::from)
                .filter(f -> f != null)
                .toList();
        return new GeoJsonFeatureCollectionDto(TYPE, features);
    }
}
