package com.farewatch.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("EmailAddress VO")
class EmailAddressTest {

    @Test
    @DisplayName("일반 이메일 형식 수락")
    void acceptsStandardEmail() {
        assertThat(new EmailAddress("user@example.com").value()).isEqualTo("user@example.com");
        assertThat(new EmailAddress("hanhyur+alert@farewatch.app").value())
                .isEqualTo("hanhyur+alert@farewatch.app");
    }

    @ParameterizedTest(name = "잘못된 이메일 거부: \"{0}\"")
    @ValueSource(strings = {
            "no-at-sign",
            "@no-local.com",
            "no-domain@",
            "spaces in@email.com",
            "double@@at.com",
            "no-tld@domain"
    })
    void rejectsInvalid(String invalid) {
        assertThatThrownBy(() -> new EmailAddress(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("null 거부")
    void rejectsNull(String nullValue) {
        assertThatThrownBy(() -> new EmailAddress(nullValue))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("같은 값이면 equals")
    void equalsByValue() {
        assertThat(new EmailAddress("a@b.com")).isEqualTo(new EmailAddress("a@b.com"));
        assertThat(new EmailAddress("a@b.com")).isNotEqualTo(new EmailAddress("c@d.com"));
    }
}
