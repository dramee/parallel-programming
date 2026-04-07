import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.ThreadFactory;

class ExecutorTest {

    ThreadFactory factory = Thread::new;

    @Test
    void testSimpleExecution() throws Exception {
        var executor = new SingleThreadExecutorService(factory);

        var f = executor.submit(() -> 42);

        assertEquals(42, f.get());
    }

    @Test
    void testSequentialExecution() throws Exception {
        var executor = new SingleThreadExecutorService(factory);

        StringBuilder sb = new StringBuilder();

        var f1 = executor.submit(() -> { sb.append("A"); return null; });
        var f2 = executor.submit(() -> { sb.append("B"); return null; });

        f1.get();
        f2.get();

        assertEquals("AB", sb.toString());
    }

    @Test
    void testExceptionPropagation() {
        var executor = new SingleThreadExecutorService(factory);

        var f = executor.submit(() -> {
            throw new RuntimeException("fail");
        });

        assertThrows(Exception.class, f::get);
    }

    @Test
    void testIsDone() throws Exception {
        var executor = new SingleThreadExecutorService(factory);

        var f = executor.submit(() -> 1);

        f.get();

        assertTrue(f.isDone());
    }

    @Test
    void testWorkerRestart() throws Exception {
        var executor = new SingleThreadExecutorService(factory);

        var f1 = executor.submit(() -> {
            throw new Error("fatal");
        });

        try { f1.get(); } catch (Exception ignored) {}

        var f2 = executor.submit(() -> 10);

        assertEquals(10, f2.get());
    }
}