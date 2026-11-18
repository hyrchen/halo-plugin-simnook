package run.halo.simnook;

import static org.assertj.core.api.Assertions.assertThat;
import static run.halo.simnook.Fixtures.card;
import static run.halo.simnook.Fixtures.renewal;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/** 号卡日期、状态推导，以及访客视图与管理视图的字段边界。 */
class CardViewTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 6);

    @Test
    void derivesExpiryRenewalWindowAndStatusFromRecords() {
        var c = card("a");
        var older = renewal("2026-10-01", "2027-01-01", "10", "USD", "7.2");
        var newer = renewal("2026-10-03", "2026-12-01", "20", "EUR", "0.9");
        var topup = renewal("2026-10-02", null, "2", "CNY", null);
        topup.setType("充值");
        c.getSpec().setRecords(List.of(older, topup, newer));

        var view = PublicCard.from(c, TODAY);
        // 有效期取交易日期最新的延长记录，而不是最晚的延长日期。
        assertThat(view.expiresAt()).isEqualTo("2026-12-01");
        assertThat(view.periodStart()).isEqualTo("2026-10-03");
        assertThat(view.firstPurchaseAt()).isEqualTo("2026-10-01");
        assertThat(view.totalSpentCNY()).isEqualByComparingTo("92");
        assertThat(view.history())
                .extracting(PublicCard.PublicRecord::kind)
                .containsExactly("renewal", "topup", "renewal");

        c.getSpec().setNextRenewalAt("2026-11-05");
        c.getSpec().setNextRenewalEnd("2026-11-08");
        view = PublicCard.from(c, TODAY);
        assertThat(view.urgency()).isEqualTo("续费窗口开放中（剩 2 天）");

        c.getSpec().setCancelledAt("2026-10-20");
        view = PublicCard.from(c, TODAY);
        assertThat(view.status()).isEqualTo("cancelled");
        assertThat(view.nextRenewalAt()).isNull();
        assertThat(view.urgency()).isEqualTo("号码已失效");
    }

    @Test
    void urgencyTiersUseInclusiveBoundaries() {
        assertThat(CardFacts.urgencyCode("active", TODAY.plusDays(3).toString(), null, TODAY))
                .isEqualTo("days3");
        assertThat(CardFacts.urgencyCode("active", TODAY.plusDays(4).toString(), null, TODAY))
                .isEqualTo("days7");
        assertThat(CardFacts.urgencyCode("active", TODAY.plusDays(60).toString(), null, TODAY))
                .isEqualTo("days60");
        assertThat(CardFacts.urgencyCode("active", TODAY.plusDays(61).toString(), null, TODAY))
                .isEqualTo("beyond");
        assertThat(CardFacts.urgencyCode("active", null, null, TODAY)).isEqualTo("noDate");
        assertThat(CardFacts.urgencyCode(
                        "expired", TODAY.toString(), TODAY.plusDays(7).toString(), TODAY))
                .isEqualTo("overdue");
        assertThat(CardFacts.urgencyCode("active", TODAY.minusDays(1).toString(), TODAY.toString(), TODAY))
                .isEqualTo("window");
    }

    @Test
    void visitorViewOnlyExposesTheCardPublicList() {
        var c = card("a");
        c.getSpec().setTypeKind("adapter");
        c.getSpec().setCountryName("中国");
        c.getSpec().setRecords(List.of(renewal("2026-10-01", "2027-01-01", "10", "USD", "7.2")));

        var defaults = PublicCard.visitorView(c, TODAY, 0, Locale.SIMPLIFIED_CHINESE);
        assertThat(defaults.get("publicFields")).isEqualTo(PublicCard.DEFAULT_PUBLIC_FIELDS);
        assertThat(defaults.get("type")).isEqualTo("eSIM · 卡托");
        assertThat(defaults).doesNotContainKeys("number", "device", "balance", "account", "services");
        assertThat(defaults.toString()).doesNotContain("private-", "13800123456");

        c.getSpec().setPublicFields(List.of("number", "device", "unknown"));
        var custom = PublicCard.visitorView(c, TODAY, 0, Locale.SIMPLIFIED_CHINESE);
        assertThat(custom.get("publicFields")).isEqualTo(List.of("number", "device"));
        assertThat(custom).containsEntry("number", "+86 13800123456").containsEntry("device", "private-device");
        assertThat(custom)
                .containsEntry("countryName", "")
                .containsEntry("history", List.of())
                .containsEntry("name", "••••");
        assertThat(custom.get("typeKind")).isNull();

        c.getSpec().setPublicFields(List.of());
        assertThat(PublicCard.visitorView(c, TODAY, 0, Locale.SIMPLIFIED_CHINESE)
                        .toString())
                .doesNotContain("测试卡", "3456", "private-");
    }

    @Test
    void managedViewAddsPrivateFieldsAndLocalizedChecks() {
        var c = card("a");
        c.getSpec().setNumber("");
        c.getSpec().setCountryName("美国");
        c.getSpec().setCancelledAt("not-a-date");
        var foreign = renewal("2026-10-01", "2026-13-01", "10", "JPY", null);
        c.getSpec().setRecords(List.of(foreign));

        var en = ManagementEndpoint.managedCard(c, TODAY, 0, Locale.ENGLISH);
        var zh = ManagementEndpoint.managedCard(c, TODAY, 0, Locale.SIMPLIFIED_CHINESE);
        assertThat(en.path("statusLabel").asString()).isEqualTo("Active");
        assertThat(en.path("type").asString()).isEqualTo("Physical SIM");
        assertThat(en.path("account").asString()).isEqualTo("private-account");
        assertThat(en.path("dataCheck").asString()).contains("Phone number is missing");
        assertThat(zh.path("dataCheck").asString()).contains("缺号码", "停用日期不是有效日期");
        assertThat(zh.path("history").get(0).path("title").asString()).isEqualTo("private-title");
        assertThat(zh.path("history").get(0).path("checks").toString()).contains("外币缺汇率", "本次有效期延至不是有效日期");
    }
}
