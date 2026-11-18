package run.halo.simnook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static run.halo.simnook.BackupModels.*;
import static run.halo.simnook.Fixtures.card;
import static run.halo.simnook.Fixtures.record;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.plugin.PluginContext;

/** 会写入号卡的操作：备份恢复与快捷续费。 */
class CardWriteTest {
    private static final String HUGE = "12345678901234567890.123456789";

    @Test
    void backupRoundTripKeepsDecimalsAndRejectsUnsafeFiles() {
        var c = card("card-a");
        c.getSpec().setRecords(List.of(record("r1", HUGE)));
        var text = BackupService.JSON.writeValueAsString(
                new BackupDocument("simnook-backup", 1, "", "", List.of(new CardEntry("card-a", c.getSpec()))));
        assertThat(BackupService.parse(text)
                        .cards()
                        .getFirst()
                        .spec()
                        .getRecords()
                        .getFirst()
                        .getAmount())
                .isEqualByComparingTo(HUGE);
        assertThatThrownBy(() -> BackupService.parse(text.replace("\"version\":1", "\"version\":2")))
                .hasMessageContaining("版本");
        assertThatThrownBy(
                        () -> BackupService.parse(text.replace("\"name\":\"测试卡\"", "\"unknown\":1,\"name\":\"测试卡\"")))
                .hasMessageContaining("格式");
        var entry = new CardEntry("card-a", c.getSpec());
        assertThatThrownBy(() -> BackupService.parse(BackupService.JSON.writeValueAsString(
                        new BackupDocument("simnook-backup", 1, "", "", List.of(entry, entry)))))
                .hasMessageContaining("重复资源");
    }

    @Test
    void appendMergesRecordsWithoutDuplicatesAndChecksVersions() {
        var existing = List.of(record("same-id", "10"), record(null, "20"));
        var incoming =
                List.of(record("same-id", "999"), record("new-id", "30"), record(null, "20.00"), record(null, "20"));
        var merged = BackupService.appendRecords(existing, incoming);
        // 同 ID 保留现有记录；无 ID 的记录按内容计数，重复导入不会再追加。
        assertThat(merged).hasSize(4);
        assertThat(merged.getFirst().getAmount()).isEqualByComparingTo("10");
        assertThat(BackupService.appendRecords(merged, incoming)).hasSize(4);

        var current = card("card-a");
        current.getSpec().setRecords(List.of(record("old", "1")));
        var imported = card("card-a");
        imported.getSpec().setName("备份名称");
        imported.getSpec().setRecords(List.of(record("new", "2")));
        var document =
                new BackupDocument("simnook-backup", 1, "", "", List.of(new CardEntry("card-a", imported.getSpec())));
        var snapshot = new BackupService.Snapshot(Map.of("card-a", current));
        var plan = BackupService.prepare(
                document, List.of(new RestoreDecision("SimCard", "card-a", "append", "3")), snapshot);
        var updated = (SimCard) plan.getFirst().resource();
        assertThat(updated.getSpec().getName()).isEqualTo("测试卡");
        assertThat(updated.getSpec().getRecords()).hasSize(2);
        assertThat(current.getSpec().getRecords()).hasSize(1);
        assertThatThrownBy(() -> BackupService.prepare(
                        document, List.of(new RestoreDecision("SimCard", "card-a", "overwrite", "2")), snapshot))
                .hasMessageContaining("重新预览");
    }

    @Test
    void restoreStopsAfterFirstFailedWrite() {
        var client = mock(ReactiveExtensionClient.class);
        when(client.list(eq(SimCard.class), isNull(), any())).thenReturn(Flux.empty());
        when(client.create(any(SimCard.class))).thenAnswer(call -> {
            SimCard created = call.getArgument(0);
            return "b".equals(created.getMetadata().getName())
                    ? Mono.error(new IllegalStateException("write failed"))
                    : Mono.just(created);
        });
        var entries = List.of("a", "b", "c").stream()
                .map(id -> new CardEntry(id, card(id).getSpec()))
                .toList();
        var text = BackupService.JSON.writeValueAsString(new BackupDocument("simnook-backup", 1, "", "", entries));
        var decisions = List.of("a", "b", "c").stream()
                .map(id -> new RestoreDecision("SimCard", id, "create", null))
                .toList();
        var result = new BackupService(client, mock(PluginContext.class))
                .restore(new RestoreRequest(text, BackupService.hash(text), decisions))
                .block(Duration.ofSeconds(3));
        assertThat(result.items()).extracting(RestoreItem::status).containsExactly("success", "failed", "pending");
        verify(client, times(2)).create(any(SimCard.class));
    }

    @Test
    void csvNeutralizesSpreadsheetFormulas() {
        var out = new StringBuilder();
        BackupService.csvRow(out, "+8613800123456", "=1+1", "a,\"b", new BigDecimal(HUGE));
        assertThat(out).hasToString("\"'+8613800123456\",\"'=1+1\",\"a,\"\"b\",\"" + HUGE + "\"\n");
    }

    @Test
    void quickRenewalAppendsOnceAndKeepsServerDecimals() {
        var c = card("a");
        var old = record("old", HUGE);
        c.getSpec().setRecords(List.of(old));
        c.getSpec().setNextRenewalAt("2026-10-30");
        var row = record("new", "0");
        row.setType("续费/保号");
        row.setDate("2026-11-08");
        row.setExtendsTo("2026-12-08");
        var request = new ManagementModels.QuickRenewalRequest("3", row, null, null);

        var renewed = ManagementEndpoint.renew(c, request);
        assertThat(renewed.getSpec().getRecords()).hasSize(2);
        assertThat(renewed.getSpec().getRecords().getFirst().getAmount()).isEqualByComparingTo(HUGE);
        assertThat(renewed.getSpec().getNextRenewalAt()).isNull();
        assertThat(c.getSpec().getRecords()).hasSize(1);
        // 同一请求重放时直接返回，不会重复追加。
        assertThat(ManagementEndpoint.renew(renewed, request)).isSameAs(renewed);
        assertThatThrownBy(() ->
                        ManagementEndpoint.renew(c, new ManagementModels.QuickRenewalRequest("2", row, null, null)))
                .hasMessageContaining("409");
        assertThatThrownBy(() -> ManagementEndpoint.renew(
                        c, new ManagementModels.QuickRenewalRequest("3", row, "2026-12-09", "2026-12-08")))
                .hasMessageContaining("截止日");
        row.setAmount(BigDecimal.TEN);
        row.setCurrency("USD");
        assertThatThrownBy(() -> ManagementEndpoint.renew(c, request)).hasMessageContaining("汇率");
    }
}
