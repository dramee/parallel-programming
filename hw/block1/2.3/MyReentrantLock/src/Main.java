import java.util.concurrent.locks.ReentrantLock;

public class Main {
    static int x = 0;

    public static void main(String[] args) throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();
//        TODO: implement factory
        Thread A = new Thread(() -> {
            lock.lock();
            try {
                x++;
            } finally {
                lock.unlock();
            }
        });
        Thread B = new Thread(() -> {
            lock.lock();
            try {
                x++;
            } finally {
                lock.unlock();
            }
        });
        A.start();
        B.start();
        A.join();
        B.join();
        System.out.println(x);
    }
}
