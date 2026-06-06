package currencyexchange.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@AllArgsConstructor
@Getter
public class ExchangeRateDTO {
    int id;
    CurrencyDto baseCurrency;
    CurrencyDto targetCurrency;
    BigDecimal rate;
}
