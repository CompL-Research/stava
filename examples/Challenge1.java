package examples;

public class Challenge1 {
    public static void main(String[] args) {
        Challenge1 obj = new Challenge1();
        obj.m1();        
    }

    public void m1() {
        int x = 3;
        Node A = new Node();
        Node B = new Node();

        m2(A, B, x);
    }

    public void m2(Node p1, Node p2, int x) {
        if (x == 0) return;

        m3(p1, p2, x - 1);
        p1.f = p2;
    }

    public void m3(Node p1, Node p2, int x) {
        Node C = new Node();
        Node D = new Node();

        m2(C, D, x);
    }
}
