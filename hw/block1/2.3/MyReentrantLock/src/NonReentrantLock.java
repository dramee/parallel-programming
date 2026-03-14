public interface NonReentrantLock {
    void lock() throws IllegalMonitorStateException;
    void unlock();
}
