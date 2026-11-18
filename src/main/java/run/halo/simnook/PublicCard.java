package run.halo.simnook;

import static run.halo.simnook.CardFacts.present;
import static run.halo.simnook.CardFacts.text;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** 公开字段白名单。禁止在这里序列化 CardSpec 或 ExpenseRecord。 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record PublicCard(
        String id,
        String name,
        String carrier,
        String last4,
        String numberMasked,
        String region,
        String countryName,
        String typeKind,
        String typeColor,
        String logo,
        List<String> keepAlive,
        List<String> keepAliveCodes,
        int cycleDays,
        String cycleLabel,
        BigDecimal price,
        String currency,
        String expiresAt,
        String nextRenewalAt,
        String nextRenewalEnd,
        String periodStart,
        String status,
        String statusLabel,
        String cancelledAt,
        String firstPurchaseAt,
        BigDecimal totalSpentCNY,
        String urgency,
        int order,
        List<PublicRecord> history) {

    private static final Pattern NAME_SEPARATOR = Pattern.compile("\\s*[·•|]\\s*");
    private static final JsonMapper JSON = JsonMapper.builder().build();
    static final Comparator<SimCard> SOURCE_ORDER = Comparator.comparing(
                    (SimCard card) -> card.getMetadata().getCreationTimestamp(),
                    Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(card -> card.getMetadata().getName());

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record PublicRecord(
            String id,
            String type,
            String kind,
            String date,
            BigDecimal amount,
            String currency,
            BigDecimal rate,
            BigDecimal amountCNY,
            String extendsTo) {}

    public static PublicCard from(SimCard card, LocalDate today) {
        return from(card, today, 0);
    }

    static PublicCard from(SimCard card, LocalDate today, int sourceOrder) {
        var spec = card.getSpec();
        var facts = CardFacts.from(spec, today);
        var records = CardFacts.records(spec);
        var cardId = card.getMetadata().getName();
        var history = IntStream.range(0, records.size())
                .boxed()
                .sorted(Comparator.comparing(records::get, CardFacts.RECORD_ORDER.reversed()))
                .map(index -> {
                    var record = records.get(index);
                    return new PublicRecord(
                            recordId(cardId, record, index),
                            text(record.getType()),
                            CardFacts.recordKind(record.getType()),
                            CardFacts.date(record.getDate()),
                            record.getAmount(),
                            currency(record.getCurrency()),
                            record.getRate(),
                            CardFacts.amountCny(record),
                            CardFacts.date(record.getExtendsTo()));
                })
                .toList();
        var nameParts = NAME_SEPARATOR.split(spec.getName(), 3);
        var number = digits(spec.getNumber());
        var tail = number.length() >= 4 ? last4(number) : nameParts.length >= 2 ? last4(digits(nameParts[1])) : "";
        var carrier = present(spec.getCarrier()) ? spec.getCarrier() : spec.getName();
        if (!present(spec.getCarrier()) && nameParts.length > 0 && present(nameParts[0])) {
            carrier = nameParts[0].strip();
        }
        var keepAlive = spec.getKeepAlive() == null ? List.<String>of() : spec.getKeepAlive();
        return new PublicCard(
                cardId,
                spec.getName(),
                carrier,
                tail,
                tail.isEmpty() ? "••••" : "•••• " + tail,
                text(spec.getRegion()),
                text(spec.getCountryName()),
                CardFacts.typeKind(spec),
                CardFacts.typeColor(spec),
                spec.getLogo(),
                keepAlive,
                keepAlive.stream().map(CardFacts::keepAliveCode).toList(),
                CardFacts.cycleDays(spec),
                text(spec.getCycleLabel()),
                Objects.requireNonNullElse(spec.getPrice(), BigDecimal.ZERO),
                currency(spec.getCurrency()),
                facts.expiry(),
                facts.renewal(),
                facts.renewalEnd(),
                facts.periodStart(),
                facts.status(),
                facts.statusLabel(),
                CardFacts.date(spec.getCancelledAt()),
                facts.firstPurchase(),
                history.stream()
                        .map(PublicRecord::amountCNY)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                facts.urgency(),
                Objects.requireNonNullElse(spec.getOrder(), sourceOrder),
                history);
    }

    /**
     * 号卡可公开字段的完整清单，顺序即后台展示顺序。publicFields 为空值时取 {@link #DEFAULT_PUBLIC_FIELDS}， 与早期版本访客可见的内容一致；空列表表示全部私有。新增字段时同步
     * SimCard 的 allowableValues 和后台选项。
     */
    static final List<String> PUBLIC_FIELDS = List.of(
            "name",
            "carrier",
            "last4",
            "number",
            "country",
            "type",
            "keepAlive",
            "cycle",
            "price",
            "dates",
            "firstPurchase",
            "history",
            "device",
            "balance",
            "account",
            "services");

    static final List<String> DEFAULT_PUBLIC_FIELDS = List.of(
            "name",
            "carrier",
            "last4",
            "country",
            "type",
            "keepAlive",
            "cycle",
            "price",
            "dates",
            "firstPurchase",
            "history");

    static List<String> publicFields(SimCard.CardSpec spec) {
        var chosen = spec.getPublicFields() == null ? DEFAULT_PUBLIC_FIELDS : spec.getPublicFields();
        return PUBLIC_FIELDS.stream().filter(chosen::contains).toList();
    }

    /** 匿名访客视图：只保留号卡公开列表中的字段，未公开的字段置空，前台按 publicFields 显示「登录后可见」。 */
    static Map<String, Object> visitorView(SimCard card, LocalDate today, int sourceOrder, Locale locale) {
        var spec = card.getSpec();
        var selected = publicFields(spec);
        Map<String, Object> view = JSON.convertValue(from(card, today, sourceOrder), new TypeReference<>() {});
        for (var field : PUBLIC_FIELDS) {
            var shown = selected.contains(field);
            switch (field) {
                case "name", "number" -> {
                    // 名称依赖末四位的隐藏结果，在循环后统一处理。
                    if (shown && "number".equals(field)) view.put("number", text(spec.getNumber()));
                }
                case "carrier" -> {
                    if (!shown) {
                        view.put("carrier", "");
                        view.put("logo", null);
                    }
                }
                case "last4" -> {
                    if (!shown) {
                        view.put("last4", "");
                        view.put("numberMasked", "••••");
                    }
                }
                case "country" -> {
                    if (!shown) {
                        view.put("region", "");
                        view.put("countryName", "");
                    }
                }
                case "type" -> {
                    if (shown) view.put("type", CardFacts.typeText(spec, locale));
                    else {
                        view.put("typeKind", null);
                        view.put("typeColor", "default");
                    }
                }
                case "keepAlive" -> {
                    if (!shown) {
                        view.put("keepAlive", List.of());
                        view.put("keepAliveCodes", List.of());
                    }
                }
                case "cycle" -> {
                    if (!shown) {
                        view.put("cycleDays", 0);
                        view.put("cycleLabel", "");
                    }
                }
                case "price" -> {
                    // 保持数值类型以兼容前台类型定义，前台按 publicFields 显示「登录后可见」。
                    if (!shown) view.put("price", 0);
                }
                case "dates" -> {
                    if (!shown) {
                        for (var key : List.of(
                                "expiresAt",
                                "nextRenewalAt",
                                "nextRenewalEnd",
                                "periodStart",
                                "cancelledAt",
                                "urgency")) view.put(key, null);
                    }
                }
                case "firstPurchase" -> {
                    if (!shown) view.put("firstPurchaseAt", null);
                }
                case "history" -> {
                    if (!shown) {
                        view.put("history", List.of());
                        view.put("totalSpentCNY", null);
                    }
                }
                case "device" -> {
                    if (shown) view.put("device", spec.getDevice());
                }
                case "balance" -> {
                    if (shown) {
                        view.put("balance", spec.getBalance());
                        view.put("balanceUpdatedAt", CardFacts.date(spec.getBalanceUpdatedAt()));
                    }
                }
                case "account" -> {
                    if (shown) view.put("account", spec.getAccount());
                }
                case "services" -> {
                    if (shown) view.put("services", spec.getServices() == null ? List.of() : spec.getServices());
                }
                default -> throw new IllegalStateException(field);
            }
        }
        if (!selected.contains("name")) view.put("name", view.get("numberMasked"));
        view.put("publicFields", selected);
        return view;
    }

    static String recordId(String cardId, SimCard.ExpenseRecord record, int index) {
        return cardId + "-" + (present(record.getId()) ? record.getId() : index);
    }

    static String digits(String value) {
        return text(value).replaceAll("[^0-9]", "");
    }

    private static String last4(String digits) {
        return digits.substring(Math.max(0, digits.length() - 4));
    }

    private static String currency(String currency) {
        return present(currency) ? currency : "CNY";
    }
}
