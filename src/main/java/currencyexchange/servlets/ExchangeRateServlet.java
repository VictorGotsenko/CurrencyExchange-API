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
import java.sql.SQLException;
import java.util.Optional;

@WebServlet(name = "ExchangeRateServlet", urlPatterns = "/exchangeRate/*")
public class ExchangeRateServlet extends HttpServlet {

    ExchangeRatesRepository exchangeRatesRepository;
    CurrenciesRepository currenciesRepository;

    @Override
    public void init() throws ServletException {
        Connection connection = (Connection) getServletContext().getAttribute("ConnectionToDB");
        exchangeRatesRepository = new ExchangeRatesRepositoryImpl(connection);
        currenciesRepository = new CurrenciesRepositoryImpl(connection);

    }


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        /* **************************************
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

        HTTP коды ответов:

        Успех - 200
        Коды валют пары отсутствуют в адресе - 400
        Обменный курс для пары не найден - 404
        Ошибка (например, база данных недоступна) - 500
        ****************************************/


        // 1. Получить всю строку запроса (например, "id=10&name=test")
        String requestURIString = request.getRequestURI();

        // Получить URI без параметров (например, /mycontext/myservlet)
        String uri = request.getRequestURI();
        String uriInfo = request.getPathInfo();

        // Получить полную строку запроса (например, /mycontext/myservlet?id=1)
        String url = request.getRequestURL().toString();
        String queryString = request.getQueryString(); // id=1

//        String codeCurrencyInfo = "";
        String codeCurrencyInfo = request.getPathInfo();
//        codeCurrencyInfo = request.getPathInfo();

        String pathInfo = request.getPathInfo();

        //------------------
        // 3.a Is there the code?
        if (request.getPathInfo() == null
                || request.getPathInfo().equals("/")
                || request.getPathInfo().isEmpty()) {
            //Код валюты отсутствует в адресе - 400
            // 1. Устанавливаем статус 400
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

            // 2. Устанавливаем тип контента JSON
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            // 3. Формируем JSON-ответ
            String jsonError = String.format(
                    "{\"error\": \"HTTP Error 400 Bad Request\", \"message\": \"%s\"}",
                    "Коды валют пары отсутствуют в адресе"
            );

            // 4. Отправляем ответ
            PrintWriter out = response.getWriter();
            out.print(jsonError);
            out.flush();
            return;
        }
        //--------------------

        // 3. Парсить
        String codeCurrencies = request.getPathInfo().replace("/", "").replace(" ", "").toUpperCase();
        String baseCurrencyCode = codeCurrencies.substring(0, 3);
        String targetCurrencyCode = codeCurrencies.substring(3);

        // Искать
        // найти id base
        // найти id target
        Optional<Currency> baseCurrency = null;
        try {
            baseCurrency = currenciesRepository.findByCode(baseCurrencyCode);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        Optional<Currency> targetCurrency = null;
        try {
            targetCurrency = currenciesRepository.findByCode(targetCurrencyCode);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        int baseCurrencyId = -1;
        int targetCurrencyId = -1;
        if (baseCurrency.isPresent()) {
            baseCurrencyId = baseCurrency.get().getId();
        }

        if (targetCurrency.isPresent()) {
            targetCurrencyId = targetCurrency.get().getId();
        }

        // запросить пару: 1)пара есть, вернуть пару 2) пары нет, вернуть 404
        Optional<ExchangeRate> result = null;
        try {
            result = exchangeRatesRepository.findById(baseCurrencyId, targetCurrencyId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (result.isEmpty()) {
            //err 404
            // 1. Устанавливаем статус 404
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);

            // 2. Устанавливаем тип контента JSON
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            // 3. Формируем JSON-ответ
            String jsonError = String.format(
                    "{\"error\": \"Not Found\", \"message\": \"%s\"}",
                    "Обменный курс для пары не найден"
            );

            // 4. Отправляем ответ
            PrintWriter out = response.getWriter();
            out.print(jsonError);
            out.flush();
            return;
        } else {
            ExchangeRateDTO exchangeRateDTO;
            ExchangeRate exchangeRate = result.get();


            int id = exchangeRate.getId();
            baseCurrencyId = exchangeRate.getBaseCurrencyCode();
            targetCurrencyId = exchangeRate.getTargetCurrencyCode();
            double rate = exchangeRate.getRate();

            try {
                baseCurrency = currenciesRepository.findById(baseCurrencyId);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            try {
                targetCurrency = currenciesRepository.findById(targetCurrencyId);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            if (baseCurrency.isPresent() && targetCurrency.isPresent()) {
                Currency baseCurr = baseCurrency.get();
                CurrencyDTO baseCurrencyDTO = new CurrencyDTO(
                        baseCurr.getId(),
                        baseCurr.getName(),
                        baseCurr.getCode(),
                        baseCurr.getSign());

                Currency targetCurr = targetCurrency.get();
                CurrencyDTO targetCurrencyDTO = new CurrencyDTO(
                        targetCurr.getId(),
                        targetCurr.getName(),
                        targetCurr.getCode(),
                        targetCurr.getSign());

                exchangeRateDTO = new ExchangeRateDTO(id, baseCurrencyDTO, targetCurrencyDTO, rate);
                PrintWriter printWriter = response.getWriter();

                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.setHeader("Content-Security-Policy", "default-src 'self';"); //CSP (Content Security Policy)

                ObjectMapper mapper = new ObjectMapper();
                printWriter.println(mapper.writeValueAsString(exchangeRateDTO));
            } else {
                //  Ошибка (например, база данных недоступна) - 500
                // 1. Устанавливаю статус 500
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                // 2. Устанавливаю тип контента JSON
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.setHeader("Content-Security-Policy", "default-src 'self';"); //CSP (Content Security Policy)

                // 3. Формирую JSON-ответ
                String jsonError = String.format(
                        "{\"error\": \"Internal Server Error\", \"message\": \"%s\"}",
                        "Произошла ошибка при обработке запроса"
                );

                // 4. Отправляю ответ
                PrintWriter out = response.getWriter();
                out.print(jsonError);
                out.flush();
            }
        }
    }
}
