package ptg;

import soot.Unit;
import soot.SootMethod;
import utils.getBCI;

public class StandardObject {
    private SootMethod method;
    private ObjectNode obj;
    
    public StandardObject(SootMethod m, ObjectNode o){
        this.method = m;
        this.obj = o;
    }
    public SootMethod getMethod() {
        return this.method;
    }
    public ObjectNode getObject() {
        return this.obj;
    }
    public String toString() {
        return "("+method+","+obj+")";
    }
}