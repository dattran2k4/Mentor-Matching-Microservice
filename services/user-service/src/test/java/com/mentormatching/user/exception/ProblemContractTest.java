package com.mentormatching.user.exception;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import com.mentormatching.user.security.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {ProblemContractTest.Endpoints.class, ProblemErrorController.class})
@Import({ProblemContractTest.Endpoints.class, GlobalExceptionHandler.class, ProblemResponses.class, RequestIdFilter.class,
        SecurityConfiguration.class, ProblemAuthenticationEntryPoint.class, ProblemAccessDeniedHandler.class})
class ProblemContractTest {
    @Autowired MockMvc mvc;

    @RestController
    static class Endpoints {
        record Registration(@NotBlank @Email String email) {}
        @PostMapping("/test/validation") String validate(@Valid @RequestBody Registration body) { return "ok"; }
        @GetMapping("/test/conflict") String conflict() {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "This email is already registered.");
        }
        @GetMapping("/test/failure") String failure() { throw new IllegalStateException("secret SQL password"); }
        @GetMapping("/test/denied") String denied() { throw new AccessDeniedException("private permission details"); }
        @GetMapping("/test/number") String number(@RequestParam int count) { return "ok"; }
        @GetMapping("/test/min") String minimum(@RequestParam @Min(1) int count) { return "ok"; }
        @GetMapping("/test/invalid-return") @Min(1) int invalidReturn() { return 0; }
        @GetMapping("/test/conversion-failure") String conversionFailure() {
            throw new org.springframework.beans.ConversionNotSupportedException("private-value", Integer.class,
                    new IllegalStateException("private converter failure"));
        }
        @GetMapping("/test/unavailable") String unavailable() {
            var failure = new org.springframework.web.ErrorResponseException(HttpStatus.SERVICE_UNAVAILABLE);
            failure.getHeaders().set("Retry-After", "30");
            failure.setDetail("private upstream detail");
            throw failure;
        }
        @GetMapping("/test/ok") String ok() { return "ok"; }
    }

    @Test void validationReturnsFieldErrorsWithoutRejectedValues() throws Exception {
        mvc.perform(post("/test/validation").with(user("learner")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"private-invalid-value\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:mentor-matching:problem:validation-error"))
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.instance").value("/test/validation"))
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.errors[0].field").value("email"))
            .andExpect(jsonPath("$.errors[0].message").isString())
            .andExpect(content().string(not(containsString("private-invalid-value"))))
            .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test void conversionConfigurationFailureIs500Not400() throws Exception {
        mvc.perform(get("/test/conversion-failure").with(user("learner")))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
            .andExpect(content().string(not(containsString("private-value"))));
    }

    @Test void frameworkErrorPreservesStatusAndHeadersButSanitizesDetail() throws Exception {
        mvc.perform(get("/test/unavailable").with(user("learner")))
            .andExpect(status().isServiceUnavailable())
            .andExpect(header().string("Retry-After", "30"))
            .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
            .andExpect(content().string(not(containsString("private upstream detail"))));
    }

    @Test void malformedJsonIs400Not500() throws Exception {
        mvc.perform(post("/test/validation").with(user("learner")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{broken"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test void businessExceptionPreservesPublicCodeAndStatus() throws Exception {
        mvc.perform(get("/test/conflict").with(user("learner")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
            .andExpect(jsonPath("$.detail").value("This email is already registered."));
    }

    @Test void unexpectedFailureIsSanitizedAndCorrelated() throws Exception {
        var result = mvc.perform(get("/test/failure").with(user("learner")))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
            .andExpect(jsonPath("$.traceId").doesNotExist())
            .andExpect(content().string(not(containsString("secret SQL password"))))
            .andExpect(content().string(not(containsString("IllegalStateException")))).andReturn();
        String id = result.getResponse().getHeader("X-Request-ID");
        assertThat(id).isNotBlank();
        assertThat(result.getResponse().getContentAsString()).contains(id);
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test void unauthenticatedApiReturnsProblemAndChallengeNotRedirect() throws Exception {
        mvc.perform(get("/test/ok").accept(MediaType.TEXT_HTML))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(header().string("WWW-Authenticate", startsWith("Basic ")))
            .andExpect(header().doesNotExist("Location"))
            .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test void csrfFailureReturnsSecurityProblem() throws Exception {
        mvc.perform(post("/test/validation").with(user("learner"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"person@example.com\"}"))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test void controllerAccessDeniedIs403Not500() throws Exception {
        mvc.perform(get("/test/denied").with(user("learner")))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
            .andExpect(content().string(not(containsString("private permission details"))));
    }

    @Test void unsupportedMethodPreservesAllowHeader() throws Exception {
        mvc.perform(post("/test/ok").with(user("learner")).with(csrf()))
            .andExpect(status().isMethodNotAllowed()).andExpect(header().string("Allow", containsString("GET")))
            .andExpect(jsonPath("$.status").value(405));
    }

    @Test void unknownRouteIs404() throws Exception {
        mvc.perform(get("/missing-route").with(user("learner")))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test void wrongParameterTypeDoesNotLeakInput() throws Exception {
        mvc.perform(get("/test/number").param("count", "private-input").with(user("learner")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(content().string(not(containsString("private-input"))));
    }

    @Test void methodParameterValidationIs400() throws Exception {
        mvc.perform(get("/test/min").param("count", "0").with(user("learner")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test void returnValueValidationIs500WithoutClientFieldErrors() throws Exception {
        mvc.perform(get("/test/invalid-return").with(user("learner")))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(content().string(not(containsString("HandlerMethodValidationException"))));
    }

    @Test void missingParameterIs400() throws Exception {
        mvc.perform(get("/test/number").with(user("learner")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test void unsupportedContentTypeIs415() throws Exception {
        mvc.perform(post("/test/validation").with(user("learner")).with(csrf())
                .contentType(MediaType.TEXT_PLAIN).content("invalid"))
            .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test void servletErrorDispatchUsesOriginalPathAndSanitizedBody() throws Exception {
        mvc.perform(get("/error").with(request -> { request.setDispatcherType(DispatcherType.ERROR); return request; })
                .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 500)
                .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/original")
                .requestAttr(RequestDispatcher.ERROR_MESSAGE, "secret failure"))
            .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.instance").value("/original"))
            .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
            .andExpect(content().string(not(containsString("secret failure"))));
    }

    @Test void requestIdIsServerGeneratedAndChangesPerRequest() throws Exception {
        var first = mvc.perform(get("/test/ok").with(user("learner")).header("X-Request-ID", "untrusted"))
            .andExpect(status().isOk()).andReturn();
        var second = mvc.perform(get("/test/ok").with(user("learner"))).andReturn();
        String id = first.getResponse().getHeader("X-Request-ID");
        assertThat(id).isNotBlank().isNotEqualTo("untrusted")
            .isNotEqualTo(second.getResponse().getHeader("X-Request-ID"));
        assertThat(MDC.get("requestId")).isNull();
    }
}
