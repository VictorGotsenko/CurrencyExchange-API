package currencyexchange.util;

import currencyexchange.dto.CurrencyDto;
import currencyexchange.model.Currency;
import currencyexchange.repository.CurrenciesRepository;
import currencyexchange.repository.CurrenciesRepositoryImpl;
import currencyexchange.repository.ExchangeRatesRepository;
import currencyexchange.repository.ExchangeRatesRepositoryImpl;
import lombok.SneakyThrows;

import java.sql.Connection;
import java.util.Optional;

public final class ConverterDTOs {

    CurrenciesRepository currenciesRepository;
    ExchangeRatesRepository exchangeRatesRepository;

    public ConverterDTOs(Connection connection) {
        this.exchangeRatesRepository = new ExchangeRatesRepositoryImpl(connection);
        this.currenciesRepository = new CurrenciesRepositoryImpl(connection);
    }

    @SneakyThrows
    public CurrencyDto currencyToDTO(int id) {

        Optional<Currency> newCurrency = currenciesRepository.findById(id);

        if (newCurrency.isEmpty()) {
            return new CurrencyDto(-1, "", "", "");
        }

        Currency result = newCurrency.get();
        CurrencyDto currencyDTO = new CurrencyDto(
                result.getId(),
                result.getName(),
                result.getCode(),
                result.getSign());
        return currencyDTO;
    }
}
