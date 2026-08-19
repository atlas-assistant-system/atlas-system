module sharedkernel {

    requires transitive jdk.httpserver;
    requires java.logging;
    requires java.sql;

    exports sharedkernel.domain.ddd;
    exports sharedkernel.domain.events;
    exports sharedkernel.domain.exceptions;
    exports sharedkernel.domain.guards;
    exports sharedkernel.domain.results;
    exports sharedkernel.domain.types;
    exports sharedkernel.application.cqrs;
    exports sharedkernel.application.events;
    exports sharedkernel.application.logging;
    exports sharedkernel.application.outbox;
    exports sharedkernel.application.paging;
    exports sharedkernel.application.ports;
    exports sharedkernel.application.unitofwork;
    exports sharedkernel.infrastructure;
    exports sharedkernel.infrastructure.console;
    exports sharedkernel.infrastructure.logging;
    exports sharedkernel.infrastructure.persistence;
    exports sharedkernel.presentation.errors;
    exports sharedkernel.presentation.http;
    exports sharedkernel.presentation.sse;
}
