package currencyexchange.repository;

import currencyexchange.model.ExchangeRate;

import java.sql.Connection;
import java.sql.SQLException;
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

        return result;
    }


    public Optional<ExchangeRate> findByCode(String code) throws SQLException {


        return Optional.empty();
    }
}
