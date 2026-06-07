package currencyexchange.listener;

import currencyexchange.exeption.DatabaseException;

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
import java.util.stream.Collectors;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Slf4j
@WebListener
public final class ApplicationContextListener implements ServletContextListener {

    HikariDataSource dataSource;
    private static final String JDBC_URL = "jdbc:sqlite:currencies.sqlite?foreign_keys=true";

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        ServletContext servletContext = servletContextEvent.getServletContext();

        dataSource = createHikariDataSource();
        servletContext.setAttribute("dataSource", dataSource);

        initDB();
        insertCurrenciesData();
        insertExchangeRatesData();
    }

    private HikariDataSource createHikariDataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl(JDBC_URL);

        config.setMaximumPoolSize(1);  //Only for SQLite: pool == 1
        config.setMinimumIdle(1);

        config.setConnectionTimeout(30000); // 30 секунд ожидания соединения
        config.setIdleTimeout(600000);       // 10 минут
        config.setMaxLifetime(1800000);      // 30 минут

        log.info("Config HikariCP created");
        return new HikariDataSource(config);
    }

    private static String readResourceFile(String fileName) {
        InputStream inputStream = ApplicationContextListener.class.getClassLoader().getResourceAsStream(fileName);
        assert inputStream != null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void initDB() {
        try (Connection connection = dataSource.getConnection()) {
            Statement statement = connection.createStatement();
            statement.execute("PRAGMA foreign_keys = ON;");
            log.info("Внешние ключи включены.");
            String sql = readResourceFile("schema.sql");
            statement.executeUpdate(sql);
            log.info(sql);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при инициализации БД и создании таблиц");
        }
    }

    private void insertCurrenciesData() {
        try (Connection connection = dataSource.getConnection()) {
            Statement statement = connection.createStatement();
            String sql = readResourceFile("dataCurrency.sql");
            statement.executeUpdate(sql);
            log.info(sql);
            log.info("Таблица Currency заполнена");
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при заполнении таблицы Currency");
        }
    }

    private void insertExchangeRatesData() {
        try (Connection connection = dataSource.getConnection()) {
            Statement statement = connection.createStatement();
            String sql = readResourceFile("dataExchangeRates.sql");
            statement.executeUpdate(sql);
            log.info(sql);
            log.info("Таблица ExchangeRates заполнена");
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при заполнении таблицы ExchangeRates");
        }
    }
}
