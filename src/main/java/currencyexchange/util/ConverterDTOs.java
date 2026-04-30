package currencyexchange.util;

import currencyexchange.dto.CurrencyDTO;
import currencyexchange.model.Currency;
import currencyexchange.repository.CurrenciesRepository;
import currencyexchange.repository.CurrenciesRepositoryImpl;
import currencyexchange.repository.ExchangeRatesRepository;
import currencyexchange.repository.ExchangeRatesRepositoryImpl;
import lombok.SneakyThrows;

import java.sql.Connection;
import java.util.Optional;

public class ConverterDTOs {

    CurrenciesRepository currenciesRepository;
    ExchangeRatesRepository exchangeRatesRepository;

    public ConverterDTOs(Connection connection) {
        this.exchangeRatesRepository = new ExchangeRatesRepositoryImpl(connection);
        this.currenciesRepository = new CurrenciesRepositoryImpl(connection);
    }

    @SneakyThrows
    public CurrencyDTO Currency2DTO(int id) {

        Optional<Currency> newCurrency = currenciesRepository.findById(id);

        if (newCurrency.isEmpty()) {
            return new CurrencyDTO(-1, "", "", "");
        }

        Currency result = newCurrency.get();
        CurrencyDTO currencyDTO = new CurrencyDTO(
                result.getId(),
                result.getName(),
                result.getCode(),
                result.getSign());
        return currencyDTO;
    }
}
