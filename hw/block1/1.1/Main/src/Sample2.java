import javax.naming.ldap.ExtendedRequest;
import javax.swing.plaf.synth.SynthRadioButtonMenuItemUI;

public class Sample2 {
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
            Thread C = new Thread(()-> {
                System.out.println("C joins B");
                try {
                    B.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }, "Thread_C");
            C.start();
            try {
                System.out.println("A joins C");
                C.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }, "Thread_A");

        A.start();
        A.join();
        System.out.println("Main finished");
    }
}