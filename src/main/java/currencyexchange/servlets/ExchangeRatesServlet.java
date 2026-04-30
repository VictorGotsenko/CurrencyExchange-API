package currencyexchange.servlets;

import currencyexchange.dto.CurrencyDTO;
import currencyexchange.dto.ExchangeRateDTO;
import currencyexchange.model.Currency;
import currencyexchange.model.ExchangeRate;
import currencyexchange.repository.CurrenciesRepository;
import currencyexchange.repository.CurrenciesRepositoryImpl;
import currencyexchange.repository.ExchangeRatesRepository;
import currencyexchange.repository.ExchangeRatesRepositoryImpl;
import currencyexchange.util.ConverterDTOs;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@WebServlet(name = "ExchangeRatesServlet", urlPatterns = "/exchangeRates")
public class ExchangeRatesServlet extends HttpServlet {

    private ExchangeRatesRepository exchangeRatesRepository;
    private CurrenciesRepository currenciesRepository;
    private ObjectMapper mapper;
    private ConverterDTOs converterDTOs;

    @Override
    public void init() throws ServletException {
        Connection connection = (Connection) getServletContext().getAttribute("ConnectionToDB");
        exchangeRatesRepository = new ExchangeRatesRepositoryImpl(connection);
        currenciesRepository = new CurrenciesRepositoryImpl(connection);
        converterDTOs = new ConverterDTOs(connection);

        // Create and enable features
        mapper = JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

    }

    @SneakyThrows
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        /* **************************************
        Получение списка всех обменных курсов
        GET /exchangeRates
        -----------------------------------------
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
            request.getSession().setAttribute("errorCode", "SC_INTERNAL_SERVER_ERROR");
            String jsonError = String.format(
                    "{\"error\": \"Internal Server Error\", \"message\": \"%s\"}",
                    "Произошла ошибка при обработке запроса"
            );
            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, jsonError);
        }

        List<ExchangeRateDTO> exchangeRateDTOList = new ArrayList<>();
        for (ExchangeRate exchangeRate : exchangeRateList) {
            CurrencyDTO baseCurrencyDTO = converterDTOs
                    .Currency2DTO(exchangeRate.getBaseCurrencyId());

            CurrencyDTO targetCurrencyDTO = converterDTOs
                    .Currency2DTO(exchangeRate.getTargetCurrencyId());

            exchangeRateDTOList.add(new ExchangeRateDTO(
                    exchangeRate.getId(),
                    baseCurrencyDTO,
                    targetCurrencyDTO,
                    exchangeRate.getRate()));
        }

        PrintWriter printWriter = response.getWriter();
        response.setStatus(HttpServletResponse.SC_OK);
        printWriter.println(mapper.writeValueAsString(exchangeRateDTOList));

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
     /* ******************************************
     Добавление нового обменного курса в базу
     POST /exchangeRates
     --------------------------------------------
     Данные передаются в теле запроса в виде полей формы (x-www-form-urlencoded).
     Поля формы - baseCurrencyCode, targetCurrencyCode, rate
     Пример ответа - JSON представление вставленной в базу записи, включая её ID:
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
     --------------------------------------------
     HTTP коды ответов:
     Успех - 201
     Отсутствует нужное поле формы - 400 +
     Валютная пара с таким кодом уже существует - 409 +
     Одна (или обе) валюта из валютной пары не существует в БД - 404 +
     Ошибка (например, база данных недоступна) - 500 +
    ********************************************/


        String baseCurrencyCode = request.getParameter("baseCurrencyCode");
        String targetCurrencyCode = request.getParameter("targetCurrencyCode");
        String rate = request.getParameter("rate");

        if (baseCurrencyCode == null || targetCurrencyCode == null || rate == null) {
            request.getSession().setAttribute("errorCode", "SC_BAD_REQUEST");

            String jsonError = String.format(
                    "{\"error\": \"HTTP Error 400 Bad Request\", \"message\": \"%s\"}",
                    "Отсутствует нужное поле формы"
            );

            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, jsonError);
            return;
        }

        //  Неверный запрос - Дубль в паре  - 400
        if (baseCurrencyCode.equals(targetCurrencyCode)) {
            request.getSession().setAttribute("errorCode", "SC_BAD_REQUEST");

            String jsonError = String.format(
                    "{\"error\": \"HTTP Error 400 Bad Request\", \"message\": \"%s\"}",
                    "Отсутствует нужное поле формы"
            );

            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, jsonError);
            return;
        }

        // checks
        try {
            List<Currency> currencies = currenciesRepository.getCurrencies();
            List<ExchangeRate> exchangeRates = exchangeRatesRepository.getExchangeRates();

            Currency baseCurrency = currencies.stream()
                    .filter(currency -> currency.getCode().equals(baseCurrencyCode))
                    .findFirst().orElse(null);

            Currency targetCurrency = currencies.stream()
                    .filter(currency -> currency.getCode().equals(targetCurrencyCode))
                    .findFirst().orElse(null);


            if (baseCurrency == null || targetCurrency == null) {
                // Одна (или обе) валюта из валютной пары не существует в БД - 404
                String baseCode = (baseCurrency != null) ? "" : baseCurrencyCode;
                String souz = (baseCurrency == null && targetCurrency == null) ? " и " : "";
                String targetCode = (targetCurrency != null) ? "" : targetCurrencyCode;

                String errorMsg = "Валюта " + baseCode + souz + targetCode
                        + " из валютной пары не существует в БД ";

                request.getSession().setAttribute("errorCode", "SC_NOT_FOUND");
                String jsonError = String.format(
                        "{\"error\": \"HTTP Error 404 Not Found\", \"message\": \"%s\"}",
                        errorMsg
                );

                request.getSession().setAttribute("jsonError", jsonError);
                response.sendError(HttpServletResponse.SC_NOT_FOUND, jsonError);
                return;
            } else {
                // Проверить - Валютная пара с таким кодом уже существует? - 409
                ExchangeRate exchangeRate = exchangeRates.stream()
                        .filter(e -> e.getBaseCurrencyId() == baseCurrency.getId()
                                && e.getTargetCurrencyId() == targetCurrency.getId())
                        .findFirst().orElse(null);
                if (null != exchangeRate) {
                    request.getSession().setAttribute("errorCode", "SC_CONFLICT");
                    String jsonError = String.format(
                            "{\"error\": \"HTTP Error 409 Conflict\", \"message\": \"%s\"}",
                            "Валютная пара с таким кодом уже существует"
                    );

                    request.getSession().setAttribute("jsonError", jsonError);
                    response.sendError(HttpServletResponse.SC_CONFLICT, jsonError);
                    return;
                }
            }


        } catch (SQLException e) {
            request.getSession().setAttribute("errorCode", "SC_INTERNAL_SERVER_ERROR");
            String jsonError = String.format(
                    "{\"error\": \"Internal Server Error\", \"message\": \"%s\"}",
                    "Произошла ошибка при обработке запроса"
            );
            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, jsonError);
        }

        // save and return
        try {
            Optional<Currency> baseCurrency = currenciesRepository.findByCode(baseCurrencyCode);
            Optional<Currency> targetCurrency = currenciesRepository.findByCode(targetCurrencyCode);

            if (baseCurrency.isPresent() && targetCurrency.isPresent()) {
                ExchangeRate newExchangeRate = new ExchangeRate(
                        baseCurrency.get().getId(),
                        targetCurrency.get().getId(),
                        rate);
                exchangeRatesRepository.save(newExchangeRate);

                CurrencyDTO baseCurrencyDTO = converterDTOs.Currency2DTO(baseCurrency.get().getId());
                CurrencyDTO targetCurrencyDTO = converterDTOs.Currency2DTO(targetCurrency.get().getId());
                ExchangeRateDTO exchangeRateDTO = new ExchangeRateDTO(
                        newExchangeRate.getId(),
                        baseCurrencyDTO,
                        targetCurrencyDTO,
                        new BigDecimal(rate));

                PrintWriter printWriter = response.getWriter();
                response.setStatus(HttpServletResponse.SC_OK);
                printWriter.println(mapper.writeValueAsString(exchangeRateDTO));
            }
        } catch (SQLException e) {
            request.getSession().setAttribute("errorCode", "SC_INTERNAL_SERVER_ERROR");
            String jsonError = String.format(
                    "{\"error\": \"Internal Server Error\", \"message\": \"%s\"}",
                    "Произошла ошибка при обработке запроса"
            );
            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, jsonError);
        }
    }
}
