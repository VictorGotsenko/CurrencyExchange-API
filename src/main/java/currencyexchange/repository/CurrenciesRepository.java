package currencyexchange.repository;

import currencyexchange.model.Currency;

import java.util.List;
import java.util.Optional;

public interface CurrenciesRepository {

    List<Currency> getCurrencies();

    Optional<Currency> findByCode(String code);

    Optional<Currency> findById(int id);

    void save(Currency currency);
}
