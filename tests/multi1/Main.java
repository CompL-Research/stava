
class Test{
    public void func() {
        bar();
    }
    public void bar() {
        System.out.println("Bar called");
    }
}

class Multithreading extends Thread {
    public void run()
    {
        try {
            // Displaying the thread that is running
            System.out.println(
                "Thread " + Thread.currentThread().getId()
                + " is running");
            Test t = new Test();
            t.func();
        }
        catch (Exception e) {
            // Throwing an exception
            System.out.println("Exception is caught");
        }
    }
}

public class Main{
    public static void main(String[] args) {
        Multithreading mt = new Multithreading();
        mt.start();
        try { 
            mt.join();
        } catch (Exception e) {
            
        }
    }
}