package currencyexchange.repository;

import currencyexchange.model.Currency;
import currencyexchange.model.ExchangeRate;

import java.math.BigDecimal;
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
    private final CurrenciesRepository currenciesRepository;

    public ExchangeRatesRepositoryImpl(Connection connection) {
        this.connection = connection;
        currenciesRepository = new CurrenciesRepositoryImpl(connection);
    }

    @Override
    public void save(ExchangeRate exchangeRate) throws SQLException {
        String sql = "INSERT INTO exchangerates (basecurrencyid, targetcurrencyid, rate) VALUES (?, ?, ?);";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setInt(1, exchangeRate.getBaseCurrencyId());
            preparedStatement.setInt(2, exchangeRate.getTargetCurrencyId());
            preparedStatement.setBigDecimal(3, exchangeRate.getRate());

            preparedStatement.executeUpdate();
            var generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                exchangeRate.setId(generatedKeys.getInt(1));
            } else {
                throw new SQLException("DB have not returned an id after saving an entity");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public List<ExchangeRate> getExchangeRates() throws SQLException {
        List<ExchangeRate> result = new ArrayList<>();

        String sql = "SELECT * FROM exchangerates";

        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql);

        while (true) {
            try {
                if (!resultSet.next()) break;
                int baseCurrency = resultSet.getInt("basecurrencyid");
                int targetCurrency = resultSet.getInt("targetcurrencyid");
                String rate = resultSet.getString("rate");

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
    public Optional<ExchangeRate> findById(int id) throws SQLException {
        String sql = "SELECT * FROM exchangerates WHERE id = ?";

        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setInt(1, id);
        ResultSet resultSet = preparedStatement.executeQuery();

        if (resultSet.next()) {
            ExchangeRate result = new ExchangeRate(
                    resultSet.getInt("basecurrencyid"),
                    resultSet.getInt("targetcurrencyid"),
                    resultSet.getString("rate"));

            result.setId(resultSet.getInt("id"));
            String created_at = resultSet.getString("created_at");
            LocalDateTime date = LocalDateTime.parse(created_at, result.getFormatter());
            result.setCreatedAt(date);

            return Optional.of(result);
        }
        return Optional.empty();
    }

    @Override
    public Optional<ExchangeRate> findByCodes(String baseCurrencyCode, String targetCurrencyCode) throws SQLException {
        Optional<Currency> baseCurrencyIs = currenciesRepository.findByCode(baseCurrencyCode);
        Optional<Currency> targetCurrencyIs = currenciesRepository.findByCode(targetCurrencyCode);

        if (baseCurrencyIs.isEmpty() || targetCurrencyIs.isEmpty()) {
            return Optional.empty();
        }

        Currency baseCurrency = baseCurrencyIs.get();
        Currency targetCurrency = targetCurrencyIs.get();
        return findByCurrencyIDs(baseCurrency.getId(), targetCurrency.getId());
    }

    @Override
    public Optional<ExchangeRate> findByCurrencyIDs(int baseCurrencyId, int targetCurrencyId) throws SQLException {
        String sql = "SELECT * FROM exchangerates WHERE basecurrencyid = ? AND targetcurrencyid = ?";

        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setInt(1, baseCurrencyId);
        preparedStatement.setInt(2, targetCurrencyId);
        ResultSet resultSet = preparedStatement.executeQuery();

        if (resultSet.next()) {
            ExchangeRate result = new ExchangeRate(baseCurrencyId, targetCurrencyId, resultSet.getString("rate"));
            result.setId(resultSet.getInt("id"));
            String created_at = resultSet.getString("created_at");
            LocalDateTime date = LocalDateTime.parse(created_at, result.getFormatter());
            result.setCreatedAt(date);

            return Optional.of(result);
        }
        return Optional.empty();
    }

    @Override
    public void update(int exchangeRateId, BigDecimal rate) throws SQLException {
        String sql = "UPDATE exchangerates SET rate = ? WHERE id = ?";

        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setBigDecimal(1, rate);
        preparedStatement.setInt(2, exchangeRateId);
        preparedStatement.executeUpdate();
    }


}
