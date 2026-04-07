package currencyexchange.repository;

import currencyexchange.model.ExchangeRate;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface ExchangeRatesRepository {
//ExchangeRate

    List<ExchangeRate> getExchangeRates() throws SQLException;

    Optional<ExchangeRate> findByCode(String code) throws SQLException;

    void save(ExchangeRate exchangeRate) throws SQLException;
}
