package atlas.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import sharedkernel.archunit.SharedKernelRules;

@AnalyzeClasses(packages = "atlas", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchTests sharedRules = ArchTests.in(SharedKernelRules.class);
}
