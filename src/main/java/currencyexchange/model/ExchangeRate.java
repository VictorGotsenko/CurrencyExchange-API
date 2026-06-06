package currencyexchange.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
@AllArgsConstructor
public final class ExchangeRate {

    // LONG для ID подходит лучше. Но в SQLite нет long
    // for details https://www.sqlite.org/datatype3.html#storage_classes_and_datatypes
    private int id;
    private int baseCurrencyId;
    private int targetCurrencyId;
    private BigDecimal rate;

    public ExchangeRate(int baseCurrencyId, int targetCurrencyId, BigDecimal rate) {
        this.baseCurrencyId = baseCurrencyId;
        this.targetCurrencyId = targetCurrencyId;
        this.rate = rate;
    }
}
