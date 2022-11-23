package examples;

public class Case1 {
    public static void main(String[] args) {
        Case1 obj = new Case1();
        obj.m1();
    }

    // Consider Both A and B are in their respective function stack
    public void m1() {
        Node A =  new Node();   // A is in lower function call stackframe
        m2(A);
    }

    public void m2(Node A) {
        Node B = new Node();    // B is in higher function call stackframe
                                // Thus B's lifetime is less than A
        m3(A, B);
    }

    public void m3(Node A, Node B) {
        A.f = B;    // Object pointed to by B must escape as B's lifetime is less than A's
                    // lifetime Else, A.f will become undefined, and will not point to any
                    // object once m2's function stack is poped out.
    }
    
    // Object pointed to by B should be Heapified!
}
