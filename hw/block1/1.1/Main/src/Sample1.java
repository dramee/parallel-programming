
public class Sample1 {
    public static void main(String[] args) throws Exception {
        Thread B = new Thread(() -> {
            System.out.println("B started");
            throw new RuntimeException("Boom in B");
        }, "Thread_B");

        Thread A = new Thread(() -> {
            System.out.println("A starting B");
            B.start();
            try {
                B.join();
                System.out.println("A finished join B");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, "Thread_A");

        A.start();
        A.join();
        System.out.println("Main finished");
    }
}