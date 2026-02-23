public class Sample3 {
    public static void main(String[] args) throws Exception {
        Thread B = new Thread(() -> {
            throw new RuntimeException("Boom in thread B");
        }, "Thread_B");
        Thread A = new Thread(() -> {
            System.out.println("A starting B");
            B.start();
            try {
                B.join();
                System.out.println("A joining B");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, "Thread_A");
        Thread D = new Thread(() -> {
            try {
                System.out.println("D joining A");
                A.join();
                System.out.println("D doing some job");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, "Thread_D");
        A.start();
        D.start();
        D.join();
        A.join();
        System.out.println("Main finished");
    }
}
