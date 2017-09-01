package nl.gogognome.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AssertExtensions {

    public static void assertThrows(Class<? extends Throwable> expectedException, RunnableThrowingException runnable) {
        Throwable actualThrowable = null;
        try {
            runnable.run();
        } catch (Throwable t) {
            actualThrowable = t;
        }
        assertNotNull("Expected exception " + expectedException + " not thrown", actualThrowable);
        assertEquals("Different exception was thrown", expectedException, actualThrowable.getClass());
    }

    public interface RunnableThrowingException {
        void run() throws Exception;
    }
}
