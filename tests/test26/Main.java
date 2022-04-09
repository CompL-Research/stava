public class Main {
    public static void main(String[] args) {
        Node A = new Node();    //O3
        Node B = new Node();    //O4
        foo(A);
        bar(B);
    }
    public static void foo(Node p1){
        Node D = new Node();    //O9
        bar(D);
        p1.n = D; //DANGER
    }
    public static void bar(Node p2) {
        Node E = new Node();    //O14
        p2.n = E;
    }
}