package com.gertidokaj.geocento.java_full_stack_assessment.service;

import com.gertidokaj.geocento.java_full_stack_assessment.model.GeoJsonPolygon;
import com.gertidokaj.geocento.java_full_stack_assessment.model.Product;
import com.gertidokaj.geocento.java_full_stack_assessment.model.ProductMetadata;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalDouble;

@Service
public class ProductDiscoveryService {

    private final List<Product> products = new ArrayList<>();

    @PostConstruct
    void initMockData() {
        // mock data: UK, Alps, France
        products.add(
                Product.builder()
                        .id("S2A_20240115T103000")
                        .name("Sentinel-2A")
                        .acquisitionDate(OffsetDateTime.parse("2024-01-15T10:30:00Z"))
                        .provider("ESA")
                        .resolution("10m")
                        .thumbnailUrl("/thumbnails/sentinel2a.jpg")
                        .metadata(ProductMetadata.builder()
                                .sensor("MSI")
                                .cloudCoverage(15)
                                .build())
                        .footprint(buildBoxPolygon(-1.0, 51.0, 0.0, 52.0))
                        .build()
        );

        products.add(
                Product.builder()
                        .id("L8_20240201T090000")
                        .name("Landsat-8")
                        .acquisitionDate(OffsetDateTime.parse("2024-02-01T09:00:00Z"))
                        .provider("USGS")
                        .resolution("30m")
                        .thumbnailUrl("/thumbnails/landsat8.jpg")
                        .metadata(ProductMetadata.builder()
                                .sensor("OLI")
                                .cloudCoverage(5)
                                .build())
                        .footprint(buildBoxPolygon(10.0, 45.0, 11.0, 46.0))
                        .build()
        );

        products.add(
                Product.builder()
                        .id("SPOT6_20240310T120000")
                        .name("SPOT-6")
                        .acquisitionDate(OffsetDateTime.parse("2024-03-10T12:00:00Z"))
                        .provider("AIRBUS")
                        .resolution("1.5m")
                        .thumbnailUrl("/thumbnails/spot6.jpg")
                        .metadata(ProductMetadata.builder()
                                .sensor("HRG")
                                .cloudCoverage(25)
                                .build())
                        .footprint(buildBoxPolygon(2.0, 48.0, 3.0, 49.0))
                        .build()
        );
    }

    public List<Product> searchByAoi(GeoJsonPolygon aoi) {
        if (aoi == null || !aoi.isPolygon()) return Collections.emptyList();

        BoundingBox aoiBox = computeBoundingBox(aoi);
        if (aoiBox.isEmpty()) return Collections.emptyList();

        List<Product> result = new ArrayList<>();
        for (Product product : products) {
            if (product.getFootprint() == null) continue;
            BoundingBox productBox = computeBoundingBox(product.getFootprint());
            if (!productBox.isEmpty() && aoiBox.intersects(productBox)) {
                result.add(product);
            }
        }
        return result;
    }

    private GeoJsonPolygon buildBoxPolygon(double minLon, double minLat, double maxLon, double maxLat) {
        List<List<Double>> ring = List.of(
                List.of(minLon, minLat),
                List.of(maxLon, minLat),
                List.of(maxLon, maxLat),
                List.of(minLon, maxLat),
                List.of(minLon, minLat)
        );
        return GeoJsonPolygon.fromRing(ring);
    }

    private BoundingBox computeBoundingBox(GeoJsonPolygon polygon) {
        if (polygon == null || polygon.coordinates().isEmpty()) return BoundingBox.empty();
        List<List<Double>> firstRing = polygon.coordinates().get(0);
        if (firstRing == null || firstRing.isEmpty()) return BoundingBox.empty();

        OptionalDouble minLon = firstRing.stream()
                .filter(p -> p != null && p.size() >= 2)
                .mapToDouble(p -> p.get(0))
                .min();
        OptionalDouble maxLon = firstRing.stream()
                .filter(p -> p != null && p.size() >= 2)
                .mapToDouble(p -> p.get(0))
                .max();
        OptionalDouble minLat = firstRing.stream()
                .filter(p -> p != null && p.size() >= 2)
                .mapToDouble(p -> p.get(1))
                .min();
        OptionalDouble maxLat = firstRing.stream()
                .filter(p -> p != null && p.size() >= 2)
                .mapToDouble(p -> p.get(1))
                .max();

        if (minLon.isEmpty() || maxLon.isEmpty() || minLat.isEmpty() || maxLat.isEmpty()) {
            return BoundingBox.empty();
        }

        return new BoundingBox(
                minLon.getAsDouble(),
                minLat.getAsDouble(),
                maxLon.getAsDouble(),
                maxLat.getAsDouble()
        );
    }

    private record BoundingBox(double minLon, double minLat, double maxLon, double maxLat) {
        static BoundingBox empty() {
            return new BoundingBox(1, 1, 0, 0);
        }

        boolean isEmpty() {
            return minLon > maxLon || minLat > maxLat;
        }

        boolean intersects(BoundingBox other) {
            if (this.isEmpty() || other.isEmpty()) return false;
            return this.maxLon >= other.minLon &&
                    this.minLon <= other.maxLon &&
                    this.maxLat >= other.minLat &&
                    this.minLat <= other.maxLat;
        }
    }
}

