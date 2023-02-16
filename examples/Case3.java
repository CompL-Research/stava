package examples;

public class Case3 {
    public static void main(String[] args) {
        Case1 obj = new Case1();
        obj.m1();
    }

    // Consider Both A and B are in their respective function stack
    public void m2() {
        Node A = new Node();
        Node B =  new Node();       // A and B are in the same stack frame
        m3(A, B);
    }

    public void m3(Node A, Node B) {
        A.f = B;        // Object pointed to by B need not escape as B's lifetime is equal to A's
                        // lifetime. So, A and B will be popped out at the same time.
    }
    
    // Object pointed to by B need not be Heapified!
}
