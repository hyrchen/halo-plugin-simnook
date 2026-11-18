package run.halo.simnook;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.*;
import java.util.*;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.Role;
import run.halo.app.core.extension.User;
import run.halo.app.core.extension.notification.Subscription;
import run.halo.app.core.user.service.RoleService;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.infra.ExternalLinkProcessor;
import run.halo.app.notification.NotificationCenter;
import run.halo.app.notification.NotificationReasonEmitter;

class ReminderServiceTest {
    @Test
    void matchesOnlySelectedDaysEventsAndCards() {
        var f = new Fixture();
        assertThat(RenewalSettings.today(Clock.fixed(Instant.parse("2026-11-07T16:00:00Z"), ZoneOffset.UTC)))
                .isEqualTo(LocalDate.of(2026, 11, 8));
        var today = LocalDate.of(2026, 11, 8);
        var spec = f.channel.getSpec();
        spec.setEvents(List.of("upcoming"));
        spec.setDaysAhead(List.of(30, 7, 1));
        Map.of("2026-12-08", true, "2026-11-15", true, "2026-11-14", false, "2026-11-09", true, "2026-11-08", false)
                .forEach((date, expected) -> {
                    f.card.getSpec().setNextRenewalAt(date);
                    assertThat(ReminderService.matches(f.channel, f.card, today))
                            .as(date)
                            .isEqualTo(expected);
                });

        spec.setEvents(List.of("window"));
        f.card.getSpec().setNextRenewalAt("2026-11-07");
        f.card.getSpec().setNextRenewalEnd("2026-11-09");
        assertThat(ReminderService.matches(f.channel, f.card, today)).isTrue();
        f.card.getSpec().setNextRenewalEnd("2026-11-07");
        assertThat(ReminderService.matches(f.channel, f.card, today)).isFalse();
        spec.setEvents(List.of("overdue"));
        assertThat(ReminderService.matches(f.channel, f.card, today)).isTrue();
        spec.setCardScope("selected");
        spec.setCards(List.of("other-card"));
        assertThat(ReminderService.matches(f.channel, f.card, today)).isFalse();
        spec.setCards(List.of("a"));
        assertThat(ReminderService.matches(f.channel, f.card, today)).isTrue();
        f.card.getSpec().setCancelledAt("2026-11-01");
        assertThat(ReminderService.matches(f.channel, f.card, today)).isFalse();
    }

    @Test
    void sendsOncePerSendTimeSlot() {
        var f = new Fixture();
        // 固定时钟为上海 09:00：只有 10:00 的渠道本轮不发送，下次是今天 10:00。
        f.channel.getSpec().setSendTimes(List.of("10:00"));
        f.service.check().block();
        verifyNoInteractions(f.emitter);
        assertThat(ReminderService.viewOf(f.channel, ZonedDateTime.of(2026, 11, 8, 9, 0, 0, 0, RenewalSettings.ZONE))
                        .nextRunAt())
                .isEqualTo("2026-11-08T02:00:00Z");

        // 08:00 和 09:00 都已过：只按最晚的 09:00 时段发一次，同一时段不重复。
        f.channel.getSpec().setSendTimes(List.of("08:00", "09:00", "21:00"));
        f.service.check().block();
        f.service.check().block();
        verify(f.emitter, times(2)).emit(anyString(), any());
        assertThat(f.channel.getSpec().getSubmittedDates()).containsEntry("admin", "2026-11-08@09:00");

        var evening = ZonedDateTime.of(2026, 11, 8, 21, 30, 0, 0, RenewalSettings.ZONE);
        assertThat(ReminderService.currentSlot(f.channel.getSpec().getSendTimes(), evening))
                .isEqualTo("2026-11-08@21:00");
        assertThat(ReminderService.nextRun(f.channel.getSpec().getSendTimes(), evening))
                .isEqualTo("2026-11-09T00:00:00Z");
    }

    @Test
    void retriesOnlyFailedRecipientAndKeepsNumbersMasked() {
        var f = new Fixture();
        when(f.emitter.emit(anyString(), any()))
                .thenReturn(Mono.empty(), Mono.error(new IllegalStateException("down")), Mono.empty());
        f.service.check().block();
        assertThat(f.channel.getSpec().getSubmittedDates()).containsOnlyKeys("admin");
        assertThat(f.channel.getSpec().getLastError()).contains("other");
        f.service.check().block();
        assertThat(f.channel.getSpec().getSubmittedDates()).containsKeys("admin", "other");
        assertThat(f.channel.getSpec().getLastError()).isNull();
        f.service.check().block();
        verify(f.emitter, times(3)).emit(anyString(), any());
        assertThat(ReminderService.summary(List.of(f.card), LocalDate.of(2026, 11, 8), Locale.ENGLISH))
                .doesNotContain("13800123456");
    }

    @Test
    void disabledEmptyAndUnsubscribedChannelsDoNotSend() {
        var f = new Fixture();
        f.channel.getSpec().setEnabled(false);
        f.service.check().block();
        f.channel.getSpec().setEnabled(true);
        f.channel.getSpec().setEvents(List.of("overdue"));
        f.service.check().block();
        f.channel.getSpec().setEvents(List.of("upcoming"));
        when(f.client.list(eq(Subscription.class), any(), isNull())).thenReturn(Flux.empty());
        f.service.check().block();
        assertThat(f.channel.getSpec().getLastError()).contains("admin");
        verifyNoInteractions(f.emitter);
    }

    @Test
    void testDoesNotConsumeDailyScheduleAndReportsFailure() {
        var f = new Fixture();
        when(f.emitter.emit(anyString(), any())).thenReturn(Mono.error(new IllegalStateException("down")));
        var view = f.service.test("channel-a").block();
        assertThat(view.lastTestError()).contains("admin", "other");
        assertThat(view.lastTestAt()).isNotNull();
        assertThat(f.channel.getSpec().getSubmittedDates()).isEmpty();
        assertThat(view.lastSubmittedAt()).isNull();
    }

    private static ReminderService.ChannelInput input(
            String name,
            List<String> events,
            List<String> times,
            String scope,
            List<String> cards,
            List<Integer> days) {
        return new ReminderService.ChannelInput(
                name, "测试", true, List.of("admin"), events, times, scope, cards, days, name == null ? null : 9L);
    }

    @Test
    void rejectsInvalidInputStaleVersionsAndUnauthorizedRecipients() {
        var f = new Fixture();
        var upcoming = List.of("upcoming");
        var nine = List.of("09:00");
        assertThatThrownBy(() -> ReminderService.validate(new ReminderService.ChannelInput(
                        null, "", true, List.of(), List.of(), nine, "all", List.of(), List.of(7), null)))
                .hasMessageContaining("渠道名称");
        assertThatThrownBy(() ->
                        ReminderService.validate(input(null, upcoming, List.of("9点"), "all", List.of(), List.of(7))))
                .hasMessageContaining("HH:mm");
        assertThatThrownBy(() -> ReminderService.validate(
                        input(null, upcoming, List.of("09:00", "09:00"), "all", List.of(), List.of(7))))
                .hasMessageContaining("不重复");
        assertThatThrownBy(
                        () -> ReminderService.validate(input(null, upcoming, nine, "selected", List.of(), List.of(7))))
                .hasMessageContaining("号卡");
        assertThatThrownBy(() -> ReminderService.validate(input(null, upcoming, nine, "all", List.of(), List.of(400))))
                .hasMessageContaining("天数");
        // 只提醒逾期时不需要天数；自定义天数在 0–365 内均可。
        ReminderService.validate(input(null, List.of("overdue"), nine, "all", List.of(), List.of()));
        ReminderService.validate(input(null, upcoming, nine, "all", List.of(), List.of(45, 10, 0)));

        var stale = input("channel-a", upcoming, nine, "all", List.of(), List.of(7));
        assertThatThrownBy(() -> f.service.save(stale).block()).hasMessageContaining("409");
        when(f.roles.listPermissions(anySet())).thenReturn(Flux.empty());
        assertThatThrownBy(() -> f.service.save(stale).block()).hasMessageContaining("无号卡管理权限");
        verifyNoInteractions(f.notifications);
    }

    @Test
    void editingDoesNotResubscribeAndDeletionRemovesSubscriptions() {
        var f = new Fixture();
        var input = new ReminderService.ChannelInput(
                "channel-a",
                "编辑",
                false,
                List.of("admin", "other"),
                List.of("overdue"),
                List.of("20:00", "08:30"),
                "selected",
                List.of("a"),
                List.of(1, 30, 7),
                1L);
        var saved = f.service.save(input).block();
        assertThat(saved.enabled()).isFalse();
        assertThat(saved.sendTimes()).containsExactly("08:30", "20:00");
        assertThat(saved.cards()).containsExactly("a");
        // 未勾选「即将到期」时不保留天数。
        assertThat(saved.daysAhead()).isEmpty();
        verifyNoInteractions(f.notifications);
        f.service.delete("channel-a", 1L).block();
        verify(f.notifications, times(2)).unsubscribe(any(), any());
        verify(f.client).delete(f.channel);
    }

    static class Fixture {
        final ReactiveExtensionClient client = mock(ReactiveExtensionClient.class);
        final NotificationCenter notifications = mock(NotificationCenter.class);
        final NotificationReasonEmitter emitter = mock(NotificationReasonEmitter.class);
        final RoleService roles = mock(RoleService.class);
        final ExternalLinkProcessor links = mock(ExternalLinkProcessor.class);
        final ReminderChannel channel = new ReminderChannel();
        final SimCard card = Fixtures.card("a");
        final ReminderService service;

        Fixture() {
            var metadata = new Metadata();
            metadata.setName("channel-a");
            metadata.setVersion(1L);
            channel.setMetadata(metadata);
            var spec = new ReminderChannel.Spec();
            spec.setDisplayName("提醒");
            spec.setRecipients(List.of("admin", "other"));
            spec.setEvents(List.of("upcoming", "window", "overdue"));
            spec.setDaysAhead(List.of(7, 3, 1));
            spec.setEnabled(true);
            channel.setSpec(spec);
            card.getSpec().setNextRenewalAt("2026-11-09");
            when(client.list(eq(SimCard.class), any(), any())).thenReturn(Flux.just(card));
            when(client.list(eq(ReminderChannel.class), any(), any()))
                    .thenAnswer(call -> Flux.defer(() -> {
                        Predicate<ReminderChannel> filter = call.getArgument(1);
                        return Flux.just(channel).filter(filter);
                    }));
            when(client.fetch(eq(ReminderChannel.class), anyString())).thenReturn(Mono.just(channel));
            when(client.update(any(ReminderChannel.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));
            when(client.delete(any(ReminderChannel.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));
            when(notifications.unsubscribe(any(), any())).thenReturn(Mono.empty());
            var user = new User();
            user.setMetadata(metadata);
            user.setSpec(new User.UserSpec());
            when(client.fetch(eq(User.class), anyString())).thenReturn(Mono.just(user));
            when(roles.getRolesByUsername(anyString())).thenReturn(Flux.just("manager"));
            var role = new Role();
            role.setRules(List.of(new Role.PolicyRule.Builder()
                    .apiGroups("*")
                    .resources("*")
                    .verbs("*")
                    .build()));
            when(roles.listPermissions(anySet())).thenReturn(Flux.just(role));
            var subscriptions = new ArrayList<Subscription>();
            for (var recipient : spec.getRecipients()) {
                var sub = new Subscription();
                sub.setMetadata(metadata);
                var nativeSpec = new Subscription.Spec();
                var subscriber = new Subscription.Subscriber();
                subscriber.setName(recipient);
                nativeSpec.setSubscriber(subscriber);
                nativeSpec.setReason(ReminderService.interest("channel-a", recipient));
                sub.setSpec(nativeSpec);
                subscriptions.add(sub);
            }
            when(client.list(eq(Subscription.class), any(), isNull())).thenAnswer(call -> {
                Predicate<Subscription> filter = call.getArgument(1);
                return Flux.fromIterable(subscriptions).filter(filter);
            });
            when(emitter.emit(anyString(), any())).thenReturn(Mono.empty());
            when(links.processLink(anyString())).thenAnswer(call -> call.getArgument(0));
            service = new ReminderService(
                    client,
                    notifications,
                    emitter,
                    roles,
                    links,
                    Clock.fixed(Instant.parse("2026-11-08T01:00:00Z"), ZoneOffset.UTC));
        }
    }
}
