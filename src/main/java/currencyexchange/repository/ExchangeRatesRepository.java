package currencyexchange.repository;

import currencyexchange.model.ExchangeRate;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface ExchangeRatesRepository {
//ExchangeRate

    List<ExchangeRate> getExchangeRates() throws SQLException;

    Optional<ExchangeRate> findByCode(String baseCurrency) throws SQLException;

    Optional<ExchangeRate> findById(int baseCurrencyId, int targetCurrencyId) throws SQLException;

    void save(ExchangeRate exchangeRate) throws SQLException;
}
