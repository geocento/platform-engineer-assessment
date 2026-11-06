package com.gertidokaj.geocento.java_full_stack_assessment.validation;

import com.gertidokaj.geocento.java_full_stack_assessment.model.GeoJsonPolygon;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;

public class GeoJsonPolygonValidator implements ConstraintValidator<ValidGeoJsonPolygon, GeoJsonPolygon> {

    private static final double MIN_LON = -180;
    private static final double MAX_LON = 180;
    private static final double MIN_LAT = -90;
    private static final double MAX_LAT = 90;

    @Override
    public boolean isValid(GeoJsonPolygon value, ConstraintValidatorContext context) {
        if (value == null) return true;
        if (!value.isPolygon()) return false;
        List<List<List<Double>>> coords = value.coordinates();
        if (coords == null || coords.isEmpty()) return false;
        List<List<Double>> ring = coords.get(0);
        if (ring == null || ring.size() < 4) return false;
        if (!ring.get(0).equals(ring.get(ring.size() - 1))) return false;
        for (List<Double> point : ring) {
            if (point == null || point.size() < 2) return false;
            Double lon = point.get(0);
            Double lat = point.get(1);
            if (lon == null || lat == null) return false;
            if (lon < MIN_LON || lon > MAX_LON || lat < MIN_LAT || lat > MAX_LAT) return false;
        }
        return true;
    }
}
