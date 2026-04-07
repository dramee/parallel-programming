import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class CondVarFuture<V> {

    private final Lock lock = new ReentrantLock();
    private final Condition doneCondition = lock.newCondition();

    private V result;
    private Throwable exception;
    private boolean done = false;

    public V get() throws ExecutionException {
        lock.lock();
        try {
            while (!done) {
                doneCondition.await();
            }

            if (exception != null) {
                throw new ExecutionException(exception);
            }

            return result;
        } catch (InterruptedException e) {
            throw new ExecutionException(e);
        } finally {
            lock.unlock();
        }
    }

    public boolean isDone() {
        lock.lock();
        try {
            return done;
        } finally {
            lock.unlock();
        }
    }

    void setResult(V value) {
        lock.lock();
        try {
            result = value;
            done = true;
            doneCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    void setException(Throwable t) {
        lock.lock();
        try {
            exception = t;
            done = true;
            doneCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }
}

/**
 * Single-thread executor service implementation.
 */
class SingleThreadExecutorService {

    private final ThreadFactory factory;
    private final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();

    private Thread worker;

    public SingleThreadExecutorService(ThreadFactory f) {
        this.factory = f;
        startWorkerIfNeeded();
    }

    private void startWorkerIfNeeded() {
        if (worker == null || !worker.isAlive()) {
            worker = factory.newThread(this::workerLoop);
            worker.start();
        }
    }

    private void workerLoop() {
        try {
            while (true) {
                Runnable task = queue.take();
                task.run();
            }
        } catch (Throwable fatal) {
            // thread dies → will be recreated
        }
    }

    public <T> CondVarFuture<T> submit(Callable<T> task) {
        CondVarFuture<T> future = new CondVarFuture<>();

        Runnable wrapped = () -> {
            try {
                T result = task.call();
                future.setResult(result);
            } catch (Throwable e) {
                future.setException(e);

                // fatal → kill worker
                if (e instanceof Error) {
                    throw (Error) e;
                }
            }
        };

        queue.offer(wrapped);
        startWorkerIfNeeded();

        return future;
    }
}