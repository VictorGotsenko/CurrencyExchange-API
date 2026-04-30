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
import currencyexchange.util.ExchangeRateUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@WebServlet(name = "ExchangeRateServlet", urlPatterns = "/exchangeRate/*")
public class ExchangeRateServlet extends HttpServlet {

    ExchangeRatesRepository exchangeRatesRepository;
    CurrenciesRepository currenciesRepository;
    ConverterDTOs converterDTOs;
    ExchangeRateUtils exchangeRateUtils;
    ObjectMapper mapper;

    @Override
    public void init() throws ServletException {
        Connection connection = (Connection) getServletContext().getAttribute("ConnectionToDB");
        exchangeRatesRepository = new ExchangeRatesRepositoryImpl(connection);
        currenciesRepository = new CurrenciesRepositoryImpl(connection);
        converterDTOs = new ConverterDTOs(connection);
        exchangeRateUtils = new ExchangeRateUtils();

        // Create and enable features
        mapper = JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        /* **************************************
        Получение конкретного обменного курса
        GET /exchangeRate/USDRUB
        "id": 0,
           "baseCurrency": {
             "id": 0,
             "name": "United States dollar",
             "code": "USD",
             "sign": "$"
           },
          "targetCurrency": {
             "id": 2,
             "name": "Russian Ruble",
             "code": "RUB",
             "sign": "₽"
           },
        "rate": 80
        }
       ------------------------------------------
        HTTP коды ответов:

        Успех - 200
        Коды валют пары отсутствуют в адресе - 400
        Обменный курс для пары не найден - 404
        Ошибка (например, база данных недоступна) - 500
        *****************************************/
        if (request.getPathInfo() == null
                || request.getPathInfo().equals("/")
                || request.getPathInfo().isEmpty()) {
            //Код валюты отсутствует в адресе - 400
            request.getSession().setAttribute("errorCode", "SC_BAD_REQUEST");
            String jsonError = String.format(
                    "{\"error\": \"HTTP Error 400 Bad Request\", \"message\": \"%s\"}",
                    "Коды валют пары отсутствуют в адресе"
            );

            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, jsonError);
        }

        String codeCurrencies = request.getPathInfo().replace("/", "").replace(" ", "").toUpperCase();
        String baseCurrencyCode = codeCurrencies.substring(0, 3);
        String targetCurrencyCode = codeCurrencies.substring(3);

        Currency baseCurrency;
        Currency targetCurrency;
        Optional<Currency> desiredCurrency = null;

        try {
            desiredCurrency = currenciesRepository.findByCode(baseCurrencyCode);
            if (desiredCurrency.isEmpty()) {
                request.getSession().setAttribute("errorCode", "SC_INTERNAL_SERVER_ERROR");
                String jsonError = String.format(
                        "{\"error\": \"Internal Server Error\", \"message\": \"%s\"}",
                        "Произошла ошибка при обработке запроса. Код " + baseCurrencyCode + " не найден"
                );
                request.getSession().setAttribute("jsonError", jsonError);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, jsonError);
            }
            baseCurrency = desiredCurrency.get();

            desiredCurrency = currenciesRepository.findByCode(targetCurrencyCode);
            if (desiredCurrency.isEmpty()) {
                request.getSession().setAttribute("errorCode", "SC_INTERNAL_SERVER_ERROR");
                String jsonError = String.format(
                        "{\"error\": \"Internal Server Error\", \"message\": \"%s\"}",
                        "Произошла ошибка при обработке запроса. Код " + targetCurrencyCode + " не найден"
                );
                request.getSession().setAttribute("jsonError", jsonError);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, jsonError);
            }
            targetCurrency = desiredCurrency.get();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        Optional<ExchangeRate> exchangeRate;
        try {
            exchangeRate = exchangeRatesRepository.findByCurrencyIDs(baseCurrency.getId(), targetCurrency.getId());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (exchangeRate.isEmpty()) {
            request.getSession().setAttribute("errorCode", "SC_NOT_FOUND");
            String jsonError = String.format(
                    "{\"error\": \"Not Found\", \"message\": \"%s\"}",
                    "Обменный курс для пары не найден"
            );
            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, jsonError);
        }

        ExchangeRateDTO exchangeRateDTO;
        ExchangeRate exchangeRateResult = exchangeRate.get();

        int id = exchangeRateResult.getId();
        BigDecimal rate = exchangeRateResult.getRate();

        CurrencyDTO baseCurrencyDTO = new CurrencyDTO(
                baseCurrency.getId(),
                baseCurrency.getName(),
                baseCurrency.getCode(),
                baseCurrency.getSign());

        CurrencyDTO targetCurrencyDTO = new CurrencyDTO(
                targetCurrency.getId(),
                targetCurrency.getName(),
                targetCurrency.getCode(),
                targetCurrency.getSign());

        exchangeRateDTO = new ExchangeRateDTO(id, baseCurrencyDTO, targetCurrencyDTO, rate);
        PrintWriter printWriter = response.getWriter();

        response.setStatus(HttpServletResponse.SC_OK);
        printWriter.println(mapper.writeValueAsString(exchangeRateDTO));

    }

    @Override
    protected void doPatch(HttpServletRequest request, HttpServletResponse response) throws IOException {
    /* ******************************************
     Обновление существующего в базе обменного курса
     PATCH /exchangeRate/USDRUB
     --------------------------------------------
     Данные передаются в теле запроса в виде полей формы (x-www-form-urlencoded).
     Единственное поле формы - rate
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
          "id": 2,
          "name": "Russian Ruble",
          "code": "RUB",
          "sign": "₽"
        },
        "rate": 85
     }
     --------------------------------------------
     HTTP коды ответов:
     Успех - 201
     Отсутствует нужное поле формы - 400 +
     Валютная пара отсутствует в базе данных - 404 +
     Ошибка (например, база данных недоступна) - 500 +
    ********************************************/

        Currency baseCurrency;
        Currency targetCurrency;

        if (request.getPathInfo() == null
                || request.getPathInfo().equals("/")
                || request.getPathInfo().isEmpty()) {
            request.getSession().setAttribute("errorCode", "SC_BAD_REQUEST");
            String jsonError = String.format(
                    "{\"error\": \"HTTP Error 400 Bad Request\", \"message\": \"%s\"}",
                    "Коды валют пары отсутствуют в адресе"
            );
            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, jsonError);
        }

        // parse
        String codeCurrencies = request.getPathInfo().replace("/", "").replace(" ", "").toUpperCase();
        String baseCurrencyCode = codeCurrencies.substring(0, 3);
        String targetCurrencyCode = codeCurrencies.substring(3);

        String requestBody = request.getReader().lines()
                .collect(Collectors.joining(System.lineSeparator()));

        BigDecimal rate = exchangeRateUtils.getRate(requestBody);
        if (rate.compareTo(new BigDecimal("0")) == -1) {
            request.getSession().setAttribute("errorCode", "SC_BAD_REQUEST");
            String jsonError = String.format(
                    "{\"error\": \"HTTP Error 400 Bad Request\", \"message\": \"%s\"}",
                    "Ошибочное поле формы - rate. Ожидается число"
            );
            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, jsonError);
        }

        int baseCurrencyId = 0;
        int targetCurrencyId = 0;
        int exchangeRateId = 0;

        try {
            Optional<Currency> desiredBaseCurrency = currenciesRepository.findByCode(baseCurrencyCode);
            Optional<Currency> desiredTargetCurrency = currenciesRepository.findByCode(targetCurrencyCode);
            if (desiredBaseCurrency.isEmpty() || desiredTargetCurrency.isEmpty()) {
                // Какая-то валюта не найдена
                request.getSession().setAttribute("errorCode", "SC_INTERNAL_SERVER_ERROR");
                String jsonError = null;
                if (desiredBaseCurrency.isEmpty()) {
                    jsonError = String.format(
                            "{\"error\": \"Internal Server Error\", \"message\": \"%s\"}",
                            "Произошла ошибка при обработке запроса. Код " + baseCurrencyCode + " не найден"
                    );
                } else {
                    jsonError = String.format(
                            "{\"error\": \"Internal Server Error\", \"message\": \"%s\"}",
                            "Произошла ошибка при обработке запроса. Код " + targetCurrencyCode + " не найден"
                    );
                }
                request.getSession().setAttribute("jsonError", jsonError);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, jsonError);
            }
            baseCurrency = desiredBaseCurrency.get();
            targetCurrency = desiredTargetCurrency.get();

            List<ExchangeRate> exchangeRates = exchangeRatesRepository.getExchangeRates();

            if (baseCurrency != null) {
                baseCurrencyId = baseCurrency.getId();
            }
            if (targetCurrency != null) {
                targetCurrencyId = targetCurrency.getId();
            }

            ExchangeRate exchangeRate = exchangeRates.stream()
                    .filter(e -> e.getBaseCurrencyId() == baseCurrency.getId()
                            && e.getTargetCurrencyId() == targetCurrency.getId())
                    .findFirst().orElse(null);

            if (exchangeRate != null) {
                exchangeRateId = exchangeRate.getId();
            } else {
                request.getSession().setAttribute("errorCode", "SC_NOT_FOUND");
                String jsonError = String.format(
                        "{\"error\": \"HTTP Error 404 Not Found\", \"message\": \"%s\"}",
                        "Валютная пара отсутствует в базе данных"
                );
                request.getSession().setAttribute("jsonError", jsonError);
                response.sendError(HttpServletResponse.SC_NOT_FOUND, jsonError);
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

        try {
            exchangeRatesRepository.update(exchangeRateId, rate);

            Optional<ExchangeRate> exchangeRate = exchangeRatesRepository.findById(exchangeRateId);

            if (exchangeRate.isPresent()) {
                ExchangeRateDTO exchangeRateDTO = new ExchangeRateDTO(
                        exchangeRate.get().getId(),
                        converterDTOs.Currency2DTO(baseCurrencyId),
                        converterDTOs.Currency2DTO(targetCurrencyId),
                        exchangeRate.get().getRate());

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
