module atlas {

    requires com.fasterxml.jackson.jr.ob;
    requires java.logging;
    requires java.net.http;
    requires java.sql;
    requires jdk.httpserver;
    requires org.xerial.sqlitejdbc;

    exports atlas.infrastructure.sharedkernel.logging to java.logging;
}
