package run.halo.simnook;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.yaml.snakeyaml.Yaml;

/** 按 Halo DefaultNotificationTemplateRender 的引擎配置渲染通知模板，确认邮件内容与测试通知都能正常输出。 */
class NotificationTemplateTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);

    @Test
    void rendersDigestWithMaskedCardsAndStatusColors() throws Exception {
        var template = template();
        var overdue = Fixtures.card("b");
        overdue.getSpec().setNextRenewalAt("2026-11-01");
        var upcoming = Fixtures.card("a");
        upcoming.getSpec().setNextRenewalAt("2026-11-09");
        var cards = List.of(overdue, upcoming);
        var model = model(cards);

        var title = render(template.get("title"), model);
        var html = render(template.get("htmlBody"), model);
        var raw = render(template.get("rawBody"), model);

        assertThat(title).isEqualTo("SIMnook 续费提醒");
        assertThat(html)
                .contains("SIMnook 续费提醒", "共 <span", ">2</span> 张号卡需要处理", "已逾期", "未来 3 天内")
                .contains("#d03a3a", "#b86200", "href=\"/console/simnook\"")
                .doesNotContain("13800123456", "Renewal");
        assertThat(raw).contains("共 2 张号卡需要处理", "/console/simnook").doesNotContain("13800123456");
        // 写出渲染结果，便于在浏览器里检查邮件外观。
        var out = Path.of("build", "notification-preview.html");
        Files.createDirectories(out.getParent());
        Files.writeString(out, "<!doctype html><meta charset=\"utf-8\">" + html);
    }

    @Test
    void rendersTestNotificationWithoutCardList() throws Exception {
        var template = template();
        var html = render(template.get("htmlBody"), model(List.of()));
        assertThat(html).contains("这是一条测试通知").doesNotContain("张号卡需要处理", "th:text", "th:if");
        assertThat(render(template.get("rawBody"), model(List.of()))).doesNotContain("张号卡需要处理");
    }

    private static Map<String, Object> model(List<SimCard> cards) {
        var model = new HashMap<String, Object>();
        model.put("businessDate", TODAY.toString());
        model.put(
                "summary",
                cards.isEmpty()
                        ? "这是一条测试通知，渠道配置正常。"
                        : ReminderService.summary(cards, TODAY, Locale.SIMPLIFIED_CHINESE));
        model.put("items", ReminderService.items(cards, TODAY));
        model.put("count", cards.size());
        model.put("consoleUrl", "/console/simnook");
        return model;
    }

    private static String render(String template, Map<String, Object> model) {
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(new StringTemplateResolver());
        return engine.process(template, new Context(Locale.SIMPLIFIED_CHINESE, model));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> template() throws Exception {
        try (InputStream in = NotificationTemplateTest.class.getResourceAsStream("/extensions/notifications.yaml")) {
            for (var doc : new Yaml().loadAll(in)) {
                var map = (Map<String, Object>) doc;
                if ("NotificationTemplate".equals(map.get("kind"))) {
                    var spec = (Map<String, Object>) map.get("spec");
                    return (Map<String, String>) spec.get("template");
                }
            }
        }
        throw new IllegalStateException("缺少 NotificationTemplate");
    }
}
