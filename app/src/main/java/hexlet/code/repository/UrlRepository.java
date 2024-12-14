package hexlet.code.repository;

import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UrlRepository extends BaseRepository {
    private static final Logger LOGGER = Logger.getLogger(UrlRepository.class.getName());

    public static void save(Url url) throws SQLException {
        var sql = "INSERT INTO urls (name, created_at) VALUES (?, ?)";
        try (var conn = dataSource.getConnection();
             var preparedStatement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, url.getName());
            var createdAt = LocalDateTime.now();
            preparedStatement.setTimestamp(2, Timestamp.valueOf(createdAt));

            preparedStatement.executeUpdate();
            var generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                url.setId(generatedKeys.getLong(1));
                url.setCreatedAt(createdAt);

                LOGGER.info("URL saved: " + url.getName());
            } else {
                throw new SQLException("DB did not return an ID after saving an entity");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving URL: " + url.getName(), e);
            throw e;
        }
    }

    public static Optional<Url> findByName(String name) throws SQLException {
        var sql = "SELECT id, name, created_at FROM urls WHERE name = ?";
        try (var conn = dataSource.getConnection();
             var preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, name);
            try (var resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    Url url = new Url(
                            resultSet.getString("name"),
                            resultSet.getTimestamp("created_at").toLocalDateTime()
                    );
                    url.setId(resultSet.getLong("id"));
                    return Optional.of(url);
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<Url> findByIndex(Long index) throws SQLException {
        var sql = "SELECT id, name, created_at FROM urls WHERE id = ?";
        try (var conn = dataSource.getConnection();
             var preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setLong(1, index);
            try (var resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    Url url = new Url(
                            resultSet.getString("name"),
                            resultSet.getTimestamp("created_at").toLocalDateTime()
                    );
                    url.setId(resultSet.getLong("id"));
                    return Optional.of(url);
                }
            }
        }
        return Optional.empty();
    }

    public static boolean existsByName(String name) throws SQLException {
        var sql = "SELECT 1 FROM urls WHERE name = ? LIMIT 1";
        try (var conn = dataSource.getConnection();
             var preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setString(1, name);
            try (var resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    public static List<Url> getEntities() throws SQLException {
        var sql = "SELECT id, name, created_at FROM urls";
        List<Url> urls = new ArrayList<>();
        try (var conn = dataSource.getConnection();
             var preparedStatement = conn.prepareStatement(sql);
             var resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                Url url = new Url(
                        resultSet.getString("name"),
                        resultSet.getTimestamp("created_at").toLocalDateTime()
                );
                url.setId(resultSet.getLong("id"));
                url.setUrlChecks(getChecksForUrl(url.getId()));
                urls.add(url);
            }
        }
        return urls;
    }

    private static List<UrlCheck> getChecksForUrl(Long urlId) throws SQLException {
        var sql = "SELECT id, url_id, status_code, title, h1, description, created_at FROM url_checks WHERE url_id = ?";
        List<UrlCheck> checks = new ArrayList<>();
        try (var conn = dataSource.getConnection();
             var preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setLong(1, urlId);
            try (var resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    UrlCheck check = new UrlCheck(
                            resultSet.getLong("url_id"),
                            resultSet.getInt("status_code"),
                            resultSet.getString("title"),
                            resultSet.getString("h1"),
                            resultSet.getString("description"),
                            resultSet.getTimestamp("created_at").toLocalDateTime()
                    );
                    check.setId(resultSet.getLong("id"));
                    checks.add(check);
                }
            }
        }
        return checks;
    }

    public static void clear() {
        try (Connection connection = DriverManager
                .getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "sa", "")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("DELETE FROM urls");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
