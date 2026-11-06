package com.gertidokaj.geocento.java_full_stack_assessment.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ ElementType.PARAMETER, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = GeoJsonPolygonValidator.class)
@Documented
public @interface ValidGeoJsonPolygon {
    String message() default "Invalid GeoJSON Polygon: need type=Polygon and coordinates as single ring of [lon,lat] pairs";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
