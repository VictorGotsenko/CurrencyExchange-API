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
import java.util.List;

@WebServlet(name = "CurrencyServlet", urlPatterns = "/currency/*")
public class CurrencyServlet extends HttpServlet {
    // http://localhost:8080/currency/EUR

    CurrenciesRepository currenciesRepository;
    ObjectMapper mapper;

    @Override
    public void init() throws ServletException {
        // Retrieve initialization parameters defined in web.xml or annotations
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

        if (request.getPathInfo() == null
                || request.getPathInfo().equals("/")
                || request.getPathInfo().isEmpty()) {
            //Код валюты отсутствует в адресе - 400
            request.getSession().setAttribute("errorCode", "SC_BAD_REQUEST");
            String jsonError = String.format(
                    "{\"error\": \"HTTP Error 400 Bad Request\", \"message\": \"%s\"}",
                    "Код валюты отсутствует в адресе"
            );
            request.getSession().setAttribute("jsonError", jsonError);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, jsonError);
            return;
        }

        String codeCurrency = request.getPathInfo().replace("/", "").replace(" ", "").toUpperCase();

        List<Currency> currencies;
        Currency result;
        try {
            currencies = currenciesRepository.getCurrencies();
            result = currencies.stream()
                    .filter(currency -> currency.getCode().equals(codeCurrency))
                    .findFirst().orElse(null);

            if (result == null) {
                // Валюта не найдена - 404
                request.getSession().setAttribute("errorCode", "SC_NOT_FOUND");
                String jsonError = String.format(
                        "{\"error\": \"HTTP Error 404 Not Found\", \"message\": \"%s\"}",
                        "Код валюты не найден"
                );
                request.getSession().setAttribute("jsonError", jsonError);
                response.sendError(HttpServletResponse.SC_NOT_FOUND, jsonError);
            }
            CurrencyDTO currencyDTO = new CurrencyDTO(
                    result.getId(),
                    result.getName(),
                    result.getCode(),
                    result.getSign());

            response.setStatus(HttpServletResponse.SC_OK);
            PrintWriter printWriter = response.getWriter();
            printWriter.println(mapper.writeValueAsString(currencyDTO));

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
