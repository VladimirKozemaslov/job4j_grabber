package ru.job4j.grabber;

import ru.job4j.quartz.AlertRabbit;

import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PsqlStore implements Store {

    private Connection cnn;

    public PsqlStore(Properties cfg) {
        try {
            Class.forName(cfg.getProperty("jdbc.driver"));
            String url = cfg.getProperty("url");
            String login = cfg.getProperty("username");
            String password = cfg.getProperty("password");
            cnn = DriverManager.getConnection(url, login, password);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void save(Post post) {
        try (PreparedStatement statement =
                     cnn.prepareStatement(
                             "insert into post(name, text, link, created) "
                                     + "values (?, ?, ?, ?) "
                                     + "on conflict (link) "
                                     + "do update set name = excluded.name, "
                                     + "text = excluded.text, "
                                     + "created = excluded.created")) {
            statement.setString(1, post.getTitle());
            statement.setString(2, post.getDescription());
            statement.setString(3, post.getLink());
            statement.setTimestamp(4, Timestamp.valueOf(post.getCreated()));
            statement.execute();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<Post> getAll() {
        List<Post> list = new ArrayList<>();
        try (PreparedStatement statement =
                     cnn.prepareStatement(
                             "select * from post")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    list.add(getPost(resultSet));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Post findById(int id) {
        Post post = null;
        try (PreparedStatement statement = cnn.prepareStatement("select * from post where id = ?")) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    post = getPost(resultSet);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return post;
    }

    @Override
    public void close() throws Exception {
        if (cnn != null) {
            cnn.close();
        }
    }

    private Post getPost(ResultSet resultSet) {
        try {
            return new Post(
                    resultSet.getInt("id"),
                    resultSet.getString("name"),
                    resultSet.getString("link"),
                    resultSet.getString("text"),
                    resultSet.getTimestamp("created").toLocalDateTime()
            );
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void main(String[] args) {
        try (InputStream in = PsqlStore.class.getClassLoader()
                .getResourceAsStream("grabber.properties")) {
            Properties config = new Properties();
            config.load(in);
            try (PsqlStore psqlStore = new PsqlStore(config)) {
                psqlStore.save(new Post("Android-разработчик в ВК Видео",
                        "/vacancies/1000108567",
                        "VK Видео - это один из самых крупных сервисов потокового видео в России. "
                                + "Ежедневно им пользуются десятки миллионов людей и перед нами стоит "
                                + "амбициозная задача - стать видеосервисом 1 в РФ.",
                        LocalDateTime.of(2022,12, 21, 16, 50)));
                psqlStore.save(new Post("QA Automation Engineer",
                        "/vacancies/1000116009",
                        "Аутсорсинговая аккредитованная IT-компания Aston приглашает к "
                                + "сотрудничеству QA Automation Engineer на "
                                + "масштабный проект в сфере FinTech.",
                        LocalDateTime.of(2022,12, 21, 17, 15)));
                for (Post post : psqlStore.getAll()) {
                    System.out.println(post);
                }
                psqlStore.save(new Post("Разработчик Java (Биометрия)",
                        "/vacancies/1000108567",
                        "Наша команда объединяет более 1300 IT специалистов в 13 офисах "
                                + "по всей стране. Мы гордимся нашими проектами:",
                        LocalDateTime.of(2022,12, 21, 18, 11)));
                System.out.println(psqlStore.findById(2));
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}