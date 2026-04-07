package currencyexchange.model;


import currencyexchange.dto.CurrenciesDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class ExchangeRate {
    int id;
    CurrenciesDTO baseCurrencyCode;
    CurrenciesDTO targetCurrencyCode;
    double rate;
}
