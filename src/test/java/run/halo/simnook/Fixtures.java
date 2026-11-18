package run.halo.simnook;

import java.math.BigDecimal;
import run.halo.app.extension.Metadata;

/** 测试共用的号卡与消费记录构造。 */
final class Fixtures {
    private Fixtures() {}

    static SimCard card(String id) {
        var card = new SimCard();
        var metadata = new Metadata();
        metadata.setName(id);
        metadata.setVersion(3L);
        card.setMetadata(metadata);
        var spec = new SimCard.CardSpec();
        spec.setName("测试卡");
        spec.setNumber("+86 13800123456");
        spec.setAccount("private-account");
        spec.setDevice("private-device");
        spec.setBalance(new BigDecimal("99"));
        card.setSpec(spec);
        return card;
    }

    static SimCard.ExpenseRecord record(String id, String amount) {
        var row = new SimCard.ExpenseRecord();
        row.setId(id);
        row.setAmount(new BigDecimal(amount));
        row.setCurrency("CNY");
        return row;
    }

    static SimCard.ExpenseRecord renewal(String date, String extendsTo, String amount, String currency, String rate) {
        var row = record(null, amount);
        row.setType("续费/保号");
        row.setDate(date);
        row.setExtendsTo(extendsTo);
        row.setCurrency(currency);
        row.setRate(rate == null ? null : new BigDecimal(rate));
        row.setTitle("private-title");
        return row;
    }
}
