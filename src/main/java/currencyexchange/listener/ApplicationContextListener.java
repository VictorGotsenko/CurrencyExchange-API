package currencyexchange.listener;

import currencyexchange.repository.CurrenciesRepository;
import currencyexchange.repository.CurrenciesRepositoryImpl;
import currencyexchange.repository.ExchangeRatesRepository;
import currencyexchange.repository.ExchangeRatesRepositoryImpl;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

@Slf4j
@WebListener
public final class ApplicationContextListener implements ServletContextListener {

    private Connection connection;
    CurrenciesRepository currenciesRepository;
    ExchangeRatesRepository exchangeRatesRepository;

    private static final String JDBC_URL = "jdbc:sqlite:currencies.sqlite?foreign_keys=true";

    private static String readResourceFile(String fileName) throws IOException {
        InputStream inputStream = ApplicationContextListener.class.getClassLoader().getResourceAsStream(fileName);
        assert inputStream != null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {

        final ServletContext servletContext = servletContextEvent.getServletContext();

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(JDBC_URL);

            Statement statement = connection.createStatement();
            statement.execute("PRAGMA foreign_keys = ON;");
            log.info("Внешние ключи включены.");

            servletContext.setAttribute("ConnectionToDB", connection);

            currenciesRepository = new CurrenciesRepositoryImpl(connection);
            exchangeRatesRepository = new ExchangeRatesRepositoryImpl(connection);

            createTables();
            insertCurrenciesData();
            insertExchangeRatesData();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createTables() throws SQLException, IOException {
        try (Statement statement = connection.createStatement()) {
            String sql = readResourceFile("schema.sql");
            statement.executeUpdate(sql);
            log.info(sql);
        }
    }

    private void insertCurrenciesData() throws SQLException, IOException {
        try (Statement statement = connection.createStatement()) {
            String sql = readResourceFile("dataCurrency.sql");
            statement.executeUpdate(sql);
            log.info(sql);
        }
        log.info("Таблица Currency заполнена");
    }

    private void insertExchangeRatesData() throws SQLException, IOException {
        try (Statement statement = connection.createStatement()) {
            String sql = readResourceFile("dataExchangeRates.sql");
            statement.executeUpdate(sql);
            log.info(sql);
        }
        log.info("Таблица ExchangeRates заполнена");
    }
}
