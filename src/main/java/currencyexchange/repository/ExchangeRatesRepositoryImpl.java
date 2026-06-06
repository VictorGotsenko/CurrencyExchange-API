package currencyexchange.repository;

import currencyexchange.exeption.DatabaseException;
import currencyexchange.model.ExchangeRate;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ExchangeRatesRepositoryImpl implements ExchangeRatesRepository {
    private final Connection connection;

    private static final String GET_ALL = "SELECT id, basecurrencyid, targetcurrencyid, rate FROM exchangerates";
    private static final String FIND_BY_ID =
            "SELECT id, basecurrencyid, targetcurrencyid, rate FROM exchangerates WHERE id = ?";
    private static final String FIND_BY_CURRENCY_IDS = """
            SELECT id, basecurrencyid, targetcurrencyid, rate
            FROM exchangerates
            WHERE basecurrencyid = ? AND targetcurrencyid = ?""";

    private static final String INSERT_PAIR =
            "INSERT INTO exchangerates (basecurrencyid, targetcurrencyid, rate) VALUES (?, ?, ?);";

    private static final String UPDATE_RATE = "UPDATE exchangerates SET rate = ? WHERE id = ?";

    public ExchangeRatesRepositoryImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void save(ExchangeRate exchangeRate) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(INSERT_PAIR,
                Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setInt(1, exchangeRate.getBaseCurrencyId());
            preparedStatement.setInt(2, exchangeRate.getTargetCurrencyId());
            preparedStatement.setBigDecimal(3, exchangeRate.getRate());

            preparedStatement.executeUpdate();
            var generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                exchangeRate.setId(generatedKeys.getInt(1));
            } else {
                throw new DatabaseException("DB have not returned an id after saving an entity");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось вставить пару: baseId=" + exchangeRate.getBaseCurrencyId()
                    + "targetId" + exchangeRate.getTargetCurrencyId(), e);
        }
    }

    @Override
    public List<ExchangeRate> getExchangeRates() {
        List<ExchangeRate> result = new ArrayList<>();

        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(GET_ALL);
            while (resultSet.next()) {
                try {
                    int baseCurrency = resultSet.getInt("basecurrencyid");
                    int targetCurrency = resultSet.getInt("targetcurrencyid");
                    String rate = resultSet.getString("rate");

                    ExchangeRate exchangeRate = new ExchangeRate(baseCurrency, targetCurrency, new BigDecimal(rate));
                    exchangeRate.setId(resultSet.getInt("id"));
                    result.add(exchangeRate);
                } catch (SQLException e) {
                    throw new DatabaseException("Не удалось получить список обменных курсов ", e);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при получении список обменных курсов ", e);
        }
        return result;
    }

    @Override
    public Optional<ExchangeRate> findById(int id) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(FIND_BY_ID)) {
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                ExchangeRate result = new ExchangeRate(
                        resultSet.getInt("basecurrencyid"),
                        resultSet.getInt("targetcurrencyid"),
                        new BigDecimal(resultSet.getString("rate")));

                result.setId(resultSet.getInt("id"));
                return Optional.of(result);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при поиске. Id=" + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<ExchangeRate> findByCurrencyIDs(int baseCurrencyId, int targetCurrencyId) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(FIND_BY_CURRENCY_IDS)) {
            preparedStatement.setInt(1, baseCurrencyId);
            preparedStatement.setInt(2, targetCurrencyId);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                ExchangeRate result = new ExchangeRate(baseCurrencyId, targetCurrencyId,
                        new BigDecimal(resultSet.getString("rate")));
                result.setId(resultSet.getInt("id"));

                return Optional.of(result);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при поиске пары. baseId=" + baseCurrencyId
                    + ":targetId=" + targetCurrencyId, e);
        }
        return Optional.empty();
    }

    @Override
    public void update(int exchangeRateId, BigDecimal rate) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_RATE)) {
            preparedStatement.setBigDecimal(1, rate);
            preparedStatement.setInt(2, exchangeRateId);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при обновлении курса. Id=" + exchangeRateId, e);
        }
    }
}
