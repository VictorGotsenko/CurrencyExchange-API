package currencyexchange.repository;

import currencyexchange.model.ExchangeRate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ExchangeRatesRepository {

    void save(ExchangeRate exchangeRate);

    List<ExchangeRate> getExchangeRates();

    Optional<ExchangeRate> findById(int id);

    Optional<ExchangeRate> findByCurrencyIDs(int baseCurrencyId, int targetCurrencyId);

    void update(int exchangeRateId, BigDecimal rate);

}
