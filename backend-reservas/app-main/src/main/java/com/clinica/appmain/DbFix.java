package com.clinica.appmain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Elimina constraints CHECK obsoletos de la tabla APPOINTMENTS generados
 * por versiones anteriores de Hibernate sobre columnas enum.
 *
 * Usa la sintaxis correcta de H2 2.x:
 *   - INFORMATION_SCHEMA.TABLE_CONSTRAINTS  (reemplaza a .CONSTRAINTS)
 *   - Nombres de tabla en MAYÚSCULAS (H2 2.x es case-sensitive en metadatos)
 */
@Component
public class DbFix implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DbFix.class);

    private final JdbcTemplate jdbcTemplate;

    public DbFix(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {

        // 1. Eliminar constraint específico CONSTRAINT_E si aún existe
        dropConstraintIfExists("APPOINTMENTS", "CONSTRAINT_E");

        // 2. Eliminar todos los CHECK constraints restantes en APPOINTMENTS
        //    (H2 2.x: usar TABLE_CONSTRAINTS, nombre de tabla en MAYÚSCULAS)
        try {
            jdbcTemplate.query(
                "SELECT CONSTRAINT_NAME " +
                "FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                "WHERE TABLE_NAME = 'APPOINTMENTS' " +
                "  AND CONSTRAINT_TYPE = 'CHECK'",
                rs -> {
                    String name = rs.getString(1);
                    dropConstraintIfExists("APPOINTMENTS", name);
                });
        } catch (Exception e) {
            log.warn("DbFix: no se pudieron listar CHECK constraints de APPOINTMENTS: {}", e.getMessage());
        }
    }

    private void dropConstraintIfExists(String table, String constraint) {
        try {
            jdbcTemplate.execute(
                "ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS " + constraint);
            log.debug("DbFix: constraint {} eliminado (o no existía).", constraint);
        } catch (Exception e) {
            log.warn("DbFix: no se pudo eliminar constraint {}: {}", constraint, e.getMessage());
        }
    }
}
