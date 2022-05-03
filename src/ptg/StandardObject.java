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

    @Override
    public String toString() {
        return "("+method+","+obj+")";
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof StandardObject) {
            StandardObject o = (StandardObject) other;
            return this.method.equals(o.method) && this.obj.equals(o.obj);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (method.hashCode() + obj.hashCode()) * method.hashCode() + obj.hashCode();
    }
}