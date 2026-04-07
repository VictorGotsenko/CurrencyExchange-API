package currencyexchange.servlets;


import com.zaxxer.hikari.HikariDataSource;
import currencyexchange.dto.CurrencyDTO;
import currencyexchange.model.Currency;
import currencyexchange.repository.CurrenciesRepository;
import currencyexchange.repository.CurrenciesRepositoryImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@WebServlet(name = "CurrencyServlet", urlPatterns = "/currency/*")
public class CurrencyServlet extends HttpServlet {
    // http://localhost:8080/currency/EUR

    CurrenciesRepository currenciesRepository;

    @Override
    public void init() throws ServletException {
        // Retrieve initialization parameters defined in web.xml or annotations
        HikariDataSource hikariDataSource = (HikariDataSource) getServletContext().getAttribute("hikariDataSource");
        currenciesRepository = new CurrenciesRepositoryImpl(hikariDataSource);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        /* **************************************
        GET /currency/EUR
        -----------------------------------------
        {
          "id": 0,
          "name": "Euro",
          "code": "EUR",
          "sign": "€"
        }
        -----------------------------------------
        HTTP коды ответов:
          Успех - 200
          Код валюты отсутствует в адресе - 400
          Валюта не найдена - 404
          Ошибка (например, база данных недоступна) - 500
         ***************************************/

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
                    "Код валюты отсутствует в адресе"
            );

            // 4. Отправляем ответ
            PrintWriter out = response.getWriter();
            out.print(jsonError);
            out.flush();
            return;
        }

        // 3. Парсить Искать
        String codeCurrency = request.getPathInfo().replace("/", "").replace(" ", "").toUpperCase();


        // получить код валюты проверить вхождение
        try {
            List<Currency> currencies = currenciesRepository.getCurrencies();

            List<String> codes = currencies.stream()
                    .map(Currency::getCode)
                    .collect(Collectors.toList());
            if (!codes.contains(codeCurrency)) {
                // Валюта не найдена - 404
                // 1. Устанавливаем статус 404
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);

                // 2. Устанавливаем тип контента JSON
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.setHeader("Content-Security-Policy", "default-src 'self';"); //CSP (Content Security Policy)

                // 3. Формируем JSON-ответ
                String jsonError = String.format(
                        "{\"error\": \"HTTP Error 404 Not Found\", \"message\": \"%s\"}",
                        "Код валюты не найден"
                );

                // 4. Отправляем ответ
                PrintWriter out = response.getWriter();
                out.print(jsonError);
                out.flush();
                return;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        //4. Вернуть
        Optional<Currency> newCurrency;
        try {
            newCurrency = currenciesRepository.findByCode(codeCurrency);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (newCurrency.isPresent()) {
            Currency result = newCurrency.get();
            CurrencyDTO currencyDTO = new CurrencyDTO(
                    result.getId(),
                    result.getName(),
                    result.getCode(),
                    result.getSign());

            PrintWriter printWriter = response.getWriter();

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Security-Policy", "default-src 'self';"); //CSP (Content Security Policy)

            ObjectMapper mapper = new ObjectMapper();
            printWriter.println(mapper.writeValueAsString(currencyDTO));
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
