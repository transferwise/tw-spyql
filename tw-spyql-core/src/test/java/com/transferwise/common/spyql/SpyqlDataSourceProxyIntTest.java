package com.transferwise.common.spyql;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import com.transferwise.common.spyql.event.GetConnectionEvent;
import com.transferwise.common.spyql.event.ResultSetNextRowsEvent;
import com.transferwise.common.spyql.event.StatementExecuteEvent;
import com.transferwise.common.spyql.listener.SpyqlConnectionListener;
import com.transferwise.common.spyql.listener.SpyqlDataSourceListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfig.class})

public class SpyqlDataSourceProxyIntTest {

  @Autowired
  DataSource dataSource;

  private List<ResultSetNextRowsEvent> resultSetNextRowsEvents;
  private List<StatementExecuteEvent> statementExecuteEvents;
  private SpyqlDataSourceListener listener;
  private SpyqlDataSource proxy;

  @BeforeEach
  public void setup() {
    listener = new TestListener();
    proxy = new SpyqlDataSource(dataSource, listener);
    resultSetNextRowsEvents = new ArrayList<>();
    statementExecuteEvents = new ArrayList<>();
  }

  @Test
  public void dataSourceCanBeCreated() throws SQLException {
    Connection connection = proxy.getConnection();
    assertThat(connection, is(notNullValue()));
    PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1");
    try (ResultSet result = preparedStatement.executeQuery()) {
      result.first();
      assertThat(result.getInt(1), is(equalTo(1)));
    }
  }

  @Test
  public void correctResultSetEventsAreReceived() {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(proxy);

    List<String> personNames = jdbcTemplate.queryForList("SELECT NAME from PERSON", String.class);

    assertThat(personNames.size(), is(equalTo(250)));

    // We use batches
    assertThat(resultSetNextRowsEvents.size(), is(equalTo(3)));
    assertThat(resultSetNextRowsEvents.get(0).getRowsCount(), is(equalTo(100L)));
    assertThat(resultSetNextRowsEvents.get(1).getRowsCount(), is(equalTo(100L)));

    // ResultSetClose is also considered
    assertThat(resultSetNextRowsEvents.get(2).getRowsCount(), is(equalTo(50L)));
  }

  @Test
  public void correctResultSetEventsAreReceivedWhenUsingPlainStatement() throws Exception {
    int recordsCount = 0;
    Connection con = proxy.getConnection();
    try {
      try (Statement stmt = con.createStatement()) {
        try (ResultSet rs = stmt.executeQuery("SELECT NAME FROM PERSON")) {
          while (rs.next()) {
            recordsCount++;
          }
        }
      }
    } finally {
      DataSourceUtils.doCloseConnection(con, proxy);
    }
    assertThat(recordsCount, is(equalTo(250)));

    // We use batches
    assertThat(resultSetNextRowsEvents.size(), is(equalTo(3)));
    assertThat(resultSetNextRowsEvents.get(0).getRowsCount(), is(equalTo(100L)));
    assertThat(resultSetNextRowsEvents.get(1).getRowsCount(), is(equalTo(100L)));

    // ResultSetClose is also considered
    assertThat(resultSetNextRowsEvents.get(2).getRowsCount(), is(equalTo(50L)));
  }

  @Test
  public void updatingRecordsWillRecordAffectedRowsCount() {
    JdbcTemplate jdbcTemplate = new JdbcTemplate(proxy);

    int updatedRows = jdbcTemplate.update("UPDATE PERSON SET NAME=? WHERE id=?", "Kristo", 666);
    assertThat(updatedRows, is(equalTo(0)));
    assertThat(statementExecuteEvents.get(0).getAffectedRowsCount(), is(equalTo(0L)));

    updatedRows = jdbcTemplate.update("UPDATE PERSON SET NAME=? WHERE id=?", "Kristo", 1);
    assertThat(updatedRows, is(equalTo(1)));
    assertThat(statementExecuteEvents.get(1).getAffectedRowsCount(), is(equalTo(1L)));

    updatedRows = jdbcTemplate.update("UPDATE PERSON SET NAME=? ", "Kristo");
    assertThat(updatedRows, is(equalTo(250)));
    assertThat(statementExecuteEvents.get(2).getAffectedRowsCount(), is(equalTo(250L)));

    int[] updatedBatchRows = jdbcTemplate.batchUpdate("UPDATE PERSON SET NAME=? WHERE ID=?", new BatchPreparedStatementSetter() {
      @Override
      public void setValues(PreparedStatement ps, int i) throws SQLException {
        ps.setString(1, "Kristo");
        ps.setInt(2, 1);
      }

      @Override
      public int getBatchSize() {
        return 10;
      }
    });
    updatedRows = 0;
    for (int i : updatedBatchRows) {
      updatedRows += i;
    }
    assertThat(updatedRows, is(equalTo(10)));
    assertThat(statementExecuteEvents.get(3).getAffectedRowsCount(), is(equalTo(10L)));

    //TODO: Add batch support
  }

  class TestListener implements SpyqlDataSourceListener {

    @Override
    public SpyqlConnectionListener onGetConnection(GetConnectionEvent event) {
      return new SpyqlConnectionListener() {
        @Override
        public void onResultSetNextRecords(ResultSetNextRowsEvent event) {
          resultSetNextRowsEvents.add(event);
        }

        @Override
        public void onStatementExecute(StatementExecuteEvent event) {
          statementExecuteEvents.add(event);
        }
      };
    }
  }
}
