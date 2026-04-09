package currencyexchange.servlets;

import currencyexchange.dto.CurrencyDTO;
import currencyexchange.dto.ExchangeRateDTO;
import currencyexchange.model.Currency;
import currencyexchange.model.ExchangeRate;
import currencyexchange.repository.CurrenciesRepository;
import currencyexchange.repository.CurrenciesRepositoryImpl;
import currencyexchange.repository.ExchangeRatesRepository;
import currencyexchange.repository.ExchangeRatesRepositoryImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@WebServlet(name = "ExchangeRatesServlet", urlPatterns = "/exchangerates")
public class ExchangeRatesServlet extends HttpServlet {

    ExchangeRatesRepository exchangeRatesRepository;
    CurrenciesRepository currenciesRepository;

    @Override
    public void init() throws ServletException {
        Connection connection = (Connection) getServletContext().getAttribute("ConnectionToDB");
        exchangeRatesRepository = new ExchangeRatesRepositoryImpl(connection);
        currenciesRepository = new CurrenciesRepositoryImpl(connection);

    }

    @SneakyThrows
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        /* **************************************
        Получение списка всех обменных курсов
        GET /exchangeRates
        {
          "id": 0,
          "baseCurrency": {
             "id": 0,
             "name": "United States dollar",
             "code": "USD",
             "sign": "$"
           },
           "targetCurrency": {
             "id": 1,
             "name": "Euro",
             "code": "EUR",
             "sign": "€"
          },
         "rate": 0.99
         }
         HTTP коды ответов:
          Успех - 200
          Ошибка (например, база данных недоступна) - 500
         ************************************** */

        List<ExchangeRate> exchangeRateList = exchangeRatesRepository.getExchangeRates();

        if (exchangeRateList.isEmpty()) {
            //err 500
            // 1. Устанавливаем статус 500
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            // 2. Устанавливаем тип контента JSON
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            // 3. Формируем JSON-ответ
            String jsonError = String.format(
                    "{\"error\": \"Internal Server Error\", \"message\": \"%s\"}",
                    "Произошла ошибка при обработке запроса"
            );

            // 4. Отправляем ответ
            PrintWriter out = response.getWriter();
            out.print(jsonError);
            out.flush();
        } else {
            // send JSON - 200 Ok
            List<ExchangeRateDTO> exchangeRateDTOList = new ArrayList<>();
            for (ExchangeRate exchangeRate : exchangeRateList) {
                int id = exchangeRate.getId();
                int baseCurrency = exchangeRate.getBaseCurrencyCode();
                int targetCurrency = exchangeRate.getTargetCurrencyCode();
                double rate = exchangeRate.getRate();

                CurrencyDTO baseCurrencyDTO = Currency2DTO(baseCurrency);
                CurrencyDTO targetCurrencyDTO = Currency2DTO(targetCurrency);
                exchangeRateDTOList.add(new ExchangeRateDTO(id, baseCurrencyDTO, targetCurrencyDTO, rate));
            }

            PrintWriter printWriter = response.getWriter();

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Security-Policy", "default-src 'self';"); //CSP (Content Security Policy)

            ObjectMapper mapper = new ObjectMapper();
            printWriter.println(mapper.writeValueAsString(exchangeRateDTOList));
        }
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
