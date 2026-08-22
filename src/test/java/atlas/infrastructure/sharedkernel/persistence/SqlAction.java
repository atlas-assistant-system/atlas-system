package atlas.infrastructure.sharedkernel.persistence;

import java.sql.SQLException;

@FunctionalInterface
interface SqlAction {

    void execute() throws SQLException;
}
