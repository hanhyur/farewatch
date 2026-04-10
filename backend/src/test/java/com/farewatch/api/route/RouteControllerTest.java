package com.farewatch.api.route;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.farewatch.api.common.GlobalExceptionHandler;
import com.farewatch.domain.route.Route;
import com.farewatch.domain.route.RouteRepository;
import com.farewatch.domain.shared.AirportCode;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = RouteController.class)
@Import(GlobalExceptionHandler.class)
class RouteControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private RouteRepository routeRepository;

    @Test
    void list_returnsRoutesSortedById() throws Exception {
        Route a = persistedRoute(2L, "PUS", "HND");
        Route b = persistedRoute(1L, "PUS", "NRT");
        when(routeRepository.findAll()).thenReturn(List.of(a, b));

        mockMvc.perform(get("/api/v1/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].origin").value("PUS"))
                .andExpect(jsonPath("$.data[0].destination").value("NRT"))
                .andExpect(jsonPath("$.data[1].id").value(2));
    }

    @Test
    void get_existingRoute_returnsBody() throws Exception {
        when(routeRepository.findById(1L)).thenReturn(Optional.of(persistedRoute(1L, "PUS", "NRT")));

        mockMvc.perform(get("/api/v1/routes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.origin").value("PUS"));
    }

    @Test
    void get_missingRoute_returns404Envelope() throws Exception {
        when(routeRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/routes/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("route not found: 99"));
    }

    private static Route persistedRoute(long id, String origin, String destination) {
        Route r = Route.create(new AirportCode(origin), new AirportCode(destination), "KE");
        try {
            Field f = Route.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(r, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return r;
    }
}
