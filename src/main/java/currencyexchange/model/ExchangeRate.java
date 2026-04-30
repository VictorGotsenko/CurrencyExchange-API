package currencyexchange.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class ExchangeRate extends BaseModel {
    int id;
    int baseCurrencyId;
    int targetCurrencyId;
    BigDecimal rate;
    private LocalDateTime createdAt;

    public ExchangeRate(int baseCurrencyId, int targetCurrencyId, String rate) {
        this.baseCurrencyId = baseCurrencyId;
        this.targetCurrencyId = targetCurrencyId;
        this.rate = new BigDecimal(rate);
    }
}
