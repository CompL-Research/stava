class Node {
    Node n;
    public Node test() {
        // Node a = new Node();
        // a -> <func, <param,0> >
        Node b = func(new Node());
        /*
         * <external, 2> = [<func,returnVal>]
         */
        return b;
    }
    public Node func(Node a) {
        // if (a == null)
        //     a = new Node();
        return a;
        // ret -> <param,0>, <internal, 1>
    }
}

// check Handling of multi-threaded case
// disconnected cfgs

public class Main{
    
    public static void main(String[] args) {
        Node x = new Node();
        Node y = x.test();
    }
    
}
