package currencyexchange.repository;

import com.zaxxer.hikari.HikariDataSource;
import currencyexchange.exeption.DatabaseException;
import currencyexchange.model.Currency;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CurrenciesRepositoryImpl implements CurrenciesRepository {
    private final HikariDataSource dataSource;
    private static final String GET_ALL = "SELECT id, code, fullname, sign FROM currencies";
    private static final String FIND_BY_ID = "SELECT id, code, fullname, sign FROM currencies WHERE id = ?";
    private static final String FIND_BY_CODE = "SELECT id, code, fullname, sign FROM currencies WHERE code = ?";
    private static final String INSERT_CURRENCY = "INSERT INTO currencies (code, fullname ,sign) VALUES (?, ?, ?)";

    public CurrenciesRepositoryImpl(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<Currency> getCurrencies() {
        List<Currency> result = new ArrayList<>();

        try (Connection connection = dataSource.getConnection()) {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(GET_ALL);

            while (resultSet.next()) {
                try {
                    Currency currency = new Currency(
                            resultSet.getString("fullname"),
                            resultSet.getString("code"),
                            resultSet.getString("sign"));

                    currency.setId(resultSet.getInt("id"));
                    result.add(currency);
                } catch (SQLException e) {
                    throw new DatabaseException("Ошибка при создании списка валют", e);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при получении списка валют", e);
        }
        return result;
    }

    @Override
    public void save(Currency currency) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement =
                    connection.prepareStatement(INSERT_CURRENCY, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, currency.getCode());
            preparedStatement.setString(2, currency.getName());
            preparedStatement.setString(3, currency.getSign());

            preparedStatement.executeUpdate();
            var generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                currency.setId(generatedKeys.getInt(1));
            } else {
                throw new DatabaseException("DB have not returned an id after saving an entity");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при сохранении валюты " + currency.getName(), e);
        }
    }

    @Override
    public Optional<Currency> findByCode(String code) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(FIND_BY_CODE);
            preparedStatement.setString(1, code);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                Currency currency = new Currency(
                        resultSet.getString("fullname"),
                        resultSet.getString("code"),
                        resultSet.getString("sign"));

                currency.setId(resultSet.getInt("id"));
                return Optional.of(currency);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при поиске валюты " + code, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Currency> findById(int id) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(FIND_BY_ID);
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                Currency currency = new Currency(
                        resultSet.getString("fullname"),
                        resultSet.getString("code"),
                        resultSet.getString("sign"));

                currency.setId(resultSet.getInt("id"));
                return Optional.of(currency);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при поиске валюты Id=" + id, e);
        }
        return Optional.empty();
    }
}
