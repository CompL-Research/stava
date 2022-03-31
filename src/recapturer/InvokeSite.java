package recapturer;

import soot.Unit;
import soot.SootMethod;
import utils.getBCI;

public class InvokeSite {
    private SootMethod method;
    private int site;
    
    public InvokeSite(SootMethod m, Unit u){
        this.method = m;
        this.site = getBCI.get(u);
    }
    public SootMethod getMethod() {
        return this.method;
    }
    public int getSite() {
        return this.site;
    }
    public String toString() {
        return "("+method+", "+site+")";
    }
}