package com.gertidokaj.geocento.java_full_stack_assessment.model;

import java.util.List;

public record GeoJsonPolygon(
        String type,
        List<List<List<Double>>> coordinates
) {
    public boolean isPolygon() {
        return type != null && "Polygon".equalsIgnoreCase(type);
    }

    public static GeoJsonPolygon fromRing(List<List<Double>> ring) {
        return new GeoJsonPolygon("Polygon", List.of(ring));
    }
}
