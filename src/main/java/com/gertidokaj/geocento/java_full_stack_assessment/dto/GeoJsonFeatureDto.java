package com.gertidokaj.geocento.java_full_stack_assessment.dto;

import com.gertidokaj.geocento.java_full_stack_assessment.model.GeoJsonPolygon;
import com.gertidokaj.geocento.java_full_stack_assessment.model.Product;

public record GeoJsonFeatureDto(
        String type,
        GeoJsonPolygon geometry,
        ProductPropertiesDto properties
) {
    public static final String TYPE = "Feature";

    public static GeoJsonFeatureDto from(Product p) {
        GeoJsonPolygon geom = p.getFootprint();
        if (geom == null) return null;
        return new GeoJsonFeatureDto(TYPE, geom, ProductPropertiesDto.from(p));
    }
}
