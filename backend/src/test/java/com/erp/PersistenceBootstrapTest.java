package com.erp;

import com.erp.support.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceBootstrapTest extends MySqlIntegrationTest {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void startsWithMySql() {
        var product = jdbcTemplate.queryForObject("select @@version_comment", String.class);

        assertThat(product).containsIgnoringCase("MySQL");
    }
}
