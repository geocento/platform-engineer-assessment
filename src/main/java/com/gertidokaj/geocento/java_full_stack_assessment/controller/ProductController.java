package com.gertidokaj.geocento.java_full_stack_assessment.controller;

import com.gertidokaj.geocento.java_full_stack_assessment.dto.GeoJsonFeatureCollectionDto;
import com.gertidokaj.geocento.java_full_stack_assessment.model.GeoJsonPolygon;
import com.gertidokaj.geocento.java_full_stack_assessment.validation.ValidGeoJsonPolygon;
import com.gertidokaj.geocento.java_full_stack_assessment.model.Product;
import com.gertidokaj.geocento.java_full_stack_assessment.service.ProductDiscoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Satellite imagery product discovery by AOI")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);
    private final ProductDiscoveryService productDiscoveryService;

    public ProductController(ProductDiscoveryService productDiscoveryService) {
        this.productDiscoveryService = productDiscoveryService;
    }

    @Operation(
            summary = "Search products by Area of Interest",
            description = "Accepts a GeoJSON Polygon (AOI). Returns a GeoJSON FeatureCollection of products whose footprints intersect the AOI. Requires a valid JWT (Bearer token from Keycloak)."
    )
    @ApiResponse(responseCode = "200", description = "FeatureCollection of matching products")
    @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(value = "/search", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GeoJsonFeatureCollectionDto> searchByAoi(
            @RequestBody @Valid @ValidGeoJsonPolygon @Schema(description = "GeoJSON Polygon defining the Area of Interest") GeoJsonPolygon aoi) {
        List<Product> products = productDiscoveryService.searchByAoi(aoi);
        log.debug("Search by AOI: {} products found", products.size());
        return ResponseEntity.ok(GeoJsonFeatureCollectionDto.fromProducts(products));
    }
}
