class Node {
    Node p;
}
public class Main {
    private static Node stNode;
    static final class syncObject { /* empty class for synchronization only. */}
    private static final syncObject accessorMutex = new syncObject();
    public static void main(String[] args) {
        Node a = new Node();
        escapeViaStatic(a);
    }

    private static void escapeViaStatic(Node a) {
        synchronized (accessorMutex) { 
            stNode = a;
        }
    }
}