class Node {
    Node n;
}

class C1 {
    Node n;
    Node f;
    public Node test() {
		Node A = new Node();
		Node B = new Node();
		Node C = new Node();
		Node D = new Node();
        func(A,B);
        // bar(A);
		return new Node();
    }
    void bar(Node p1) {
        func(p1, new Node());
    }
	public void func(Node p1, Node p2) {
        // p1.n = p2;
        this.n = p2;
        this.f = new Node();
	}
}

public class Main {
	public static void main(String[] args) {
        C1 c = new C1();
		c.test();
	}
}
