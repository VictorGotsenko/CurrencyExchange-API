package currencyexchange.servlets;


import currencyexchange.repository.CurrenciesRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;

@WebServlet(name = "ExchangeRatesServlet", urlPatterns = "/exchangeRates/*")
public class ExchangeRatesServlet extends HttpServlet {

    Connection connection;
    CurrenciesRepository currenciesRepository;

    @Override
    public void init() throws ServletException {
        currenciesRepository = (CurrenciesRepository) getServletContext().getAttribute("currenciesRepository");
        connection = (Connection) getServletContext().getAttribute("connectDB");
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        /* ******************************************
        Валютная пара задаётся идущими подряд кодами валют в адресе запроса.
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
          "rate": 80
        }
        HTTP коды ответов:
          Успех - 200
          Коды валют пары отсутствуют в адресе - 400
          Обменный курс для пары не найден - 404
          Ошибка (например, база данных недоступна) - 500
         ********************************************* */



    }



}
