import java.util.concurrent.ExecutionException;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadFactory;

class JoinFuture<V> {

    Thread thread;
    V result;
    Throwable exception;

    /**
     * Waits for completion and returns result or throws ExecutionException
     */
    public V get() throws ExecutionException {
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (exception != null) {
            throw new ExecutionException(exception);
        }

        return result;
    }

    /**
     * Returns true if thread finished (normally or with exception)
     */
    public boolean isDone() {
        return !thread.isAlive();
    }
}

class ThreadPerTaskExecutorService {

    private final ThreadFactory factory;

    public ThreadPerTaskExecutorService(ThreadFactory f) {
        this.factory = f;
    }

    public <T> JoinFuture<T> submit(Callable<T> task) {
        JoinFuture<T> future = new JoinFuture<>();

        Thread t = factory.newThread(() -> {
            try {
                future.result = task.call();
            } catch (Throwable e) {
                future.exception = e;
            }
        });

        future.thread = t;
        t.start();

        return future;
    }
}