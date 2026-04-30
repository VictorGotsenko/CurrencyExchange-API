package currencyexchange.repository;

import currencyexchange.model.ExchangeRate;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface ExchangeRatesRepository {
//ExchangeRate

    void save(ExchangeRate exchangeRate) throws SQLException;

    List<ExchangeRate> getExchangeRates() throws SQLException;

    Optional<ExchangeRate> findById(int id) throws SQLException;

    Optional<ExchangeRate> findByCodes(String baseCurrencyCode, String targetCurrencyCode) throws SQLException;

    Optional<ExchangeRate> findByCurrencyIDs(int baseCurrencyId, int targetCurrencyId) throws SQLException;

    void update(int exchangeRateId, BigDecimal rate) throws SQLException;

}
