package com.farewatch.api.alert;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.farewatch.api.common.GlobalExceptionHandler;
import com.farewatch.domain.alert.Notification;
import com.farewatch.domain.alert.NotificationRepository;
import com.farewatch.domain.shared.FareVerdictKind;
import com.farewatch.domain.shared.Money;
import com.farewatch.domain.shared.NotificationChannel;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = NotificationController.class)
@Import(GlobalExceptionHandler.class)
class NotificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private NotificationRepository notificationRepository;

    @Test
    void list_returnsRecentNotifications() throws Exception {
        when(notificationRepository.findAllByOrderBySentAtDesc(any(Pageable.class)))
                .thenReturn(List.of(persistedNotification(1L), persistedNotification(2L)));

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].verdict").value("CHEAP"))
                .andExpect(jsonPath("$.data[0].channel").value("LOG"));
    }

    @Test
    void list_emptyHistory_returnsEmptyArray() throws Exception {
        when(notificationRepository.findAllByOrderBySentAtDesc(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    private static Notification persistedNotification(long id) {
        Notification n =
                Notification.send(
                        100L,
                        1L,
                        LocalDate.of(2026, 5, 10),
                        FareVerdictKind.CHEAP,
                        Money.krw(150_000L),
                        NotificationChannel.LOG,
                        "지금 사세요");
        try {
            Field f = Notification.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(n, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return n;
    }
}
