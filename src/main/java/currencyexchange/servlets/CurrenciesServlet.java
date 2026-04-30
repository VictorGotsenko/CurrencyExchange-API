package currencyexchange.servlets;

import currencyexchange.dto.CurrencyDTO;
import currencyexchange.model.Currency;
import currencyexchange.repository.CurrenciesRepository;
import currencyexchange.repository.CurrenciesRepositoryImpl;
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
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@WebServlet(name = "CurrenciesServlet", urlPatterns = "/currencies")
public class CurrenciesServlet extends HttpServlet {
    // http://localhost:8080/currencies

    CurrenciesRepository currenciesRepository;
    ObjectMapper mapper;

    @Override
    public void init() throws ServletException {
        Connection connection = (Connection) getServletContext().getAttribute("ConnectionToDB");
        currenciesRepository = new CurrenciesRepositoryImpl(connection);
        // Create and enable features
        mapper = JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    @SneakyThrows
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    /* ******************************************
     GET /currencies
    ---------------------------------------------
    Получение списка валют. Пример ответа:
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
     --------------------------------------------
     HTTP коды ответов:
     Успех - 200
     Ошибка (например, база данных недоступна) - 500
    ****************************************** */


        List<Currency> listCurrencies = currenciesRepository.getCurrencies();

        if (listCurrencies.isEmpty()) {
            // Устанавливаю статус 500
            request.getSession().setAttribute("errorCode", "SC_INTERNAL_SERVER_ERROR");

            // Формирую JSON-ответ
            String jsonError = String.format(
                    "{\"error\": \"Internal Server Error\", \"message\": \"%s\"}",
                    "Произошла ошибка при обработке запроса"
            );

            // Отправляю ответ
            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, jsonError);
        }
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
        printWriter.println(mapper.writeValueAsString(listCurrencyDTO));
    }

    @Override
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

        // Получение данных по имени поля формы
        String name = request.getParameter("name");
        String code = request.getParameter("code");
        String sign = request.getParameter("sign");

        if (name == null || code == null || sign == null) {
            request.getSession().setAttribute("errorCode", "SC_BAD_REQUEST");
            String jsonError = String.format(
                    "{\"error\": \"HTTP Error 400 Bad Request\", \"message\": \"%s\"}",
                    "Отсутствует нужное поле формы"
            );

            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, jsonError);
        }

        //  Валюта с таким кодом уже существует - 409
        try {
            List<Currency> currencies = currenciesRepository.getCurrencies();

            List<String> codes = currencies.stream()
                    .map(Currency::getCode)
                    .collect(Collectors.toList());
            if (codes.contains(code)) {
                request.getSession().setAttribute("errorCode", "SC_CONFLICT");
                String jsonError = String.format(
                        "{\"error\": \"HTTP Error 409 Conflict\", \"message\": \"%s\"}",
                        "Валюта с таким кодом уже существует"
                );

                request.getSession().setAttribute("jsonError", jsonError);
                response.sendError(HttpServletResponse.SC_CONFLICT, jsonError);
                return;
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

        // Paste currency
        Currency currency = new Currency(name, code, sign);
        try {
            currenciesRepository.save(currency);
        } catch (SQLException e) {
            request.getSession().setAttribute("errorCode", "SC_INTERNAL_SERVER_ERROR");
            String jsonError = String.format(
                    "{\"error\": \"Internal Server Error\", \"message\": \"%s\"}",
                    "Произошла ошибка при обработке запроса"
            );

            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, jsonError);
        }

        //4. Вернуть
        Optional<Currency> newCurrency;

        try {
            newCurrency = currenciesRepository.findByCode(code);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (newCurrency.isEmpty()) {
            request.getSession().setAttribute("errorCode", "SC_INTERNAL_SERVER_ERROR");
            String jsonError = String.format(
                    "{\"error\": \"Internal Server Error\", \"message\": \"%s\"}",
                    "Произошла ошибка при обработке запроса"
            );

            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, jsonError);
        }
        Currency result = newCurrency.get();
        CurrencyDTO currencyDTO = new CurrencyDTO(
                result.getId(),
                result.getName(),
                result.getCode(),
                result.getSign());

        response.setStatus(HttpServletResponse.SC_OK);
        PrintWriter printWriter = response.getWriter();
        printWriter.println(mapper.writeValueAsString(currencyDTO));
    }

}
