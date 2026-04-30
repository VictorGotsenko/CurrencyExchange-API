package currencyexchange.repository;

import currencyexchange.model.Currency;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface CurrenciesRepository {

    List<Currency> getCurrencies() throws SQLException;

    Optional<Currency> findByCode(String code) throws SQLException;

    Optional<Currency> findById(int id) throws SQLException;

    void save(Currency currency) throws SQLException;
}
