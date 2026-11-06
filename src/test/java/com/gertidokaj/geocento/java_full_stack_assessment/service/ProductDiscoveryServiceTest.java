package com.gertidokaj.geocento.java_full_stack_assessment.service;

import com.gertidokaj.geocento.java_full_stack_assessment.model.GeoJsonPolygon;
import com.gertidokaj.geocento.java_full_stack_assessment.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductDiscoveryServiceTest {

    private ProductDiscoveryService service;

    @BeforeEach
    void setUp() {
        service = new ProductDiscoveryService();
        service.initMockData();
    }

    @Test
    void searchByAoi_intersectingSentinel2_returnsProduct() {
        GeoJsonPolygon aoi = GeoJsonPolygon.fromRing(List.of(
                List.of(-0.5, 51.2),
                List.of(0.2, 51.2),
                List.of(0.2, 51.8),
                List.of(-0.5, 51.8),
                List.of(-0.5, 51.2)
        ));
        List<Product> result = service.searchByAoi(aoi);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Sentinel-2A");
    }

    @Test
    void searchByAoi_nonIntersecting_returnsEmpty() {
        GeoJsonPolygon aoi = GeoJsonPolygon.fromRing(List.of(
                List.of(-10.0, 40.0),
                List.of(-9.0, 40.0),
                List.of(-9.0, 41.0),
                List.of(-10.0, 41.0),
                List.of(-10.0, 40.0)
        ));
        List<Product> result = service.searchByAoi(aoi);
        assertThat(result).isEmpty();
    }

    @Test
    void searchByAoi_null_returnsEmpty() {
        assertThat(service.searchByAoi(null)).isEmpty();
    }

    @Test
    void searchByAoi_largeAoi_returnsMultipleProducts() {
        GeoJsonPolygon aoi = GeoJsonPolygon.fromRing(List.of(
                List.of(-2.0, 47.0),
                List.of(5.0, 47.0),
                List.of(5.0, 53.0),
                List.of(-2.0, 53.0),
                List.of(-2.0, 47.0)
        ));
        List<Product> result = service.searchByAoi(aoi);
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Product::getName).containsExactlyInAnyOrder("Sentinel-2A", "SPOT-6");
    }
}
