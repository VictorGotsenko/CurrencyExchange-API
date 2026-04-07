package currencyexchange.listener;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import currencyexchange.model.Currency;
import currencyexchange.model.ExchangeRate;
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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@WebListener
public class ApplicationContextListener implements ServletContextListener {
    // Java Servlets часть 4 пишем CRUD приложение
    // https://www.youtube.com/watch?v=7JfkPYOoeKw
    // about Context

    private HikariDataSource hikariDataSource;
    CurrenciesRepository currenciesRepository;
    ExchangeRatesRepository exchangeRatesRepository;

    private static final String JDBC_URL = "jdbc:sqlite:currencies.sqlite";

    private static String readResourceFile(String fileName) throws IOException {
        InputStream inputStream = ApplicationContextListener.class.getClassLoader().getResourceAsStream(fileName);
        assert inputStream != null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
//        ServletContextListener.super.contextInitialized(sce);

        final ServletContext servletContext = servletContextEvent.getServletContext();

        try {
            // 1. Загрузка драйвера и подключение
            Class.forName("org.sqlite.JDBC");
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(JDBC_URL);

            // Recommended performance settings
            hikariConfig.setMaximumPoolSize(10);
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");

            hikariDataSource = new HikariDataSource(hikariConfig);


            // установка атрибута -
            servletContext.setAttribute("hikariDataSource", hikariDataSource);

//            Connection connect = hikariDataSource.getConnection();
//            connect = DriverManager.getConnection(JDBC_URL);


            // create and set repo // установка атрибута -
            currenciesRepository = new CurrenciesRepositoryImpl(hikariDataSource);
//            servletContext.setAttribute("currenciesRepository", currenciesRepository);
//            exchangeRatesRepository =new ExchangeRatesRepositoryImpl(())


            // 2. Создание таблиц, если их нет
            createTables();

            // 3. Вставка данных Insert currencies
            insertCurrencies();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void contextDestroyed(ServletContextEvent event) {
        // close resources.
        if (hikariDataSource != null && !hikariDataSource.isClosed()) {
            hikariDataSource.close(); // Terminate all connections in the pool
        }
    }

    private void createTables() throws SQLException, IOException {
        // 2. Создание таблицы, если ее нет
//            String sql = readResourceFile("schema.sql");
//            statement.execute(sql);

        Connection connect = hikariDataSource.getConnection();
        Statement statement = connect.createStatement();
        String sql = readResourceFile("schemaDrop.sql");  //drop if exist
        statement.execute(sql);
        log.info(sql);
        sql = readResourceFile("schemaCurrencies.sql");  // create
        statement.execute(sql);
        log.info(sql);
        sql = readResourceFile("schemaExchangerates.sql");  //create
        statement.execute(sql);
        log.info(sql);
    }

    private void insertCurrencies() throws SQLException {
//        Currency currency = new Currency(name, code, sign);
        try {

            Currency currency = new Currency("United States dollar", "USD", "$");
            currenciesRepository.save(currency);
            log.info("insert {}", currency.getName());

            currency.setName("Euro");
            currency.setCode("EUR");
            currency.setSign("€");
            currenciesRepository.save(currency);
            log.info("insert {}", currency.getName());

            currency.setName("Yen");
            currency.setCode("JPY");
            currency.setSign("¥");
            currenciesRepository.save(currency);
            log.info("insert {}", currency.getName());

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void insertExchangeRates() {
//        ExchangeRate exchangeRate = new ExchangeRate("USD", "EUR", 0.97);


    }

}
