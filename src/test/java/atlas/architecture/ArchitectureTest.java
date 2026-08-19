package atlas.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import sharedkernel.archunit.SharedKernelRules;

@AnalyzeClasses(packages = "atlas")
class ArchitectureTest {

    @ArchTest
    static final ArchTests sharedRules = ArchTests.in(SharedKernelRules.class);
}
