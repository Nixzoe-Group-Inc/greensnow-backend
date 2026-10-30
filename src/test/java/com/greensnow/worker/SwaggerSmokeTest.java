package com.greensnow.worker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

// springdoc 2.5.0 targets Spring Boot 3 and this project is on 4.1.2, so compiling against it
// proves nothing - this goes over real http to prove the document and the ui actually serve.
// Boot 4 dropped TestRestTemplate, hence the plain RestTemplate against @LocalServerPort
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SwaggerSmokeTest {

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    SwaggerSmokeTest() {
        // a 401 is an answer this test wants to read, not an exception to be thrown
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void theApiDocumentIsServedAndNamesTheAuthRoutes() {
        ResponseEntity<String> document = restTemplate.getForEntity(url("/v3/api-docs"), String.class);

        assertThat(document.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(document.getBody())
                .contains("/auth/register")
                .contains("/auth/login")
                .contains("/auth/logout")
                .contains("/auth/change-password")
                .contains("/auth/forgot-password")
                .contains("/auth/reset-password")
                // the bearer scheme, without which the try it out button cannot authenticate
                .contains("bearerAuth")
                .contains("Worker Platform");
    }

    @Test
    void theSwaggerUiIsReachableWithoutATokenAndTheRestOfTheApiIsNot() {
        assertThat(restTemplate.getForEntity(url("/swagger-ui/index.html"), String.class).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        // the documentation being public must not have opened anything else
        assertThat(restTemplate.postForEntity(url("/auth/logout"), null, String.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
