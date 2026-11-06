package com.gertidokaj.geocento.java_full_stack_assessment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gertidokaj.geocento.java_full_stack_assessment.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void searchByAoi_unauthorized_returns401() throws Exception {
        Map<String, Object> aoi = Map.of(
                "type", "Polygon",
                "coordinates", List.of(List.of(
                        List.of(-0.5, 51.0),
                        List.of(0.5, 51.0),
                        List.of(0.5, 52.0),
                        List.of(-0.5, 52.0),
                        List.of(-0.5, 51.0)
                ))
        );
        mockMvc.perform(post("/api/products/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aoi)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void searchByAoi_withValidJwt_returnsFeatureCollection() throws Exception {
        Map<String, Object> aoi = Map.of(
                "type", "Polygon",
                "coordinates", List.of(List.of(
                        List.of(-0.5, 51.0),
                        List.of(0.5, 51.0),
                        List.of(0.5, 52.0),
                        List.of(-0.5, 52.0),
                        List.of(-0.5, 51.0)
                ))
        );
        mockMvc.perform(post("/api/products/search")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_openid")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aoi)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.type").value("FeatureCollection"))
                .andExpect(jsonPath("$.features").isArray())
                .andExpect(jsonPath("$.features.length()").value(1))
                .andExpect(jsonPath("$.features[0].properties.name").value("Sentinel-2A"));
    }

    @Test
    void searchByAoi_invalidPolygon_returns400() throws Exception {
        Map<String, Object> aoi = Map.of(
                "type", "Point",
                "coordinates", List.of(List.of(
                        List.of(0.0, 51.0),
                        List.of(0.5, 51.0),
                        List.of(0.5, 52.0),
                        List.of(0.0, 52.0),
                        List.of(0.0, 51.0)
                ))
        );
        mockMvc.perform(post("/api/products/search")
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_openid")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aoi)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
