class Node {
	public Node n;
}

class C1 {
    void foo() {
        Node A = new Node();
        bar(A);
    }
    Node bar (Node p) {
        return p;
    }
}


public class Main {
    public static void main(String[] args) {
        C1 c = new C1();
        c.foo();
    }    
}
