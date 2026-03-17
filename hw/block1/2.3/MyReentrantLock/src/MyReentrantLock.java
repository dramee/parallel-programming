//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
class MyReentrantLock {
    private Thread owner = null;
    private final NonReentrantLock lock;
    private int usages = 0;
    private int backOff = 1;
    private static final int MAX_BACKOFF = 1024;

    /**
     * Creates an instance of MyReentrantLock
     */
    public MyReentrantLock(NonReentrantLockFactory factory) {
        this.lock = factory.create();
    }

    private boolean myTryLock() {
        try {
            lock.lock();
            return true;
        } catch (IllegalMonitorStateException e) {
            return false;
        }
    }

    /**
     * Acquires the lock.
     * <p>
     *     Acquires the lock if it is not held by another thread and returns immediately, setting the lock usages to one.
     * <p>
     *     If current thread is holding this lock, then increments usages and method returns immediately.
     * <p>
     *     If current thread isn't holding this lock, but another thread do, then spinning with exponential backOff policy
     */
    public void lock() {
        Thread current = Thread.currentThread();
        while (true) {
            if (myTryLock()) {
                try {
                    if (owner == null) {
                        owner = current;
                        usages = 1;
                        return;
                    } else if (owner == Thread.currentThread()) {
                        usages++;
                        return;
                    }
                } finally {
                    lock.unlock();
                }
                try {
                    Thread.sleep(backOff);
                    if (backOff < MAX_BACKOFF) {
                        backOff*=2;
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                Thread.onSpinWait();
            }
        }
    }

    /**
     * Decrements usages.
     * If usages == 0, then release the lock.
     * @throws IllegalMonitorStateException if current thread doesn't own this lock or owner isn't set.
     */
    public void unlock() throws IllegalMonitorStateException {
        if (owner == null) throw new IllegalMonitorStateException("Trying to unlock when thread has no owner");
        Thread current = Thread.currentThread();
        if (owner != current) throw new IllegalMonitorStateException("Trying to unlock while lock" +
                " is held by another thread");
        if (owner == Thread.currentThread()) {
            lock.lock();
            try {
                usages--;
                if (usages == 0) {
                    owner = null;
                }
            } finally {
                lock.unlock();
            }
        }
    }
}