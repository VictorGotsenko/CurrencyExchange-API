package currencyexchange.util;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExchangeRateUtils {

        public BigDecimal getRate(String rate) {
         // Регулярное выражение для поиска чисел с запятой (точкой)
         // String regex = "\\d+(,\\d+)?";
        String regex = "\\d+(.\\d+)?";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(rate);
        String result = "";
        while (matcher.find()) {
            result = matcher.group();   // group() возвращает совпавшую подстроку
            return new BigDecimal(result);
        }

        return new BigDecimal("-1");
    }
}
