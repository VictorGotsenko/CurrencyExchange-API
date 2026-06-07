package currencyexchange.servlets;

import com.zaxxer.hikari.HikariDataSource;
import currencyexchange.dto.CurrencyDto;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@WebServlet(name = "CurrenciesServlet", urlPatterns = "/currencies")
public final class CurrenciesServlet extends HttpServlet {
    // http://localhost:8080/currencies

    CurrenciesRepository currenciesRepository;
    ObjectMapper mapper;

    @Override
    public void init() throws ServletException {
        HikariDataSource dataSource = (HikariDataSource) getServletContext().getAttribute("dataSource");
        currenciesRepository = new CurrenciesRepositoryImpl(dataSource);

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
            request.getSession().setAttribute("errorCode", "SC_INTERNAL_SERVER_ERROR");
            String jsonError = String.format(
                    "{\"error\": \"Internal Server Error\", \"message\": \"%s\"}",
                    "Произошла ошибка при обработке запроса"
            );
            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, jsonError);
        }
        List<CurrencyDto> listCurrencyDTO = new ArrayList<>();
        for (Currency currency : listCurrencies) {
            int id = currency.getId();
            String name = currency.getName();
            String code = currency.getCode();
            String sign = currency.getSign();
            listCurrencyDTO.add(new CurrencyDto(id, name, code, sign));
        }
        PrintWriter printWriter = response.getWriter();

        response.setStatus(HttpServletResponse.SC_OK);
        printWriter.println(mapper.writeValueAsString(listCurrencyDTO));
    }

    @SuppressWarnings("checkstyle:methodlength")
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

        String name = request.getParameter("name");
        String code = request.getParameter("code");
        String sign = request.getParameter("sign");

        if (name == null || name.isEmpty() || name.isBlank()) {
            request.getSession().setAttribute("errorCode", "SC_BAD_REQUEST");
            String jsonError = String.format(
                    "{\"error\": \"HTTP Error 400 Bad Request\", \"message\": \"%s\"}",
                    "Отсутствует нужное поле формы"
            );

            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, jsonError);
            return;
        }

        if (code == null || code.isEmpty() || code.isBlank()) {
            request.getSession().setAttribute("errorCode", "SC_BAD_REQUEST");
            String jsonError = String.format(
                    "{\"error\": \"HTTP Error 400 Bad Request\", \"message\": \"%s\"}",
                    "Отсутствует нужное поле формы"
            );

            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, jsonError);
            return;
        }

        if (sign == null || sign.isEmpty() || sign.isBlank()) {
            request.getSession().setAttribute("errorCode", "SC_BAD_REQUEST");
            String jsonError = String.format(
                    "{\"error\": \"HTTP Error 400 Bad Request\", \"message\": \"%s\"}",
                    "Отсутствует нужное поле формы"
            );

            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, jsonError);
            return;
        }

        if (sign != null && (sign.length() > 2)) {
            request.getSession().setAttribute("errorCode", "SC_BAD_REQUEST");
            String jsonError = String.format(
                    "{\"error\": \"HTTP Error 400 Bad Request\", \"message\": \"%s\"}",
                    "Отсутствует нужное поле формы"
            );

            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, jsonError);
            return;
        }

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

        Currency currency = new Currency(name, code, sign);
        currenciesRepository.save(currency);

        Optional<Currency> newCurrency;
        newCurrency = currenciesRepository.findByCode(code);
        if (newCurrency.isEmpty()) {
            request.getSession().setAttribute("errorCode", "SC_INTERNAL_SERVER_ERROR");
            String jsonError = String.format(
                    "{\"error\": \"Internal Server Error\", \"message\": \"%s\"}",
                    "Произошла ошибка при обработке запроса"
            );

            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, jsonError);
            return;
        }
        Currency result = newCurrency.get();
        CurrencyDto currencyDTO = new CurrencyDto(
                result.getId(),
                result.getName(),
                result.getCode(),
                result.getSign());

        response.setStatus(HttpServletResponse.SC_CREATED);
        PrintWriter printWriter = response.getWriter();
        printWriter.println(mapper.writeValueAsString(currencyDTO));
    }
}
