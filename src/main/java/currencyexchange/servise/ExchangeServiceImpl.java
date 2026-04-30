package currencyexchange.servise;

import currencyexchange.model.ExchangeRate;
import currencyexchange.repository.ExchangeRatesRepository;
import currencyexchange.repository.ExchangeRatesRepositoryImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ExchangeServiceImpl implements ExchangeService {

    ExchangeRatesRepository exchangeRatesRepository;

    public ExchangeServiceImpl(Connection connection) {
        exchangeRatesRepository = new ExchangeRatesRepositoryImpl(connection);
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
        try {
            // найти АБ прямой
            Optional<ExchangeRate> exchangeRateOptional = exchangeRatesRepository
                    .findByCodes(baseCurrencyCode, targetCurrencyCode);

            if (exchangeRateOptional.isPresent()) {
                ExchangeRate exchangeRateAB = exchangeRateOptional.get();

                BigDecimal convertedAmount = exchangeRateAB.getRate().multiply(amount);
                result.put("rate", exchangeRateAB.getRate());
                result.put("convertedAmount", convertedAmount);
                return result;
            }

            // найти БА обратный
            exchangeRateOptional = exchangeRatesRepository
                    .findByCodes(targetCurrencyCode, baseCurrencyCode);

            if (exchangeRateOptional.isPresent()) {
                ExchangeRate exchangeRateAB = exchangeRateOptional.get();
                BigDecimal convertedAmount = exchangeRateAB.getRate().multiply(amount);
                result.put("rate", exchangeRateAB.getRate());
                result.put("convertedAmount", convertedAmount);
                return result;
            }

            // найти USD-A и USD-B
            Optional<ExchangeRate> desireExchangeUSDtoBaseCurrency = exchangeRatesRepository
                    .findByCodes("USD", baseCurrencyCode);
            Optional<ExchangeRate> desireExchangeUSDtoTargetCurrency = exchangeRatesRepository
                    .findByCodes("USD", targetCurrencyCode);

            ExchangeRate exchangeRateUSD_A;
            if (desireExchangeUSDtoBaseCurrency.isPresent()) {
                exchangeRateUSD_A = desireExchangeUSDtoBaseCurrency.get();
            } else {
                return result;
            }

            ExchangeRate exchangeRateUSD_B;
            if (desireExchangeUSDtoTargetCurrency.isPresent()) {
                exchangeRateUSD_B = desireExchangeUSDtoTargetCurrency.get();
            } else {
                return result;
            }

            BigDecimal rate = exchangeRateUSD_B.getRate().divide(
                    exchangeRateUSD_A.getRate(), 6, RoundingMode.HALF_UP);
            BigDecimal convertedAmount = rate.multiply(amount);
            result.put("rate", rate);
            result.put("convertedAmount", convertedAmount);
            return result;
        } catch (SQLException e) {
            System.err.println("Ошибка при работе с БД: " + e.getMessage());
            // e.printStackTrace();
        }
        return result;
    }
}
