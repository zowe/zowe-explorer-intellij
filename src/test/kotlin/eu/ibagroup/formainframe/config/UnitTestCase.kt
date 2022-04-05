package eu.ibagroup.formainframe.config
import com.intellij.mock.MockApplication
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import io.mockk.spyk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * A test case for unit tests, which only depend on the existence of an application.
 */
open class UnitTestCase {
    /**
     * spying on MockApplication
     */
    val app = spyk(MockApplication(Disposer.newDisposable("")))

    /**
     * Setting up MockApplication
     */
    @BeforeEach
    fun setUp() {
        ApplicationManager.setApplication(app,Disposer.newDisposable(""))
    }

    /**
     * Tearing down MockApplication
     */
    @AfterEach
    fun tearDown() {
        app.dispose()
    }
}










