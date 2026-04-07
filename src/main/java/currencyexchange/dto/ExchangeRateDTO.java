package currencyexchange.dto;

import currencyexchange.model.Currency;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ExchangeRateDTO {

    int id;
    Currency baseCurrency;
    Currency targetCurrency;
    double rate;
}
