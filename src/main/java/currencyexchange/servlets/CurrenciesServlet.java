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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@WebServlet(name = "CurrenciesServlet", urlPatterns = "/currencies")
public class CurrenciesServlet extends HttpServlet {

    // http://localhost:8080/currencies

    CurrenciesRepository currenciesRepository;

    @Override
    public void init() throws ServletException {
        // Retrieve initialization parameters defined in web.xml or annotations
//        currenciesRepository = (CurrenciesRepository) getServletContext().getAttribute("currenciesRepository");
//        currenciesRepository = (CurrenciesRepository) getServletContext().getAttribute("currenciesRepository");
        // hikariDataSource
        HikariDataSource hikariDataSource = (HikariDataSource) getServletContext().getAttribute("hikariDataSource");
        currenciesRepository = new CurrenciesRepositoryImpl(hikariDataSource);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    /* *****************************************
     GET /currencies

     [
      {
        "id": 0,
        "name": "United States dollar",
        "code": "USD",
        "sign": "$"
      },
      {
        "id": 0,
        "name": "Euro",
        "code": "EUR",
        "sign": "€"
      }
     ]

     HTTP коды ответов:
     Успех - 200
     Ошибка (например, база данных недоступна) - 500
     ********************************************** */

        List<Currency> listCurrencies;
        try {
            listCurrencies = currenciesRepository.getCurrencies();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (listCurrencies.isEmpty()) {
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

            List<CurrencyDTO> listCurrencyDTO = new ArrayList<>();
            for (Currency currency : listCurrencies) {
                int id = currency.getId();
                String name = currency.getName();
                String code = currency.getCode();
                String sign = currency.getSign();
                listCurrencyDTO.add(new CurrencyDTO(id, name, code, sign));
            }

            PrintWriter printWriter = response.getWriter();

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Security-Policy", "default-src 'self';"); //CSP (Content Security Policy)

            ObjectMapper mapper = new ObjectMapper();
            printWriter.println(mapper.writeValueAsString(listCurrencyDTO));
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

    /* ******************************************
     POST /currencies
     --------------------------------------------
     Данные передаются в теле запроса в виде полей формы (x-www-form-urlencoded).
     Поля формы - name, code, sign.
     Пример ответа - JSON представление вставленной в базу записи, включая её ID:
     {
        "id": 0,
        "name": "Euro",
        "code": "EUR",
        "sign": "€"
     }
     --------------------------------------------
     HTTP коды ответов:
     Успех - 201
     Отсутствует нужное поле формы - 400 +
     Валюта с таким кодом уже существует - 409 +
     Ошибка (например, база данных недоступна) - 500
    ********************************************/

        // Установка кодировки для корректного чтения кириллицы
        request.setCharacterEncoding("UTF-8");

        // Получение данных по имени поля формы
        String name = request.getParameter("name");
        String code = request.getParameter("code");
        String sign = request.getParameter("sign");

        //  Отсутствует нужное поле формы - 400
        if (name == null || code == null || sign == null) {
            // 1. Устанавливаем статус 400
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

            // 2. Устанавливаем тип контента JSON
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            // 3. Формируем JSON-ответ
            String jsonError = String.format(
                    "{\"error\": \"HTTP Error 409 Conflict\", \"message\": \"%s\"}",
                    "Отсутствует нужное поле формы"
            );

            // 4. Отправляем ответ
            PrintWriter out = response.getWriter();
            out.print(jsonError);
            out.flush();
            return;
        }

        //  Валюта с таким кодом уже существует - 409
        try {
            List<Currency> currencies = currenciesRepository.getCurrencies();

            List<String> codes = currencies.stream()
                    .map(Currency::getCode)
                    .collect(Collectors.toList());
            if (codes.contains(code)) {
                //  Валюта с таким кодом уже существует - 409
                // 1. Устанавливаем статус 409
                response.setStatus(HttpServletResponse.SC_CONFLICT);

                // 2. Устанавливаем тип контента JSON
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                // 3. Формируем JSON-ответ
                String jsonError = String.format(
                        "{\"error\": \"HTTP Error 409 Conflict\", \"message\": \"%s\"}",
                        "Валюта с таким кодом уже существует"
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

        // Paste currency
        Currency currency = new Currency(name, code, sign);
        try {
            currenciesRepository.save(currency);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        //4. Вернуть
        Optional<Currency> newCurrency;
        try {
            newCurrency = currenciesRepository.findByCode(code);
//            newCurrency = currenciesRepository.findByCode("");
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
            // 1. Устанавливаем статус 500
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            // 2. Устанавливаем тип контента JSON
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Security-Policy", "default-src 'self';"); //CSP (Content Security Policy)

            // 3. Формируем JSON-ответ
            String jsonError = String.format(
                    "{\"error\": \"Internal Server Error\", \"message\": \"%s\"}",
                    "Произошла ошибка при обработке запроса"
            );

            // 4. Отправляем ответ
            PrintWriter out = response.getWriter();
            out.print(jsonError);
            out.flush();
        }
    }


}
