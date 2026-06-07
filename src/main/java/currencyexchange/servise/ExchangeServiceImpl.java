package currencyexchange.servise;

import com.zaxxer.hikari.HikariDataSource;
import currencyexchange.model.Currency;
import currencyexchange.model.ExchangeRate;
import currencyexchange.repository.CurrenciesRepository;
import currencyexchange.repository.CurrenciesRepositoryImpl;
import currencyexchange.repository.ExchangeRatesRepository;
import currencyexchange.repository.ExchangeRatesRepositoryImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ExchangeServiceImpl implements ExchangeService {

    CurrenciesRepository currenciesRepository;
    ExchangeRatesRepository exchangeRatesRepository;

    public ExchangeServiceImpl(HikariDataSource dataSource) {
        currenciesRepository = new CurrenciesRepositoryImpl(dataSource);
        exchangeRatesRepository = new ExchangeRatesRepositoryImpl(dataSource);
    }

    @Override
    public Map<String, BigDecimal> crossRateCalculation(String baseCurrencyCode,
                                                        String targetCurrencyCode, BigDecimal amount) {
        /*
        Получение курса для обмена может пройти по одному из трёх сценариев:
          Допустим, совершаем перевод из валюты A в валюту B:
          1. В таблице ExchangeRates существует валютная пара AB - берём её курс
          2. В таблице ExchangeRates существует валютная пара BA - берем её курс,
             и считаем обратный, чтобы получить AB
          3. В таблице ExchangeRates существуют валютные пары USD-A и USD-B - вычисляем из этих курсов курс AB
          Остальные возможные сценарии, для упрощения, опустим.
         */

        Map<String, BigDecimal> result = new HashMap<>();

        Optional<Currency> baseCurrency = currenciesRepository.findByCode(baseCurrencyCode);
        if (baseCurrency.isEmpty()) {
            return result;
        }
        int baseCurrencyId = baseCurrency.get().getId();

        Optional<Currency> targetCurrency = currenciesRepository.findByCode(targetCurrencyCode);
        if (targetCurrency.isEmpty()) {
            return result;
        }
        int targetCurrencyId = targetCurrency.get().getId();
        Optional<Currency> usd = currenciesRepository.findByCode("USD");
        if (usd.isEmpty()) {
            return result;
        }
        int usdId = usd.get().getId();

        // direct A-B
        Optional<ExchangeRate> exchangeRateOptional = exchangeRatesRepository
                .findByCurrencyIDs(baseCurrencyId, targetCurrencyId);

        if (exchangeRateOptional.isPresent()) {
            ExchangeRate exchangeRateAB = exchangeRateOptional.get();

            BigDecimal convertedAmount = exchangeRateAB.getRate().multiply(amount);
            result.put("rate", exchangeRateAB.getRate());
            result.put("convertedAmount", convertedAmount);
            return result;
        }

        // revers B-A
        exchangeRateOptional = exchangeRatesRepository
                .findByCurrencyIDs(targetCurrencyId, baseCurrencyId);

        if (exchangeRateOptional.isPresent()) {
            ExchangeRate exchangeRateAB = exchangeRateOptional.get();

            // reversRate = 1/directRate
            BigDecimal directRate = exchangeRateAB.getRate();
            BigDecimal reversRate = BigDecimal.ONE.divide(directRate, 6, RoundingMode.HALF_UP);
            BigDecimal convertedAmount = reversRate.multiply(amount);
            result.put("rate", reversRate);
            result.put("convertedAmount", convertedAmount);
            return result;
        }

        // transit USD-A, USD-B -> A-B
        Optional<ExchangeRate> desireExchangeUSDtoBaseCurrency = exchangeRatesRepository
                .findByCurrencyIDs(usdId, baseCurrencyId);
        Optional<ExchangeRate> desireExchangeUSDtoTargetCurrency = exchangeRatesRepository
                .findByCurrencyIDs(usdId, targetCurrencyId);

        ExchangeRate exchangeRateUSDtoA;
        if (desireExchangeUSDtoBaseCurrency.isPresent()) {
            exchangeRateUSDtoA = desireExchangeUSDtoBaseCurrency.get();
        } else {
            return result;
        }

        ExchangeRate exchangeRateUSDtoB;
        if (desireExchangeUSDtoTargetCurrency.isPresent()) {
            exchangeRateUSDtoB = desireExchangeUSDtoTargetCurrency.get();
        } else {
            return result;
        }

        BigDecimal rate = exchangeRateUSDtoB.getRate().divide(
                exchangeRateUSDtoA.getRate(), 6, RoundingMode.HALF_UP);
        BigDecimal convertedAmount = rate.multiply(amount);
        result.put("rate", rate);
        result.put("convertedAmount", convertedAmount);
        return result;
    }
}
