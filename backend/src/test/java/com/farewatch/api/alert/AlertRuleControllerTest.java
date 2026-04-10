package com.farewatch.api.alert;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.farewatch.api.common.GlobalExceptionHandler;
import com.farewatch.domain.alert.AlertRule;
import com.farewatch.domain.alert.AlertRuleRepository;
import com.farewatch.domain.route.RouteRepository;
import com.farewatch.domain.shared.DateRange;
import com.farewatch.domain.shared.EmailAddress;
import com.farewatch.domain.shared.VerdictTrigger;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AlertRuleController.class)
@Import(GlobalExceptionHandler.class)
class AlertRuleControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AlertRuleRepository alertRuleRepository;
    @MockBean private RouteRepository routeRepository;

    @Test
    void create_returns201WithBody() throws Exception {
        when(routeRepository.existsById(1L)).thenReturn(true);
        when(alertRuleRepository.save(any(AlertRule.class)))
                .thenAnswer(
                        inv -> {
                            AlertRule arg = inv.getArgument(0);
                            setId(AlertRule.class, arg, 100L);
                            return arg;
                        });

        String body =
                """
                {
                  "routeId": 1,
                  "userIdentifier": "user@example.com",
                  "departureDateFrom": "2026-05-01",
                  "departureDateTo": "2026-05-15",
                  "targetPrice": 180000,
                  "verdictTrigger": "CHEAP"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/alert-rules")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.userIdentifier").value("user@example.com"))
                .andExpect(jsonPath("$.data.targetPrice").value(180000));
    }

    @Test
    void create_unknownRoute_returns404() throws Exception {
        when(routeRepository.existsById(99L)).thenReturn(false);
        String body =
                """
                {
                  "routeId": 99,
                  "userIdentifier": "user@example.com",
                  "departureDateFrom": "2026-05-01",
                  "departureDateTo": "2026-05-15",
                  "verdictTrigger": "CHEAP"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/alert-rules")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_invalidEmail_returns400() throws Exception {
        String body =
                """
                {
                  "routeId": 1,
                  "userIdentifier": "not-an-email",
                  "departureDateFrom": "2026-05-01",
                  "departureDateTo": "2026-05-15",
                  "verdictTrigger": "CHEAP"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/alert-rules")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_filtersByRouteId() throws Exception {
        when(alertRuleRepository.findByRouteIdAndActiveTrue(1L))
                .thenReturn(List.of(persistedRule(10L, 1L)));

        mockMvc.perform(get("/api/v1/alert-rules").param("routeId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(10));
    }

    @Test
    void list_noFilter_returnsAllActive() throws Exception {
        when(alertRuleRepository.findByActiveTrue())
                .thenReturn(List.of(persistedRule(10L, 1L), persistedRule(11L, 2L)));

        mockMvc.perform(get("/api/v1/alert-rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void deactivate_returns204AndSavesDeactivatedRule() throws Exception {
        AlertRule rule = persistedRule(10L, 1L);
        when(alertRuleRepository.findById(10L)).thenReturn(Optional.of(rule));

        mockMvc.perform(delete("/api/v1/alert-rules/10")).andExpect(status().isNoContent());

        verify(alertRuleRepository).save(rule);
        org.assertj.core.api.Assertions.assertThat(rule.isActive()).isFalse();
    }

    @Test
    void deactivate_unknown_returns404() throws Exception {
        when(alertRuleRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/v1/alert-rules/99")).andExpect(status().isNotFound());
    }

    private static AlertRule persistedRule(long id, long routeId) {
        AlertRule r =
                AlertRule.create(
                        routeId,
                        new EmailAddress("user@example.com"),
                        new DateRange(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 15)),
                        null,
                        VerdictTrigger.CHEAP);
        setId(AlertRule.class, r, id);
        return r;
    }

    private static <T> void setId(Class<T> clazz, T instance, Long id) {
        try {
            Field f = clazz.getDeclaredField("id");
            f.setAccessible(true);
            f.set(instance, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
