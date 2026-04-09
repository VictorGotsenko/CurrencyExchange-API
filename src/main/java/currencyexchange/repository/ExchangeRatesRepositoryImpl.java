package currencyexchange.repository;

import currencyexchange.model.Currency;
import currencyexchange.model.ExchangeRate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExchangeRatesRepositoryImpl implements ExchangeRatesRepository {
    private final Connection connection;

    public ExchangeRatesRepositoryImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void save(ExchangeRate ExchangeRate) throws SQLException {

        /*
        //        String sql = "INSERT INTO exchangerates (basecurrencyid, targetcurrencyid, rate) VALUES (1, 2, 0.87););";
        String sql = "INSERT INTO exchangerates (basecurrencyid, targetcurrencyid, rate) VALUES (1, 2, 0.87);";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
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
         */
    }

    @Override
    public List<ExchangeRate> getExchangeRates() throws SQLException {
        List<ExchangeRate> result = new ArrayList<>();

         String sql = "SELECT * FROM exchangerates";

        // 4. Получение данных
        ResultSet resultSet = null;

        Statement statement = connection.createStatement();
        resultSet = statement.executeQuery(sql);

        /*
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    basecurrencyid INTEGER NOT NULL,
    targetcurrencyid INTEGER NOT NULL,
    rate DECIMAL(6) NOT NULL,
    created_at TIMESTAMP DEF
         */


        while (true) {
            try {
                if (!resultSet.next()) break;
                int baseCurrency = resultSet.getInt("basecurrencyid");
                int targetCurrency = resultSet.getInt("targetcurrencyid");
                double rate = resultSet.getDouble("rate");


                ExchangeRate exchangeRate = new ExchangeRate(baseCurrency, targetCurrency, rate);

                exchangeRate.setId(resultSet.getInt("id"));

                LocalDateTime date = LocalDateTime.parse(resultSet.getString("created_at"), exchangeRate.getFormatter());
                exchangeRate.setCreatedAt(date);

                result.add(exchangeRate);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }


    @Override
    public Optional<ExchangeRate> findByCode(String baseCurrency) throws SQLException {


        return Optional.empty();
    }

    @Override
    public Optional<ExchangeRate> findById(int baseCurrencyId, int targetCurrencyId) throws SQLException {

        String sql = "SELECT * FROM exchangerates WHERE basecurrencyid = ? AND targetcurrencyid = ?";

        // 4. Получение данных
        ResultSet resultSet = null;

        try {

            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, baseCurrencyId);
            preparedStatement.setInt(2, targetCurrencyId);

            resultSet = preparedStatement.executeQuery();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (resultSet.next()) {
            ExchangeRate result = new ExchangeRate(baseCurrencyId, targetCurrencyId, resultSet.getDouble("rate"));
            result.setId(resultSet.getInt("id"));

            String created_at = resultSet.getString("created_at");
            LocalDateTime date = LocalDateTime.parse(created_at, result.getFormatter());
            result.setCreatedAt(date);

            return Optional.of(result);
        }
        return Optional.empty();
    }



}
