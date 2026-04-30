package currencyexchange.servlets;


import currencyexchange.dto.ExchangeDTO;
import currencyexchange.model.Currency;
import currencyexchange.repository.CurrenciesRepository;
import currencyexchange.repository.CurrenciesRepositoryImpl;
import currencyexchange.repository.ExchangeRatesRepository;
import currencyexchange.repository.ExchangeRatesRepositoryImpl;
import currencyexchange.servise.ExchangeService;
import currencyexchange.servise.ExchangeServiceImpl;
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
import java.util.Map;
import java.util.Optional;

@WebServlet(name = "ExchangeServlet", urlPatterns = "/exchange")
public class ExchangeServlet extends HttpServlet {

    ExchangeRatesRepository exchangeRatesRepository;
    CurrenciesRepository currenciesRepository;
    ExchangeRateUtils exchangeRateUtils;
    ExchangeService exchangeService;
    ConverterDTOs converterDTOs;
    ObjectMapper mapper;

    @Override
    public void init() throws ServletException {
        Connection connection = (Connection) getServletContext().getAttribute("ConnectionToDB");
        exchangeRatesRepository = new ExchangeRatesRepositoryImpl(connection);
        currenciesRepository = new CurrenciesRepositoryImpl(connection);
        exchangeService = new ExchangeServiceImpl(connection);
        exchangeRateUtils = new ExchangeRateUtils();
        converterDTOs = new ConverterDTOs(connection);

        // Create and enable features
        mapper = JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

    }


    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
     /* ******************************************
     Расчёт перевода определённого количества средств из одной валюты в другую
     GET /exchange?from=BASE_CURRENCY_CODE&to=TARGET_CURRENCY_CODE&amount=$AMOUNT
     GET /exchange?from=USD&to=AUD&amount=10
     --------------------------------------------
     Пример ответа:
     {
        "baseCurrency": {
          "id": 0,
          "name": "United States dollar",
          "code": "USD",
          "sign": "$"
        },
        "targetCurrency": {
          "id": 2,
          "name": "Australian dollar",
          "code": "AUD",
          "sign": "A$"
        },
        "rate": 1.45,
        "amount": 10.00,
        "convertedAmount": 14.50
     }
     --------------------------------------------
     Получение курса для обмена может пройти по одному из трёх сценариев:
     Допустим, совершаем перевод из валюты A в валюту B:
     1. В таблице ExchangeRates существует валютная пара AB - берём её курс
     2. В таблице ExchangeRates существует валютная пара BA - берем её курс,
        и считаем обратный, чтобы получить AB
     3. В таблице ExchangeRates существуют валютные пары USD-A и USD-B - вычисляем из этих курсов курс AB
     Остальные возможные сценарии, для упрощения, опустим.
     --------------------------------------------
     Ответ в случае ошибки
     Для всех запросов, в случае ошибки, ответ должен выглядеть так:
     {
       "message": "Валюта не найдена"
     }
     Значение message зависит от того, какая именно ошибка произошла.

     --------------------------------------------
     HTTP коды ответов:
     Успех - 201
     Отсутствует нужное поле формы - 400 +
     Валютная пара отсутствует в базе данных - 404 +
     Ошибка (например, база данных недоступна) - 500 +
    ********************************************/

        if (request.getQueryString().isEmpty()) {
            request.getSession().setAttribute("errorCode", "SC_BAD_REQUEST");
            String jsonError = String.format(
                    "{\"error\": \"HTTP Error 400 Bad Request\", \"message\": \"%s\"}",
                    "Валюта не найдена"
            );
            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, jsonError);
        }

        String baseCurrencyCode = request.getParameter("from").toUpperCase();
        String targetCurrencyCode = request.getParameter("to").toUpperCase();
        String amountParameter = request.getParameter("amount"); // хорошо бы это проверить что цифра есть

        BigDecimal amount = exchangeRateUtils.getRate(amountParameter);
        if (amount.compareTo(new BigDecimal("0")) == -1) {
            request.getSession().setAttribute("errorCode", "SC_BAD_REQUEST");
            String jsonError = String.format(
                    "{\"error\": \"HTTP Error 400 Bad Request\", \"message\": \"%s\"}",
                    "Ошибочное поле формы - rate. Ожидается число"
            );
            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, jsonError);
        }

        Currency baseCurrency;
        Currency targetCurrency;
        int baseCurrencyId = 0;
        int targetCurrencyId = 0;
        try {
            Optional<Currency> desiredBaseCurrency = currenciesRepository.findByCode(baseCurrencyCode);
            Optional<Currency> desiredTargetCurrency = currenciesRepository.findByCode(targetCurrencyCode);

            if (desiredBaseCurrency.isEmpty() || desiredTargetCurrency.isEmpty()) {
                String baseCode = (desiredBaseCurrency.isPresent()) ? "" : baseCurrencyCode;
                String souz = (desiredBaseCurrency.isEmpty() && desiredTargetCurrency.isEmpty()) ? " и " : "";
                String targetCode = (desiredTargetCurrency.isPresent()) ? "" : targetCurrencyCode;

                String errorMsg = "Валюта " + baseCode + souz + targetCode
                        + " из валютной пары не существует в БД ";

                request.getSession().setAttribute("errorCode", "SC_NOT_FOUND");
                String jsonError = String.format(
                        "{\"error\": \"HTTP Error 404 Not Found\", \"message\": \"%s\"}",
                        "Валюта не найдена"
                );
                request.getSession().setAttribute("jsonError", jsonError);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, jsonError);
            } else {
                baseCurrency = desiredBaseCurrency.get();
                baseCurrencyId = baseCurrency.getId();
                targetCurrency = desiredTargetCurrency.get();
                targetCurrencyId = targetCurrency.getId();
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
        Map<String, BigDecimal> resultCalculation = exchangeService.crossRateCalculation(baseCurrencyCode, targetCurrencyCode, amount);
        if (resultCalculation.isEmpty()) {
            request.getSession().setAttribute("errorCode", "SC_NOT_FOUND");
            String jsonError = String.format(
                    "{\"error\": \"HTTP Error 404 Not Found\", \"message\": \"%s\"}",
                    "Валюта не найдена"
            );
            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, jsonError);
        }

        ExchangeDTO exchangeDTO = new ExchangeDTO(
                converterDTOs.Currency2DTO(baseCurrencyId),
                converterDTOs.Currency2DTO(targetCurrencyId),
                resultCalculation.get("rate"),
                amount,
                resultCalculation.get("convertedAmount"));

        PrintWriter printWriter = response.getWriter();
        response.setStatus(HttpServletResponse.SC_OK);
        printWriter.println(mapper.writeValueAsString(exchangeDTO));

    } // end doGet
}
