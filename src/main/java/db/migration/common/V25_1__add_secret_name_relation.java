package db.migration.common;

import static io.pivotal.security.util.UuidUtil.makeUuid;

import java.sql.Types;
import java.util.List;
import org.flywaydb.core.api.migration.spring.SpringJdbcMigration;
import org.springframework.jdbc.core.JdbcTemplate;

@SuppressWarnings("checkstyle:typename")
public class V25_1__add_secret_name_relation implements SpringJdbcMigration {

  public void migrate(JdbcTemplate jdbcTemplate) throws Exception {
    String databaseName = jdbcTemplate.getDataSource().getConnection().getMetaData()
        .getDatabaseProductName().toLowerCase();
    List<String> names = jdbcTemplate
        .queryForList("select distinct(name) from named_secret", String.class);

    for (String name : names) {
      jdbcTemplate.update(
          "insert into secret_name (uuid, name) values (?, ?)",
          new Object[]{makeUuid(databaseName), name},
          new int[]{Types.VARBINARY, Types.VARCHAR}
      );
    }
  }
}
