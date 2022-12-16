package ru.job4j.quartz;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.Instant;
import java.util.Properties;

public class AlertRabbit {
    public static void main(String[] args) {
        Properties props = getProps();
        try (Connection connection = getConnection(props)) {
            try {
                Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
                scheduler.start();
                JobDataMap data = new JobDataMap();
                data.put("db", connection);
                JobDetail job = newJob(Rabbit.class)
                        .usingJobData(data)
                        .build();
                SimpleScheduleBuilder times = simpleSchedule()
                        .withIntervalInSeconds(Integer.parseInt(props.getProperty("interval")))
                        .repeatForever();
                Trigger trigger = newTrigger()
                        .startNow()
                        .withSchedule(times)
                        .build();
                scheduler.scheduleJob(job, trigger);
                Thread.sleep(10000);
                scheduler.shutdown();
            } catch (SchedulerException se) {
                se.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static Properties getProps() {
        Properties config = new Properties();
        try (InputStream in = AlertRabbit.class.getClassLoader()
                .getResourceAsStream("rabbit.properties")) {
            config.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return config;
    }

    private static Connection getConnection(Properties props) throws ClassNotFoundException, SQLException {
        Class.forName(props.getProperty("db_driver-class-name"));
        String url = props.getProperty("db_url");
        String login = props.getProperty("db_username");
        String password = props.getProperty("db_password");
        return DriverManager.getConnection(url, login, password);
    }

    public static class Rabbit implements Job {
        @Override
        public void execute(JobExecutionContext context) {
            Connection connection = (Connection) context.getMergedJobDataMap().get("db");
            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 "insert into rabbit(create_date) values (?)")) {
                statement.setTimestamp(1, Timestamp.from(Instant.now()));
                statement.execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}