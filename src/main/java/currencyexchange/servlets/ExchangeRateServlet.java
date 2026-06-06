package currencyexchange.servlets;

import currencyexchange.dto.CurrencyDto;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@WebServlet(name = "ExchangeRateServlet", urlPatterns = "/exchangeRate/*")
public final class ExchangeRateServlet extends HttpServlet {

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
            request.getSession().setAttribute("errorCode", "SC_BAD_REQUEST");
            String jsonError = String.format(
                    "{\"error\": \"HTTP Error 400 Bad Request\", \"message\": \"%s\"}",
                    "Коды валют пары отсутствуют в адресе"
            );

            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, jsonError);
            return;
        }

        String codeCurrencies = request.getPathInfo().replace("/", "").replace(" ", "").toUpperCase();
        String baseCurrencyCode = codeCurrencies.substring(0, 3);
        String targetCurrencyCode = codeCurrencies.substring(3);

        Currency baseCurrency;
        Currency targetCurrency;
        Optional<Currency> desiredCurrency;

        desiredCurrency = currenciesRepository.findByCode(baseCurrencyCode);
        if (desiredCurrency.isEmpty()) {
            request.getSession().setAttribute("errorCode", "SC_NOT_FOUND");
            String jsonError = String.format(
                    "{\"error\": \"Not Found\", \"message\": \"%s\"}",
                    "Обменный курс для пары не найден"
            );
            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, jsonError);
            return;
        } else {
            baseCurrency = desiredCurrency.get();
        }

        desiredCurrency = currenciesRepository.findByCode(targetCurrencyCode);
        if (desiredCurrency.isEmpty()) {
            request.getSession().setAttribute("errorCode", "SC_NOT_FOUND");
            String jsonError = String.format(
                    "{\"error\": \"Not Found\", \"message\": \"%s\"}",
                    "Обменный курс для пары не найден"
            );
            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, jsonError);
            return;
        } else {
            targetCurrency = desiredCurrency.get();
        }

        Optional<ExchangeRate> exchangeRate;
        exchangeRate = exchangeRatesRepository.findByCurrencyIDs(baseCurrency.getId(), targetCurrency.getId());

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

        CurrencyDto baseCurrencyDTO = new CurrencyDto(
                baseCurrency.getId(),
                baseCurrency.getName(),
                baseCurrency.getCode(),
                baseCurrency.getSign());

        CurrencyDto targetCurrencyDTO = new CurrencyDto(
                targetCurrency.getId(),
                targetCurrency.getName(),
                targetCurrency.getCode(),
                targetCurrency.getSign());

        exchangeRateDTO = new ExchangeRateDTO(id, baseCurrencyDTO, targetCurrencyDTO, rate);
        PrintWriter printWriter = response.getWriter();

        response.setStatus(HttpServletResponse.SC_OK);
        printWriter.println(mapper.writeValueAsString(exchangeRateDTO));

    }

    @SuppressWarnings("checkstyle:methodlength")
    @Override
    protected void doPatch(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
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
                || request.getPathInfo().isEmpty()
                || request.getPathInfo().isBlank()) {
            request.getSession().setAttribute("errorCode", "SC_BAD_REQUEST");
            String jsonError = String.format(
                    "{\"error\": \"HTTP Error 400 Bad Request\", \"message\": \"%s\"}",
                    "Коды валют пары отсутствуют в адресе"
            );
            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, jsonError);
            return;
        }

        String codeCurrencies = request.getPathInfo().replace("/", "").replace(" ", "").toUpperCase();
        String baseCurrencyCode = codeCurrencies.substring(0, 3);
        String targetCurrencyCode = codeCurrencies.substring(3);

        String requestBody = request.getReader().lines()
                .collect(Collectors.joining(System.lineSeparator()));
        if (requestBody.isEmpty() || requestBody.isBlank()) {
            request.getSession().setAttribute("errorCode", "SC_BAD_REQUEST");
            String jsonError = String.format(
                    "{\"error\": \"HTTP Error 400 Bad Request\", \"message\": \"%s\"}",
                    "Отсутствует поле формы - rate. Ожидается число с десятичным разделителем - точкой"
            );
            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, jsonError);
            return;
        }

        BigDecimal rate;
        try {
            String rateValue = requestBody.substring(5).trim();
            rate = new BigDecimal(rateValue.replaceAll(",", ""));
        } catch (NumberFormatException e) {
            request.getSession().setAttribute("errorCode", "SC_BAD_REQUEST");
            String jsonError = String.format(
                    "{\"error\": \"HTTP Error 400 Bad Request\", \"message\": \"%s\"}",
                    "Ошибочное поле формы - rate. Ожидается число с десятичным разделителем - точкой"
            );
            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, jsonError);
            return;
        }

        Optional<Currency> desiredBaseCurrency = currenciesRepository.findByCode(baseCurrencyCode);
        Optional<Currency> desiredTargetCurrency = currenciesRepository.findByCode(targetCurrencyCode);
        if (desiredBaseCurrency.isEmpty() || desiredTargetCurrency.isEmpty()) {
            request.getSession().setAttribute("errorCode", "SC_NOT_FOUND");
            String jsonError = null;
            if (desiredBaseCurrency.isEmpty()) {
                jsonError = String.format(
                        "{\"error\": \"HTTP Error 404 Not Found\", \"message\": \"%s\"}",
                        "Валютная пара отсутствует в базе данных. Код " + baseCurrencyCode + " не найден"
                );
            } else {
                jsonError = String.format(
                        "{\"error\": \"HTTP Error 404 Not Found\", \"message\": \"%s\"}",
                        "Валютная пара отсутствует в базе данных. Код " + targetCurrencyCode + " не найден"
                );
            }
            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, jsonError);
            return;
        }

        baseCurrency = desiredBaseCurrency.get();
        int baseCurrencyId = baseCurrency.getId();
        targetCurrency = desiredTargetCurrency.get();
        int targetCurrencyId = targetCurrency.getId();
        List<ExchangeRate> exchangeRates = exchangeRatesRepository.getExchangeRates();

        ExchangeRate exchangeRate = exchangeRates.stream()
                .filter(e -> e.getBaseCurrencyId() == baseCurrency.getId()
                        && e.getTargetCurrencyId() == targetCurrency.getId())
                .findFirst().orElse(null);

        if (null == exchangeRate) {
            request.getSession().setAttribute("errorCode", "SC_NOT_FOUND");
            String jsonError = String.format(
                    "{\"error\": \"HTTP Error 404 Not Found\", \"message\": \"%s\"}",
                    "Валютная пара отсутствует в базе данных"
            );
            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, jsonError);
            return;
        }
        int exchangeRateId = exchangeRate.getId();

        exchangeRatesRepository.update(exchangeRateId, rate);

        Optional<ExchangeRate> exchangeRateUpdated = exchangeRatesRepository.findById(exchangeRateId);

        if (exchangeRateUpdated.isPresent()) {
            ExchangeRateDTO exchangeRateDTO = new ExchangeRateDTO(
                    exchangeRateUpdated.get().getId(),
                    converterDTOs.currencyToDTO(baseCurrencyId),
                    converterDTOs.currencyToDTO(targetCurrencyId),
                    exchangeRateUpdated.get().getRate());

            PrintWriter printWriter = response.getWriter();
            response.setStatus(HttpServletResponse.SC_OK);
            printWriter.println(mapper.writeValueAsString(exchangeRateDTO));
        }
    }
}
