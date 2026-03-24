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

    static class LockShowBackoff extends MyReentrantLock {

        /**
         * Creates an instance of MyReentrantLock
         *
         * @param factory
         */
        public LockShowBackoff(NonReentrantLockFactory factory) {
            super(factory);
        }

        public int getBackOff() {
            return backOff;
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
                    Thread.sleep(100);
                    counter++;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    lock.unlock();
                }
            }

            public long get() {
                lock.lock();
                try {
                    Thread.sleep(100);
                    return counter;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
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
        LockShowBackoff lock = new LockShowBackoff(new SimpleFactory());
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
        assertTrue(lock.getBackOff() > 1);
    }
}