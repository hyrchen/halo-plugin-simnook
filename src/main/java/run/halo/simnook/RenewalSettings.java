package run.halo.simnook;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/** 插件统一按 Asia/Shanghai 计算「今天」，避免容器默认 UTC 时区让日期分档错一天。 */
final class RenewalSettings {
    static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private RenewalSettings() {}

    static LocalDate today() {
        return today(Clock.systemUTC());
    }

    static LocalDate today(Clock clock) {
        return LocalDate.now(clock.withZone(ZONE));
    }
}
