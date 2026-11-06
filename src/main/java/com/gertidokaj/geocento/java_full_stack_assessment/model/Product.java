package com.gertidokaj.geocento.java_full_stack_assessment.model;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

@Value
@Builder
public class Product {
    String id;
    String name;
    OffsetDateTime acquisitionDate;
    String provider;
    String resolution;
    String thumbnailUrl;
    ProductMetadata metadata;
    GeoJsonPolygon footprint;
}

