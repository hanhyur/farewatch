package com.farewatch.domain.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 이메일 주소. 알림 규칙의 사용자 식별자로 사용.
 *
 * <p>v0에서는 단순 정규식 검증만 수행한다. RFC 5322 전체 준수는 과도하므로, 실전 사용
 * 케이스에 필요한 수준의 "@ 기호 + local + domain + TLD" 형태만 확인한다.
 *
 * <p>기본적으로 non-null 컬럼으로 매핑된다. 사용처에서 {@code @AttributeOverride} 로
 * 컬럼명은 바꿀 수 있지만, null 허용 여부를 override 하지 않는 한 안전한 기본값이 적용.
 */
@Embeddable
public record EmailAddress(@Column(nullable = false) String value) {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public EmailAddress {
        Objects.requireNonNull(value, "email must not be null");
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email address: '" + value + "'");
        }
    }
}
