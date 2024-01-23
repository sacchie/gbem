import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import org.junit.jupiter.api.Test

internal class DependencyTest {
    companion object {
        private val COMMON_PACKAGES = arrayOf("java..", "kotlin..", "org.jetbrains..")
    }

    private fun javaClasses(): JavaClasses = ClassFileImporter().importPaths("./")

    @Test
    fun testCpuDependencies() {
        classes()
            .that()
            .haveNameMatching("cpu\\..*")
            .should()
            .onlyDependOnClassesThat(
                resideInAPackage("cpu..").or(resideInAnyPackage(*COMMON_PACKAGES))
            )
            .check(javaClasses())
    }
}
