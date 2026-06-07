package currencyexchange.util;

import com.zaxxer.hikari.HikariDataSource;
import currencyexchange.dto.CurrencyDto;
import currencyexchange.model.Currency;
import currencyexchange.repository.CurrenciesRepository;
import currencyexchange.repository.CurrenciesRepositoryImpl;
import currencyexchange.repository.ExchangeRatesRepository;
import currencyexchange.repository.ExchangeRatesRepositoryImpl;
import lombok.SneakyThrows;

import java.util.Optional;

public final class ConverterDTOs {

    CurrenciesRepository currenciesRepository;
    ExchangeRatesRepository exchangeRatesRepository;

    public ConverterDTOs(HikariDataSource dataSource) {
        this.exchangeRatesRepository = new ExchangeRatesRepositoryImpl(dataSource);
        this.currenciesRepository = new CurrenciesRepositoryImpl(dataSource);
    }

    @SneakyThrows
    public CurrencyDto currencyToDTO(int id) {

        Optional<Currency> newCurrency = currenciesRepository.findById(id);

        if (newCurrency.isEmpty()) {
            return new CurrencyDto(-1, "", "", "");
        }

        Currency result = newCurrency.get();
        return new CurrencyDto(
                result.getId(),
                result.getName(),
                result.getCode(),
                result.getSign());
    }
}
