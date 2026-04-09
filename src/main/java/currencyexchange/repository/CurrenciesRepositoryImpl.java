package currencyexchange.repository;

import currencyexchange.model.Currency;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CurrenciesRepositoryImpl implements CurrenciesRepository {
    //    private final HikariDataSource hikariDataSource;
    private Connection connection;

//    public CurrenciesRepositoryImpl(HikariDataSource hikariDataSource) {
//        this.hikariDataSource = hikariDataSource;
//    }


    public CurrenciesRepositoryImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public List<Currency> getCurrencies() throws SQLException {
        List<Currency> result = new ArrayList<>();
        String sql = "SELECT * FROM currencies";

        // 4. Получение данных
        ResultSet resultSet = null;

        Statement statement = connection.createStatement();
        resultSet = statement.executeQuery(sql);

        while (true) {
            try {
                if (!resultSet.next()) break;
                String fullname = resultSet.getString("fullname");
                String code = resultSet.getString("code");
                String sign = resultSet.getString("sign");

                Currency currency = new Currency(fullname, code, sign);

                currency.setId(resultSet.getInt("id"));

                LocalDateTime date = LocalDateTime.parse(resultSet.getString("created_at"), currency.getFormatter());
                currency.setCreatedAt(date);

                result.add(currency);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    @Override
    public void save(Currency currency) {
//        String sql = "INSERT INTO currencies (code, fullname ,sign) VALUES ('USD', 'US Dollar', '$');";
        String sql = "INSERT INTO currencies (code, fullname ,sign) VALUES (?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

//            Connection connection = hikariDataSource.getConnection();
//            PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, currency.getCode());
            preparedStatement.setString(2, currency.getName());
            preparedStatement.setString(3, currency.getSign());

            preparedStatement.executeUpdate();
            var generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                currency.setId(generatedKeys.getInt(1));
            } else {
                throw new SQLException("DB have not returned an id after saving an entity");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<Currency> findByCode(String code) throws SQLException {
//        String sql = "SELECT id, code, fullname, sign FROM currencies WHERE code = '?';";
        String sql = "SELECT * FROM currencies WHERE code = ?";
        // Получение данных

        ResultSet resultSet = null;
        try {
//            Connection connection = hikariDataSource.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, code);

            resultSet = preparedStatement.executeQuery();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (resultSet.next()) {
            Currency currency = new Currency(
                    resultSet.getString("code"),
                    resultSet.getString("fullname"),
                    resultSet.getString("sign"));

            currency.setId(resultSet.getInt("id"));

            String created_at = resultSet.getString("created_at");
            LocalDateTime date = LocalDateTime.parse(created_at, currency.getFormatter());
            currency.setCreatedAt(date);

            return Optional.of(currency);
        }

        return Optional.empty();
    }

        @Override
    public Optional<Currency> findById(int id) throws SQLException {
//        String sql = "SELECT id, code, fullname, sign FROM currencies WHERE id = '?';";
        String sql = "SELECT * FROM currencies WHERE id = ?";

        // Получение данных
        ResultSet resultSet = null;
        try {

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, id);

            resultSet = preparedStatement.executeQuery();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (resultSet.next()) {
            Currency currency = new Currency(
                    resultSet.getString("code"),
                    resultSet.getString("fullname"),
                    resultSet.getString("sign"));

            currency.setId(resultSet.getInt("id"));

            String created_at = resultSet.getString("created_at");
            LocalDateTime date = LocalDateTime.parse(created_at, currency.getFormatter());
            currency.setCreatedAt(date);

            return Optional.of(currency);
        }

        return Optional.empty();
    }




}
