public class Node {
	public int a;
	public Node n;
	public Node(int a) {
		this.a = a;
	}
	static Node b;

	public void escapeObject() {
		b = this;
	}
}
