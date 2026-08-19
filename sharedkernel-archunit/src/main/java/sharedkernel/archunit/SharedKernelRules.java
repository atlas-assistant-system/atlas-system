package sharedkernel.archunit;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;

public final class SharedKernelRules {

    private SharedKernelRules() {}

    @ArchTest
    static final ArchTests layers = ArchTests.in(LayerRules.class);

    @ArchTest
    static final ArchTests domainPurity = ArchTests.in(DomainPurityRules.class);

    @ArchTest
    static final ArchTests ddd = ArchTests.in(DddRules.class);
}
