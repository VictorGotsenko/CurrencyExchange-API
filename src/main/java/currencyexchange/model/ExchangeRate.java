package currencyexchange.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class ExchangeRate extends BaseModel {
    int id;
    int baseCurrencyCode;
    int targetCurrencyCode;
    double rate;
    private LocalDateTime createdAt;

    public ExchangeRate(int baseCurrencyCode, int targetCurrencyCode, double rate) {
        this.baseCurrencyCode = baseCurrencyCode;
        this.targetCurrencyCode = targetCurrencyCode;
        this.rate = rate;
    }
}
