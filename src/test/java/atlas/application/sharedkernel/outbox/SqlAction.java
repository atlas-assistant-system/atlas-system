package atlas.application.sharedkernel.outbox;

import java.sql.SQLException;

@FunctionalInterface
interface SqlAction {

    void execute() throws SQLException;
}
