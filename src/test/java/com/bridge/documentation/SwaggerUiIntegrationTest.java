package com.bridge.documentation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Swagger UI functionality
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
public class SwaggerUiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testSwaggerUiIndexPageIsAccessible() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    public void testSwaggerUiRedirectWorks() throws Exception {
        mockMvc.perform(get("/swagger-ui/"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    public void testSwaggerUiResourcesAreAccessible() throws Exception {
        // Test that Swagger UI CSS is accessible
        mockMvc.perform(get("/swagger-ui/swagger-ui-bundle.js"))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/swagger-ui/swagger-ui-standalone-preset.js"))
                .andExpect(status().isOk());
    }

    @Test
    public void testOpenApiJsonEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("FHIR Bridge API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"));
    }

    @Test
    public void testOpenApiYamlEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.oai.openapi"));
    }

    @Test
    public void testSwaggerConfigEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    public void testApiDocsContainAllExpectedPaths() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/transform/hl7v2-to-fhir']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/fhir/{resourceType}/{resourceId}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/fhir/{resourceType}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/consent/status/{patientId}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/oauth2/token']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/monitoring/health']").exists());
    }

    @Test
    public void testSecurityDefinitionsArePresent() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));
    }

    @Test
    public void testTagsAreProperlyDefined() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags").isArray())
                .andExpect(jsonPath("$.tags[?(@.name == 'Transformation')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'FHIR Resources')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'Consent Management')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'Authentication')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'Monitoring')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'System')]").exists());
    }

    @Test
    public void testServerDefinitionsArePresent() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers").isArray())
                .andExpect(jsonPath("$.servers[0].url").exists())
                .andExpect(jsonPath("$.servers[0].description").exists());
    }

    @Test
    public void testContactInformationIsPresent() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.contact.name").value("FHIR Bridge Support"))
                .andExpect(jsonPath("$.info.contact.email").value("support@fhirbridge.com"))
                .andExpect(jsonPath("$.info.contact.url").value("https://fhirbridge.com/support"));
    }

    @Test
    public void testLicenseInformationIsPresent() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.license.name").value("Apache 2.0"))
                .andExpect(jsonPath("$.info.license.url").value("https://www.apache.org/licenses/LICENSE-2.0"));
    }
}