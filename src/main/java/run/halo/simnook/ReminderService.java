package run.halo.simnook;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.User;
import run.halo.app.core.extension.notification.Reason;
import run.halo.app.core.extension.notification.Subscription;
import run.halo.app.core.user.service.RoleService;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.infra.ExternalLinkProcessor;
import run.halo.app.notification.NotificationCenter;
import run.halo.app.notification.NotificationReasonEmitter;
import run.halo.app.notification.UserIdentity;

@Slf4j
@Component
public class ReminderService {
    static final String REASON = "simnook-channel-reminder";
    static final Set<String> EVENTS = Set.of("upcoming", "window", "overdue");
    private final ReactiveExtensionClient client;
    private final NotificationCenter notifications;
    private final NotificationReasonEmitter emitter;
    private final RoleService roles;
    private final ExternalLinkProcessor links;
    private final Clock clock;
    private final Set<String> busy = ConcurrentHashMap.newKeySet();

    @Autowired
    public ReminderService(
            ReactiveExtensionClient client,
            NotificationCenter notifications,
            NotificationReasonEmitter emitter,
            RoleService roles,
            ExternalLinkProcessor links) {
        this(client, notifications, emitter, roles, links, Clock.systemUTC());
    }

    ReminderService(
            ReactiveExtensionClient client,
            NotificationCenter notifications,
            NotificationReasonEmitter emitter,
            RoleService roles,
            ExternalLinkProcessor links,
            Clock clock) {
        this.client = client;
        this.notifications = notifications;
        this.emitter = emitter;
        this.roles = roles;
        this.links = links;
        this.clock = clock;
    }

    @Schema(name = "ReminderChannelInput")
    public record ChannelInput(
            String name,
            String displayName,
            boolean enabled,
            List<String> recipients,
            List<String> events,
            String sendTime,
            String cardScope,
            List<String> cards,
            List<Integer> daysAhead,
            Long version) {}

    @Schema(name = "ReminderChannelView")
    public record ChannelView(
            String name,
            String displayName,
            boolean enabled,
            List<String> recipients,
            List<String> events,
            String sendTime,
            String cardScope,
            List<String> cards,
            List<Integer> daysAhead,
            Long version,
            String nextRunAt,
            String lastSubmittedAt,
            String lastError,
            String lastTestAt,
            String lastTestError) {}

    @Schema(name = "ReminderRecipient")
    public record Recipient(String name, String displayName) {}

    @Schema(name = "ReminderCardOption")
    public record CardOption(String name, String displayName) {}

    @Schema(name = "ReminderChannelsView")
    public record ChannelsView(List<ChannelView> channels, List<Recipient> recipients, List<CardOption> cards) {}

    static final String DEFAULT_SEND_TIME = "09:00";
    static final List<Integer> DEFAULT_DAYS = List.of(7, 3, 1);
    private static final java.util.regex.Pattern TIME =
            java.util.regex.Pattern.compile("(?:[01][0-9]|2[0-3]):[0-5][0-9]");

    Mono<ChannelsView> view() {
        var now = ZonedDateTime.now(clock.withZone(RenewalSettings.ZONE));
        return channels()
                .map(channel -> viewOf(channel, now))
                .collectList()
                .zipWith(client.list(User.class, ReminderService::activeUser, null)
                        .filterWhen(user -> canManage(user.getMetadata().getName()))
                        .map(user -> new Recipient(
                                user.getMetadata().getName(), user.getSpec().getDisplayName()))
                        .collectList())
                .zipWith(client.list(
                                SimCard.class,
                                card -> card.getMetadata().getDeletionTimestamp() == null,
                                PublicCard.SOURCE_ORDER)
                        .map(card -> new CardOption(
                                card.getMetadata().getName(), card.getSpec().getName()))
                        .collectList())
                .map(result ->
                        new ChannelsView(result.getT1().getT1(), result.getT1().getT2(), result.getT2()));
    }

    private Flux<ReminderChannel> channels() {
        return client.list(
                ReminderChannel.class,
                channel -> channel.getMetadata().getDeletionTimestamp() == null,
                Comparator.comparing(channel -> channel.getMetadata().getName()));
    }

    /** 旧渠道没有这些字段，读取时补默认值：09:00、全部号卡、提前 7/3/1 天。 */
    static String sendTime(ReminderChannel.Spec spec) {
        return spec.getSendTime() != null && TIME.matcher(spec.getSendTime()).matches()
                ? spec.getSendTime()
                : DEFAULT_SEND_TIME;
    }

    static boolean allCards(ReminderChannel.Spec spec) {
        return !"selected".equals(spec.getCardScope());
    }

    static List<Integer> daysAhead(ReminderChannel.Spec spec) {
        return spec.getDaysAhead() == null || spec.getDaysAhead().isEmpty() ? DEFAULT_DAYS : spec.getDaysAhead();
    }

    static ChannelView viewOf(ReminderChannel channel) {
        return viewOf(channel, ZonedDateTime.now(RenewalSettings.ZONE));
    }

    static ChannelView viewOf(ReminderChannel channel, ZonedDateTime now) {
        var spec = channel.getSpec();
        var next = now.toLocalDate().atTime(LocalTime.parse(sendTime(spec))).atZone(RenewalSettings.ZONE);
        if (!next.isAfter(now)) next = next.plusDays(1);
        return new ChannelView(
                channel.getMetadata().getName(),
                spec.getDisplayName(),
                spec.isEnabled(),
                spec.getRecipients(),
                spec.getEvents(),
                sendTime(spec),
                allCards(spec) ? "all" : "selected",
                spec.getCards() == null ? List.of() : spec.getCards(),
                daysAhead(spec),
                channel.getMetadata().getVersion(),
                next.toInstant().toString(),
                spec.getLastSubmittedAt(),
                spec.getLastError(),
                spec.getLastTestAt(),
                spec.getLastTestError());
    }

    Mono<ReminderChannel> get(String name) {
        return client.fetch(ReminderChannel.class, name)
                .filter(channel -> channel.getMetadata().getDeletionTimestamp() == null)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "渠道不存在")));
    }

    Mono<ChannelView> save(ChannelInput raw) {
        return Mono.defer(() -> {
            var input = normalized(raw);
            validate(input);
            var name = input.name() == null ? "channel-" + UUID.randomUUID() : input.name();
            return exclusive(
                    name,
                    () -> Flux.fromIterable(input.recipients())
                            .concatMap(username -> canManage(username)
                                    .flatMap(allowed -> allowed
                                            ? Mono.just(username)
                                            : Mono.error(BackupService.bad("接收用户不存在、已停用或无号卡管理权限"))))
                            .then(
                                    input.name() == null
                                            ? Mono.fromSupplier(() -> {
                                                var channel = new ReminderChannel();
                                                var metadata = new Metadata();
                                                metadata.setName(name);
                                                channel.setMetadata(metadata);
                                                channel.setSpec(new ReminderChannel.Spec());
                                                return channel;
                                            })
                                            : get(name))
                            .flatMap(channel -> {
                                if (!Objects.equals(
                                        input.version(), channel.getMetadata().getVersion()))
                                    return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "渠道已变化，请重新加载"));
                                var previous = List.copyOf(channel.getSpec().getRecipients());
                                var spec = channel.getSpec();
                                spec.setDisplayName(input.displayName().strip());
                                spec.setEnabled(input.enabled());
                                spec.setRecipients(List.copyOf(input.recipients()));
                                spec.setEvents(List.copyOf(input.events()));
                                spec.setSendTime(input.sendTime());
                                spec.setCardScope(input.cardScope());
                                spec.setCards(
                                        "selected".equals(input.cardScope()) ? List.copyOf(input.cards()) : List.of());
                                spec.setDaysAhead(input.daysAhead().stream()
                                        .sorted(Comparator.reverseOrder())
                                        .toList());
                                spec.getSubmittedDates().keySet().retainAll(input.recipients());
                                // 编辑或启停不覆盖用户的退订选择，仅为新接收人建立订阅。
                                return Flux.fromIterable(input.recipients())
                                        .filter(user -> !previous.contains(user))
                                        .concatMap(
                                                user -> notifications.subscribe(subscriber(user), interest(name, user)))
                                        .then()
                                        .thenMany(Flux.fromIterable(previous)
                                                .filter(user ->
                                                        !input.recipients().contains(user))
                                                .concatMap(user -> notifications.unsubscribe(
                                                        subscriber(user), interest(name, user))))
                                        .then(Mono.defer(() -> persist(channel)));
                            })
                            .map(ReminderService::viewOf));
        });
    }

    static void validate(ChannelInput input) {
        if (input.displayName() == null
                || input.displayName().isBlank()
                || input.displayName().length() > 100
                || input.recipients() == null
                || input.recipients().isEmpty()
                || input.recipients().size() > 50
                || input.recipients().stream().anyMatch(user -> user == null || user.isBlank())
                || input.recipients().stream().distinct().count()
                        != input.recipients().size()
                || input.events() == null
                || input.events().isEmpty()
                || input.events().stream().anyMatch(event -> event == null || !EVENTS.contains(event))
                || input.events().stream().distinct().count() != input.events().size())
            throw BackupService.bad("请填写渠道名称、1 至 50 位不同接收用户及有效提醒范围");
        if (input.sendTime() == null || !TIME.matcher(input.sendTime()).matches())
            throw BackupService.bad("发送时间必须为 HH:mm");
        if (!"all".equals(input.cardScope()) && !"selected".equals(input.cardScope()))
            throw BackupService.bad("请选择提醒全部号卡或指定号卡");
        if ("selected".equals(input.cardScope())
                && (input.cards() == null
                        || input.cards().isEmpty()
                        || input.cards().size() > 1000
                        || input.cards().stream().anyMatch(card -> card == null || card.isBlank())
                        || input.cards().stream().distinct().count()
                                != input.cards().size())) throw BackupService.bad("请至少选择一张号卡");
        if (input.events().contains("upcoming")
                && (input.daysAhead() == null
                        || input.daysAhead().isEmpty()
                        || input.daysAhead().size() > 20
                        || input.daysAhead().stream().anyMatch(day -> day == null || day < 0 || day > 365)
                        || input.daysAhead().stream().distinct().count()
                                != input.daysAhead().size())) throw BackupService.bad("请选择有效的提前提醒天数");
    }

    /** 前端可能省略未展示的可选字段，统一补齐后再校验；只关心逾期或窗口时天数可留空。 */
    static ChannelInput normalized(ChannelInput input) {
        var upcoming = input.events() != null && input.events().contains("upcoming");
        var days = input.daysAhead() == null || input.daysAhead().isEmpty() && !upcoming
                ? DEFAULT_DAYS
                : input.daysAhead();
        return new ChannelInput(
                input.name(),
                input.displayName(),
                input.enabled(),
                input.recipients(),
                input.events(),
                input.sendTime() == null ? DEFAULT_SEND_TIME : input.sendTime(),
                input.cardScope() == null ? "all" : input.cardScope(),
                input.cards() == null ? List.of() : input.cards(),
                days,
                input.version());
    }

    Mono<Void> delete(String name, Long version) {
        return exclusive(
                name,
                () -> get(name).flatMap(channel -> {
                    if (!Objects.equals(version, channel.getMetadata().getVersion()))
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "渠道已变化，请重新加载"));
                    return Flux.fromIterable(channel.getSpec().getRecipients())
                            .concatMap(user -> notifications.unsubscribe(subscriber(user), interest(name, user)))
                            .then(Mono.defer(() -> client.delete(channel)))
                            .then();
                }));
    }

    Mono<ChannelView> test(String name) {
        return exclusive(
                name,
                () -> get(name).flatMap(channel -> {
                    var failures = new ArrayList<String>();
                    return Flux.fromIterable(channel.getSpec().getRecipients())
                            .concatMap(user -> eligible(name, user)
                                    .flatMap(allowed -> allowed
                                            ? emit(
                                                    channel,
                                                    user,
                                                    "TEST",
                                                    "这是一条 SIMnook 测试通知。",
                                                    "This is a SIMnook test notification.")
                                            : Mono.error(BackupService.bad("用户已停用、无权限或已取消订阅")))
                                    .onErrorResume(error -> {
                                        log.warn("SIMnook 渠道 {} 测试失败，用户 {}", name, user, error);
                                        failures.add(user);
                                        return Mono.empty();
                                    }))
                            .then(Mono.defer(() -> {
                                channel.getSpec().setLastTestAt(clock.instant().toString());
                                channel.getSpec()
                                        .setLastTestError(
                                                failures.isEmpty() ? null : "测试未提交：" + String.join("、", failures));
                                return persist(channel).map(ReminderService::viewOf);
                            }));
                }));
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 10_000)
    public Mono<Void> check() {
        return Mono.defer(() -> {
                    var now = ZonedDateTime.now(clock.withZone(RenewalSettings.ZONE));
                    var today = now.toLocalDate();
                    return client.list(
                                    SimCard.class,
                                    card -> card.getMetadata().getDeletionTimestamp() == null,
                                    PublicCard.SOURCE_ORDER)
                            .collectList()
                            .flatMap(cards -> channels()
                                    // 每个渠道按自己的发送时间触发，未到时间的渠道本轮跳过。
                                    .filter(channel -> channel.getSpec().isEnabled()
                                            && !now.toLocalTime()
                                                    .isBefore(LocalTime.parse(sendTime(channel.getSpec()))))
                                    .concatMap(channel -> exclusive(
                                                    channel.getMetadata().getName(),
                                                    () -> get(channel.getMetadata()
                                                                    .getName())
                                                            .filter(latest -> latest.getSpec()
                                                                    .isEnabled())
                                                            .flatMap(latest -> deliver(latest, cards, today)))
                                            .onErrorResume(error -> {
                                                log.error(
                                                        "SIMnook 渠道检查失败：{}",
                                                        channel.getMetadata().getName(),
                                                        error);
                                                return Mono.empty();
                                            }))
                                    .then());
                })
                .doOnError(error -> log.error("SIMnook 续费提醒检查失败", error))
                .onErrorComplete();
    }

    /** 判断号卡今天是否需要通过该渠道提醒： 逾期、续费窗口开放按勾选的类型每天提醒；即将到期只在距续费起始日恰好为勾选天数的那天提醒。 */
    static boolean matches(ReminderChannel channel, SimCard card, LocalDate today) {
        var spec = channel.getSpec();
        if (!allCards(spec)
                && (spec.getCards() == null
                        || !spec.getCards().contains(card.getMetadata().getName()))) return false;
        var facts = CardFacts.from(card.getSpec(), today);
        var urgency = CardFacts.urgencyCode(facts.status(), facts.renewal(), facts.renewalEnd(), today);
        if ("cancelled".equals(urgency) || "noDate".equals(urgency)) return false;
        if ("overdue".equals(urgency) || "window".equals(urgency))
            return spec.getEvents().contains(urgency);
        var days = ChronoUnit.DAYS.between(today, LocalDate.parse(facts.renewal()));
        return spec.getEvents().contains("upcoming") && daysAhead(spec).contains((int) days);
    }

    Mono<Void> deliver(ReminderChannel channel, List<SimCard> allCards, LocalDate today) {
        var cards =
                allCards.stream().filter(card -> matches(channel, card, today)).toList();
        if (cards.isEmpty()) return Mono.empty();
        if (channel.getSpec().getLastError() == null
                && channel.getSpec().getRecipients().stream()
                        .allMatch(user -> today.toString()
                                .equals(channel.getSpec().getSubmittedDates().get(user)))) return Mono.empty();
        var name = channel.getMetadata().getName();
        var failures = new ArrayList<String>();
        // 保存每名接收人的进度，后续用户失败时不重发已经提交的用户。
        return Flux.fromIterable(channel.getSpec().getRecipients())
                .filter(user -> !today.toString()
                        .equals(channel.getSpec().getSubmittedDates().get(user)))
                .concatMap(user -> eligible(name, user)
                        .flatMap(allowed -> {
                            if (!allowed) {
                                failures.add(user);
                                return Mono.empty();
                            }
                            return emit(
                                            channel,
                                            user,
                                            today.toString(),
                                            summary(cards, today, Locale.SIMPLIFIED_CHINESE),
                                            summary(cards, today, Locale.ENGLISH))
                                    .then(Mono.defer(() -> {
                                        channel.getSpec().getSubmittedDates().put(user, today.toString());
                                        channel.getSpec()
                                                .setLastSubmittedAt(
                                                        clock.instant().toString());
                                        return persist(channel)
                                                .doOnNext(saved -> channel.setMetadata(saved.getMetadata()));
                                    }))
                                    .then();
                        })
                        .onErrorResume(error -> {
                            log.error("SIMnook 渠道 {} 提交失败，用户 {}", name, user, error);
                            failures.add(user);
                            return Mono.empty();
                        }))
                .then(Mono.defer(() -> {
                    channel.getSpec()
                            .setLastError(
                                    failures.isEmpty()
                                            ? null
                                            : "未提交给 Halo：" + String.join("、", failures) + "。请检查用户权限、订阅和 Halo 通知设置。");
                    return persist(channel);
                }))
                .then();
    }

    private Mono<Void> emit(ReminderChannel channel, String user, String date, String summary, String summaryEn) {
        return emitter.emit(
                REASON,
                builder -> builder.author(UserIdentity.of("system"))
                        .subject(Reason.Subject.builder()
                                .apiVersion("v1alpha1")
                                .kind("ConfigMap")
                                .name("simnook-config")
                                .title("SIMnook")
                                .url(links.processLink("/console/simnook"))
                                .build())
                        .attribute("recipient", user)
                        .attribute("channel", channel.getMetadata().getName())
                        .attribute("businessDate", date)
                        .attribute("summary", summary)
                        .attribute("summaryEn", summaryEn));
    }

    private Mono<Boolean> eligible(String name, String user) {
        return canManage(user)
                .flatMap(allowed -> !allowed
                        ? Mono.just(false)
                        : client.list(
                                        Subscription.class,
                                        sub -> sub.getMetadata().getDeletionTimestamp() == null
                                                && sub.getSpec() != null
                                                && sub.getSpec().getSubscriber() != null
                                                && user.equals(sub.getSpec()
                                                        .getSubscriber()
                                                        .getName())
                                                && sub.getSpec().getReason() != null
                                                && REASON.equals(sub.getSpec()
                                                        .getReason()
                                                        .getReasonType())
                                                && interest(name, user)
                                                        .getExpression()
                                                        .equals(sub.getSpec()
                                                                .getReason()
                                                                .getExpression()),
                                        null)
                                .hasElements());
    }

    Mono<Boolean> canManage(String username) {
        return client.fetch(User.class, username)
                .filter(ReminderService::activeUser)
                .flatMap(user -> roles.getRolesByUsername(username)
                        .collectList()
                        .flatMapMany(names -> roles.listPermissions(new HashSet<>(names)))
                        .flatMapIterable(role -> role.getRules() == null ? List.of() : role.getRules())
                        .any(rule -> contains(rule.getApiGroups(), "console.api.simnook.halo.run")
                                && contains(rule.getResources(), "cardviews")
                                && (contains(rule.getVerbs(), "list") || contains(rule.getVerbs(), "get"))
                                && (rule.getResourceNames() == null || rule.getResourceNames().length == 0)))
                .defaultIfEmpty(false);
    }

    private static boolean activeUser(User user) {
        return user.getMetadata().getDeletionTimestamp() == null
                && user.getSpec() != null
                && !Boolean.TRUE.equals(user.getSpec().getDisabled());
    }

    private static boolean contains(String[] values, String value) {
        return values != null
                && (Arrays.asList(values).contains("*") || Arrays.asList(values).contains(value));
    }

    private static Subscription.Subscriber subscriber(String username) {
        var subscriber = new Subscription.Subscriber();
        subscriber.setName(username);
        return subscriber;
    }

    static Subscription.InterestReason interest(String channel, String username) {
        var interest = new Subscription.InterestReason();
        interest.setReasonType(REASON);
        interest.setExpression("props.channel == '" + channel.replace("'", "''") + "' && props.recipient == '"
                + username.replace("'", "''") + "'");
        return interest;
    }

    private Mono<ReminderChannel> persist(ReminderChannel channel) {
        return channel.getMetadata().getVersion() == null ? client.create(channel) : client.update(channel);
    }

    private <T> Mono<T> exclusive(String name, Supplier<Mono<T>> operation) {
        return Mono.defer(() -> {
            if (!busy.add(name)) return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "渠道正在处理，请稍后重试"));
            return Mono.defer(operation).doFinally(signal -> busy.remove(name));
        });
    }

    static String summary(List<SimCard> cards, LocalDate today, Locale locale) {
        return cards.stream()
                .map(card -> {
                    var view = PublicCard.from(card, today);
                    return view.name() + " · " + view.numberMasked() + " | "
                            + CardFacts.urgency(
                                    view.status(), view.nextRenewalAt(), view.nextRenewalEnd(), today, locale)
                            + " | " + view.nextRenewalAt()
                            + (view.nextRenewalEnd() == null ? "" : " ~ " + view.nextRenewalEnd());
                })
                .collect(java.util.stream.Collectors.joining("\n"));
    }
}
