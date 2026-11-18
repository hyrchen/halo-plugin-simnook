package run.halo.simnook;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import run.halo.app.extension.AbstractExtension;
import run.halo.app.extension.GVK;

/** 渠道配置及投递进度仅由管理端点访问，不开放通用 CRUD。 */
@Data
@EqualsAndHashCode(callSuper = true)
@GVK(
        group = "simnook.halo.run",
        version = "v1alpha1",
        kind = "ReminderChannel",
        plural = "reminderchannels",
        singular = "reminderchannel")
public class ReminderChannel extends AbstractExtension {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Spec spec;

    @Data
    public static class Spec {
        private String displayName;
        private boolean enabled;
        private List<String> recipients = new ArrayList<>();
        private List<String> events = new ArrayList<>();
        /** 每日发送时间 HH:mm，按 Asia/Shanghai 计算。 */
        private String sendTime = "09:00";
        /** all：全部号卡（含以后新增）；selected：仅 {@link #cards} 中的号卡。 */
        private String cardScope = "all";

        private List<String> cards = new ArrayList<>();
        /** 只在续费起始日前这些天提醒即将到期，0 表示当天。 */
        private List<Integer> daysAhead = new ArrayList<>();

        private Map<String, String> submittedDates = new LinkedHashMap<>();
        private String lastSubmittedAt;
        private String lastError;
        private String lastTestAt;
        private String lastTestError;
    }
}
