import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.ThreadFactory;

class JoinFutureTest {

    ThreadFactory factory = Thread::new;

    @Test
    void testSimpleResult() throws Exception {
        var executor = new ThreadPerTaskExecutorService(factory);

        var f = executor.submit(() -> 42);

        assertEquals(42, f.get());
    }

    @Test
    void testExceptionPropagation() {
        var executor = new ThreadPerTaskExecutorService(factory);

        var f = executor.submit(() -> {
            throw new RuntimeException("fail");
        });

        assertThrows(Exception.class, f::get);
    }

    @Test
    void testIsDoneAfterGet() throws Exception {
        var executor = new ThreadPerTaskExecutorService(factory);

        var f = executor.submit(() -> 1);

        f.get();

        assertTrue(f.isDone());
    }

    @Test
    void testParallelExecution() throws Exception {
        var executor = new ThreadPerTaskExecutorService(factory);

        var f1 = executor.submit(() -> {
            Thread.sleep(100);
            return 1;
        });

        var f2 = executor.submit(() -> 2);

        assertNotSame(f1.thread, f2.thread);
        assertEquals(2, f2.get());
        assertEquals(1, f1.get());
    }

    @Test
    void testMultipleTasks() throws Exception {
        var executor = new ThreadPerTaskExecutorService(factory);

        var f1 = executor.submit(() -> 10);
        var f2 = executor.submit(() -> 20);

        assertEquals(10, f1.get());
        assertEquals(20, f2.get());
    }
}