package currencyexchange.servise;

import java.math.BigDecimal;
import java.util.Map;

public interface ExchangeService {

    //Расчёт перевода определённого количества средств из одной валюты в другую
    Map<String, BigDecimal> crossRateCalculation(String baseCurrencyCode, String targetCurrencyCode, BigDecimal amount);

}
