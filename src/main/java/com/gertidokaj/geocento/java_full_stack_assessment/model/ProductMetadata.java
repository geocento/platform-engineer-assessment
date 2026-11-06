package com.gertidokaj.geocento.java_full_stack_assessment.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProductMetadata {
    String sensor;
    int cloudCoverage;
}

