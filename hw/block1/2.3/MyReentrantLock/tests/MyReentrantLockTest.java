import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

class MyReentrantLockTest {

    static class SimpleLock implements NonReentrantLock {

        private final ReentrantLock lock = new ReentrantLock();
        private Thread owner = null;

        public void lock() {
            Thread current = Thread.currentThread();

            if (current == owner) {
                throw new IllegalStateException("Lock is non-reentrant");
            }

            lock.lock();
            owner = current;
        }

        public void unlock() {
            if (Thread.currentThread() != owner) {
                throw new IllegalMonitorStateException("Current thread does not hold the lock");
            }

            owner = null;
            lock.unlock();
        }
    }


    static class SimpleFactory implements NonReentrantLockFactory {
        public NonReentrantLock create() {
            return new SimpleLock();
        }
    }

    @Test
    void testReentrantLock() {
        MyReentrantLock lock = new MyReentrantLock(new SimpleFactory());

        lock.lock();
        lock.lock();

        lock.unlock();
        lock.unlock();
    }

    @Test
    void testIllegalUnlock() {
        MyReentrantLock lock = new MyReentrantLock(new SimpleFactory());

        assertThrows(IllegalMonitorStateException.class, lock::unlock);
    }

    @Test
    void testMutualExclusion() throws Exception {
        class Counter {
            private final MyReentrantLock lock = new MyReentrantLock(new SimpleFactory());
            private long counter;

            public Counter(long initial) {
                counter = initial;
            }

            public void increment() {
                lock.lock();
                try {
                    counter++;
                } finally {
                    lock.unlock();
                }
            }

            public long get() {
                lock.lock();
                try {
                    return counter;
                } finally {
                    lock.unlock();
                }
            }
        }
        Counter counter = new Counter(0);
        Thread A = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                counter.increment();
            }
        });
        Thread B = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                counter.increment();
            }
        });
        Thread C = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                counter.increment();
            }
        });
        A.start();
        B.start();
        C.start();
        A.join();
        B.join();
        C.join();
        long res = counter.get();
        assertEquals(150, res);
    }

    @Test
    void testBackOffPolicy() throws InterruptedException {
        AtomicInteger x = new AtomicInteger();
        MyReentrantLock lock = new MyReentrantLock(new SimpleFactory());
        Thread A = new Thread(() -> {
            lock.lock();
            try {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } finally {
                lock.unlock();
            }
        });
        Thread B = new Thread(() -> {
           lock.lock();
           try {
               x.getAndIncrement();
           } finally {
               lock.unlock();
           }
        });
        A.start();
        B.start();
        A.join();
        B.join();
        assertEquals(1, x.get());
    }
}