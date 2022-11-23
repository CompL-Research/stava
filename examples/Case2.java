package examples;

public class Case2 {
    public static void main(String[] args) {
        Case1 obj = new Case1();
        obj.m1();
    }

    // Consider Both A and B are in their respective function stack
    public void m1() {
        Node B =  new Node();   // B is in lower function call stackframe
        m2(B);
    }

    public void m2(Node B) {
        Node A = new Node();    // A is in higher function call stackframe
                                // Thus B's lifetime is more than A
        m3(A, B);
    }

    public void m3(Node A, Node B) {
        A.f = B;    // Object pointed to by B need not escape as B's lifetime is more than A's
                    // lifetime. So, if B will not be popped before A in anycase.
    }
    
    // Object pointed to by B need not be Heapified!
}
