package resolver;

import config.StoreEscape;
import es.*;
import ptg.ObjectNode;
import ptg.ObjectType;
import ptg.PointsToGraph;
import ptg.RetLocal;
import handlers.*;
import soot.MethodOrMethodContext;
import soot.SootField;
import soot.SootMethod;
import soot.Scene;
import soot.Unit;
import soot.Value;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
// import javafx.util.Pair;
import java.util.*;
import java.io.*;
/*
 *
 * Some notes:
 * 1. We just need to mark object in the function returned as escaping and nothing more, if some object is coming from
 *      callee then it should be marked as escaping in the callee. It is not the responsiblity of caller.
 * 
 */

class StandardObject {
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

public class ReworkedResolver{
    public Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> existingSummaries;
    public Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> solvedSummaries;
    public Map<SootMethod, HashMap<ObjectNode, StandardObject>> objMap;
    HashMap<SootMethod, HashMap<ObjectNode, ResolutionStatus>> resolutionStatus;
    Map<SootMethod, PointsToGraph> ptgs;
    Map<StandardObject, Set<StandardObject> > graph;
    Map<StandardObject, Set<StandardObject> > revgraph;

    List<SootMethod> noBCIMethods;

    List<StandardObject> reverseTopoOrder;
    
    public ReworkedResolver(Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> existingSummaries,
            Map<SootMethod, PointsToGraph> ptgs,
            List<SootMethod> escapingMethods) {
        this.existingSummaries = existingSummaries;
        this.ptgs = ptgs;
        this.noBCIMethods = escapingMethods;

        System.out.println(this.existingSummaries);

        for (SootMethod method: this.ptgs.keySet())
        {
            System.out.println(method+": "+this.ptgs.get(method));
        }
    
        this.objMap = new HashMap<> ();
        this.solvedSummaries = new HashMap<> ();
        this.resolutionStatus = new HashMap<> ();

        this.graph = new HashMap<>();
        this.reverseTopoOrder = new ArrayList<>();
        this.revgraph = new HashMap<>();

        for (Map.Entry<SootMethod, HashMap<ObjectNode, EscapeStatus>> entry : existingSummaries.entrySet()) {
			SootMethod method = entry.getKey();
			HashMap<ObjectNode, EscapeStatus> map = entry.getValue();
            HashMap<ObjectNode, ResolutionStatus> q = new HashMap<> ();
            HashMap<ObjectNode, StandardObject> tobj = new HashMap<> ();
			for (Map.Entry<ObjectNode, EscapeStatus> e : map.entrySet()) {
				ObjectNode obj = e.getKey();
                q.put(obj, ResolutionStatus.UnAttempted);
                StandardObject x = new StandardObject(method, obj);
                tobj.put(obj,x);
                this.graph.put(x, new HashSet<>());
                this.revgraph.put(x, new HashSet<>());
			}
            resolutionStatus.put(method, q);
            this.objMap.put(method, tobj);
			this.solvedSummaries.put(method, new HashMap<>());
        }
        /*
         * Next, we traverse all function calls and add mapping from caller to the 
         * objects passed. We are just moving towards inter-procedural resolution :P
         * 
         */
        AddCallerSummaries();
        GenerateGraphFromSummary();
        FindSCC();
        // findCondesedGraph();
    }

    void printGraph(Map<StandardObject, Set<StandardObject> > graph) {
        System.out.println("Printing graph: ");
        for (StandardObject u: graph.keySet()) {
            System.out.print(u+": ");
            for (StandardObject v: graph.get(u)) {
                System.out.print(v+",");
            }
            System.out.println();
        }
        System.out.println();
    }

    // Convert all <caller,<argument,x>> statements to the actual caller functions and replace <argument,x> 
    // to parameter passed.

    void AddCallerSummaries() {
        CallGraph cg = Scene.v().getCallGraph();
        for (SootMethod key: this.existingSummaries.keySet() ) {
            HashMap<ObjectNode, EscapeStatus> methodInfo = this.existingSummaries.get(key);
            HashMap<ObjectNode, EscapeStatus> solvedMethodInfo = this.solvedSummaries.get(key);

            for (ObjectNode obj: methodInfo.keySet()) {

                EscapeStatus status = methodInfo.get(obj);
                HashSet<EscapeState> newStates = new HashSet<>();


                for (EscapeState state : status.status ) {
                    if ( state instanceof ConditionalValue) {

                        ConditionalValue cstate = (ConditionalValue)state;
                        if (//cstate.method != null || 
                            cstate.object.type!=ObjectType.argument) {
                            newStates.add(state);
                            continue;
                        }
                        int parameternumber = cstate.object.ref;

                        /*
                         *  ALERT: XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
                         *  This is probably incorrect, but this works when Stores are marked as escaping.
                         *  If stores are marked as escaping, it doesn't matter, if objects are stored in
                         *  fields of parameters, they will anyway be marked as escaping. It is quite pos-
                         *  -sible that I am missing something.
                         */


                        if (StoreEscape.MarkStoreEscaping && StoreEscape.ReduceParamDependence) {
                            newStates.add(state);
                            continue;
                        }
                        if(parameternumber <0) {
                            newStates.add(state);
                            continue;
                        }
                        Iterator<Edge> iter = cg.edgesInto(key);
                        // System.out.println("isempty:"+iter.hasNext()+": "+key);
                        newStates.add(state);
                        while(iter.hasNext()) {
                            parameternumber = cstate.object.ref;
                            Edge edge = iter.next();
                            // System.out.println(key+" "+obj+" "+cstate+" " + +parameternumber + " "+edge.src() );
                            // System.out.println("Edge type:" + edge.kind() + " " + key+ " "+edge.srcUnit()+" "+edge.src());
                            if (parameternumber >= 0) {
                                if (edge.kind() == Kind.REFL_CONSTR_NEWINSTANCE){
                                    parameternumber = 0;
                                }
                                
                                else if (edge.kind() == Kind.REFL_INVOKE){
                                    parameternumber = 1;
                                }
                            }
                            List<ObjectNode> objects;
                            try {
                                // if (parameternumber >=0 )
                                    objects = GetObjects(edge.srcUnit(), parameternumber, edge.src());
                                // else 
                                //     objects = GetBaseObjects(edge.srcUnit(), parameternumber, edge.src());
                            } catch (Exception e) {
                                System.err.println("Cond: "+cstate+ " "+cstate.object+" "+cstate.object.ref+" "+parameternumber);
                                throw e;
                            }
                            if (objects == null) {
                                // Do we need to do something, if we cannot find any objects here?
                                System.err.println("Objects are null!.");
                            }
                            else
                                for (ObjectNode x: objects) {
                                    newStates.add(CreateNewEscapeState(x, cstate, edge.src())); 
                                }
                        }

                    }
                    else {
                        newStates.add(state);
                        if(state instanceof Escape) {
                            this.solvedSummaries.get(key).put(obj, new EscapeStatus(Escape.getInstance()));
                        }
                    }
                }

                // System.out.println(key+" "+obj+"From: "+ status.status);
                // status.status = newStates;
                solvedMethodInfo.put(obj, new EscapeStatus());
                solvedMethodInfo.get(obj).status = newStates;
                // System.out.println(key+" "+obj+"TO: "+ newStates);

            }
        }
        System.out.println(this.solvedSummaries);
    }

    EscapeState CreateNewEscapeState(ObjectNode obj, ConditionalValue state, SootMethod src) {
        return new ConditionalValue(src, obj, state.fieldList, state.isReal);
    }

    List<ObjectNode> GetObjects(Unit u, int num, SootMethod src) {
        List<ObjectNode> objs = new ArrayList<>();
        InvokeExpr expr;
        if ( u instanceof JInvokeStmt) {
            expr = ((JInvokeStmt)u).getInvokeExpr();
        }
        else if ( u instanceof JAssignStmt) {
            expr = (InvokeExpr)(((JAssignStmt)u).getRightOp());
        }
        else {
            System.out.println(u);
            return null;
        }
        Value arg;
        try {
            if (num >=0 )
                arg = expr.getArg(num);
            else if (num == -1 && (expr instanceof AbstractInstanceInvokeExpr))
                arg = ((AbstractInstanceInvokeExpr)expr).getBase();
            else return null;

        }
        catch (Exception e) {
            System.err.println(u + " "+num+" "+expr);
            CallGraph cg = Scene.v().getCallGraph();
            Iterator<Edge> iter = cg.edgesOutOf(u);
            while(iter.hasNext()) {
                Edge edg = iter.next();
                System.err.println("EXT: "+edg.tgt()+" "+edg.kind());
            }
            throw e;
        }
        if (! (arg instanceof Local))
            return objs;
        else if ( ((Local)arg).getType() instanceof PrimType )
            return objs;
        try {
            for (ObjectNode o: this.ptgs.get(src).vars.get(arg)) {
                objs.add(o);
            }
        }
        catch (Exception e) {
            System.err.println(src+" "+arg+" "+u+" "+num);
            System.err.println(e);
            return null;
        }
        // System.out.println("Unit: "+u+" : "+objs);
        return objs;
    }

    StandardObject getSObj(SootMethod method, ObjectNode obj) {
        if (objMap.get(method) == null) {
            objMap.put(method,new HashMap<>());
        }
        StandardObject objx = objMap.get(method).get(obj);
        if (objx == null)
            objMap.get(method).put(obj, new StandardObject(method, obj));

        return objMap.get(method).get(obj);
    }
    void GenerateGraphFromSummary() {
        for (SootMethod key: this.solvedSummaries.keySet() ) {
            HashMap<ObjectNode, EscapeStatus> methodInfo = this.solvedSummaries.get(key);

            for (ObjectNode obj: methodInfo.keySet()) {
                EscapeStatus status = methodInfo.get(obj);
                Set <StandardObject> target = new HashSet<>();
                for (EscapeState state : status.status ) {
                    if ( state instanceof ConditionalValue) {
                        ConditionalValue cstate = (ConditionalValue)state;
                        // if ( cstate.object.equals(new ObjectNode(0, ObjectType.returnValue)) 
                        //     && cstate.method == null) {
                        //         cstate.method = key;
                        // }

                        if ( cstate.method != null ) {
                            try {
                                // StandardObject objx = getSObj(cstate.method,cstate.object);
                                // target.add(objx);
                                // Actually figure out, if above code is sufficient or not.
                                // If not, use the below one.
                                // getObjs() actually find all the objects pointed by obj along with
                                // its fields of obj pointed by conditional value.
                                Set<StandardObject> objx = getObjs(cstate);
                                target.addAll(objx);
                                for (StandardObject x: objx) {
                                    if (this.graph.get(x) == null) {
                                        this.graph.put(x, new HashSet<>());
                                    }
                                }
                            } catch(Exception e) {
                                // System.err.println(cstate.method+" "+cstate.object);
                                // System.err.println(e);
                                continue;
                            }
                        }
                        else {
                            // System.err.println(key+" "+obj+" Method NULL: "+cstate);
                        }
                    }
                }
                this.graph.put(objMap.get(key).get(obj), target);
            }
        }

        if (StoreEscape.MarkParamReturnEscaping == false)
        for (StandardObject srcobj: this.graph.keySet()) {
            if (srcobj.getObject().type == ObjectType.external)
                for (StandardObject tgtobj: this.graph.get(srcobj)) {
                    if (isReturnedFromDifferentFunction(srcobj, tgtobj)) {
                        HashSet<ObjectNode> retObjs = JReturnStmtHandler.returnedObjects.get(tgtobj.getMethod());
                        if (retObjs == null) continue;
                        for (ObjectNode retobj: retObjs) {
                            this.graph.get(objMap.get(tgtobj.getMethod()).get(retobj)).add(srcobj);
                        } 
                        // this.graph.get(tgtobj).add(srcobj);
                    }
                }
        }

        for (SootMethod method: this.ptgs.keySet()) {
            Map<ObjectNode, Map<SootField, Set<ObjectNode>>> fieldMap = this.ptgs.get(method).fields;
            for (ObjectNode obj: fieldMap.keySet()) {
                StandardObject sobj = getSObj(method, obj);

                for (SootField field: fieldMap.get(obj).keySet()) {
                    for (ObjectNode tobj: fieldMap.get(obj).get(field)) {
                        StandardObject tsobj = getSObj(method, tobj);
                        this.graph.get(tsobj).add(sobj);
                    }
                }
            }
        }
        /*
         *  Find the objects passed and the respective dummy nodes, and match every field.
         * 
         */
        List<StandardObject[] > toAlter = new ArrayList<>();
        for (StandardObject obj1: this.graph.keySet()) {
            for (StandardObject obj2: this.graph.get(obj1)) {
                if (obj2.getObject().type == ObjectType.parameter) {
                    toAlter.add(new StandardObject[]{obj1, obj2});
                }
            }
        }

        for (StandardObject[] obj :toAlter)
        {
            matchObjs(obj[0], obj[1]);
        }

        for (StandardObject key: this.graph.keySet()) {
            for (StandardObject val: this.graph.get(key)) {
                if ( ! this.revgraph.containsKey(val))
                    this.revgraph.put(val, new HashSet<>());
                this.revgraph.get(val).add(key);
            }
        }
        printGraph(this.graph);
        // printGraph(this.revgraph);
    }

    private void matchObjs(StandardObject obj1, StandardObject obj2) {
        try {
            Map<SootField, Set<ObjectNode> > fieldMap1 = this.ptgs.get(obj1.getMethod()).fields.get(obj1.getObject()); // fieldMap of obj1.
            Map<SootField, Set<ObjectNode> > fieldMap2 = this.ptgs.get(obj2.getMethod()).fields.get(obj2.getObject()); // fieldMap of obj1.
            
            if (fieldMap1 == null || fieldMap2 == null)
                return;


            for (SootField f: fieldMap1.keySet() )
            {
                if (fieldMap1.get(f) == null || fieldMap2.get(f) == null) {
                    continue;
                }
                for (ObjectNode o1s: fieldMap1.get(f)) {
                    for (ObjectNode o2s: fieldMap2.get(f)) {
                        StandardObject sobj1 = getSObj(obj1.getMethod(), o1s);
                        StandardObject sobj2 = getSObj(obj2.getMethod(), o2s);
                        // System.err.println(sobj1+"-><-"+sobj2);
                        if(this.graph.get(sobj1).contains(sobj2) && this.graph.get(sobj2).contains(sobj1))
                            continue;
                        else {
                            if (! this.graph.get(sobj1).contains(sobj2))
                                this.graph.get(sobj1).add(sobj2);
                            if (! this.graph.get(sobj2).contains(sobj1))
                                this.graph.get(sobj2).add(sobj1);
                            matchObjs(sobj1, sobj2);
                        }
                    }
                }
            }
        }
        catch(Exception e) {
            System.err.println(e);
            System.out.println(obj1+" "+obj2);
        }
        
    }

    private Set<StandardObject> getObjs(ConditionalValue cv) {
		Iterable<ObjectNode> _ret = new LinkedHashSet<ObjectNode>();
		Collection<ObjectNode> c = (Collection<ObjectNode>) _ret;
		// <m, <parameter,0>.f.g> or <m,<returnValue,0>.f.g>
		LinkedList<ObjectNode> workList = new LinkedList<ObjectNode>();
		PointsToGraph ptg;
		ptg = this.ptgs.get(cv.getMethod());
		if (ptg == null || cv.object.equals(new ObjectNode(0, ObjectType.returnValue))) {
//			System.out.println("the method of "+ cv.toString() + " doesn't have a ptg defined!");
            HashSet<StandardObject> x = new HashSet<>();
            x.add(getSObj(cv.getMethod(), cv.object));
            return x;
//			throw new IllegalArgumentException("the method of "+ cv.toString() + " doesn't have a ptg defined!");
		}
		// if (cv.object.equals(new ObjectNode(0, ObjectType.returnValue))) {
		// 	if (ptg.vars.get(RetLocal.getInstance()) != null)
		// 		c.addAll(ptg.vars.get(RetLocal.getInstance()));
		// } else {
		// 	c.add(cv.object);
        // }
        c.add (cv.object);

        workList.addAll(c);

		LinkedList<ObjectNode> temp;
		LinkedList<ObjectNode> workListNext = new LinkedList<ObjectNode>();
		if (cv.fieldList != null) {
            Iterator<SootField> i = cv.fieldList.iterator();
			while (i.hasNext()) {
				SootField f = i.next();
				Iterator<ObjectNode> itr = workList.iterator();
				while (itr.hasNext()) {
					ObjectNode o = itr.next();
					if (ptg.fields.containsKey(o) && ptg.fields.get(o).containsKey(f)) {
						for (ObjectNode obj : ptg.fields.get(o).get(f)) {
							if (!c.contains(obj)) c.add(obj);
						}
						workListNext.addAll(ptg.fields.get(o).get(f));
					}
				}
				workList.clear();
				temp = workListNext;
				workListNext = workList;
				workList = temp;
			}
		}
        // return _ret;
        Set<StandardObject> fnobjs = new HashSet<>();
        for (ObjectNode x: c) {
            fnobjs.add(getSObj(cv.getMethod(), x));
        }
        return fnobjs;
	}

    void FindSCC() {
        HashMap<StandardObject, Boolean> used = new HashMap<>();
        List<List<StandardObject> > components = new ArrayList<>();

        for (StandardObject u: this.graph.keySet()) {
            if (used.containsKey(u) && used.get(u) == true)
                continue;
            dfs1(u, used);
        }

        used = new HashMap<>();

        // System.out.println("Topo Order: "+this.reverseTopoOrder);
        for (int i= 0 ;i<this.reverseTopoOrder.size();i++) {
            StandardObject u = this.reverseTopoOrder.get(this.reverseTopoOrder.size() -1 -i);
            if (used.containsKey(u) && used.get(u) == true)
                continue;
            List<StandardObject> component = new ArrayList<>();
            dfs2(u, used, component);
            components.add(component);
            // System.out.println("Compo:" + component);
            resolve(component);
        }
    }

    void dfs1 (StandardObject u, HashMap<StandardObject, Boolean> used) {
        used.put(u, true);
        if (this.revgraph.get(u) != null) {    
            for (StandardObject v: this.revgraph.get(u)) {
                if (used.containsKey(v) && used.get(v) == true)
                    continue;
                dfs1(v,used);
            }
        }
        this.reverseTopoOrder.add(u);
    }


    void dfs2 (StandardObject u, HashMap<StandardObject, Boolean> used, List<StandardObject> component) {
        used.put(u, true);
        component.add(u);
        
        if (this.graph.get(u) != null) {
            for (StandardObject v: this.graph.get(u)) {
                if (used.containsKey(v) && used.get(v) == true)
                    continue;
                dfs2(v,used,component);
            }
        }
    }

    boolean isReturnObject(StandardObject s) {
        if (s.getObject().equals(new ObjectNode(0, ObjectType.returnValue)))
            return true;
        return false;
    }
    /*
     *  Two issues: First return statements have a function name as
     *  null. and this code simply ignores such statements. - Resolved
     * 
     *  How to check code with mutiple return statements.
     * 
     *  Second, return statements should be marked as escaping iff object
     *  returned is allocated in that function.
     */
    boolean isEscapingObject( StandardObject sobj) {

        HashMap<ObjectNode, EscapeStatus> ess = this.solvedSummaries.get(sobj.getMethod());
        // System.err.println("isEscaping Object: "+ess+" Object: "+sobj);
        if (ess == null)
            return false;

        if (this.noBCIMethods.contains(sobj.getMethod())) {
            return true;
        }

        EscapeStatus es = ess.get(sobj.getObject());
        // System.err.println("isEscaping Object: "+es+" Object: "+sobj);
        if (es != null && es.doesEscape()) {
            // System.err.println("es is escaping.");
            // SetComponent(component, Escape.getInstance());
            return true;
        }
        return isAssignedToThis(sobj);
    }

    boolean isReturnedFromDifferentFunction(StandardObject sobj, StandardObject nxt) {
        if (this.noBCIMethods.contains(nxt.getMethod())) {
            return false;
        }
        if (sobj.getMethod() != nxt.getMethod()) {
            if (isReturnObject(nxt))
                return true;
        }
        return false;
    }

    boolean isAssignedToThis(StandardObject sobj) { // Is assigned to this or parameter.
        HashMap<ObjectNode, EscapeStatus> objEs = this.solvedSummaries.get(sobj.getMethod());
        if (objEs == null)
            return false;
        EscapeStatus es = objEs.get(sobj.getObject());

        if (es == null)
            return false;

        if (sobj.getObject().type == ObjectType.parameter || sobj.getObject().type == ObjectType.argument)
            return false;

        // if (sobj.getObject().type != ObjectType.internal)
        //     return false;
        
        // If any internal object is assigned to the parameter then it is escaping.
        // Whereas, same argument is not valid for external object,
        // But if any external object is assigned to static variable, 
        // then it is escaping.


        if (sobj.getObject().type == ObjectType.internal) {
            for (EscapeState e : es.status) {
                if (e instanceof ConditionalValue) {
                    ConditionalValue cv = (ConditionalValue) e;
                    if (cv.method == null && cv.object.type == ObjectType.argument )//&& cv.object.ref == -1)
                        return true;
                }
            }
        }
        else {
            for (EscapeState e : es.status) {
                if (e instanceof ConditionalValue) {
                    ConditionalValue cv = (ConditionalValue) e;
                    if (cv.method == null && cv.object.type == ObjectType.argument && cv.object.ref == -1)
                        return true;
                }
            }
        }
        return false;
    }

    boolean isEscapingParam(StandardObject sobj) {
        for (StandardObject nxt: this.graph.get(sobj)) {
            if (isReturnObject(nxt))
                continue;
            if (isEscapingObject(nxt) )
                return true;
        }
        return false;
    }

    void resolve(List<StandardObject> component) {
        List<EscapeState> conds = new ArrayList<>();
        for (StandardObject sobj : component) {
            // try {
            //     System.err.println(sobj.getMethod()+" "+sobj.getObject());
            //     System.err.println(this.graph.get(sobj));
            //     System.err.println(" "+this.solvedSummaries.get(sobj.getMethod()).get(sobj.getObject()));
            // }
            // catch (Exception e) {
            //     System.err.println("Error");
            // }
            if (isReturnObject(sobj))
            {
                // for (StandardObject obj: component) {
                //     if (ofSameMethod(obj, sobj))
                //         markObjectAsEscaping(obj);
                // }
                System.err.println("Identified as return obj: "+sobj);
                SetComponent(component, Escape.getInstance());
                return;
            }
            if (isEscapingObject(sobj)) {
                System.err.println("Identified as escaping obj: "+sobj);
                SetComponent(component, Escape.getInstance());
                return;
            }
            // if (isAssignedToThis(sobj)) {
            //     SetComponent(component, Escape.getInstance());
            // }
            // System.err.println("SBOJ: "+sobj+" Dependencies: "+this.graph.get(sobj));
            if (StoreEscape.MarkParamReturnEscaping == false)
                if (sobj.getObject().type == ObjectType.parameter) {
                    if (isEscapingParam(sobj)) {
                        System.err.println("Identified as escaping param: "+sobj);
                        SetComponent(component, Escape.getInstance());
                        return;
                    }
                    continue;
                }

            for (StandardObject nxt: this.graph.get(sobj)) {
                // System.err.println("NEXT: "+nxt);
                // if (nxt.getMethod().isJavaLibraryMethod()){
                //     System.err.println("Escaping obj: "+nxt);
                //     SetComponent(component, Escape.getInstance());
                //     return;
                //     // continue;
                // }
                try {
                    if (StoreEscape.MarkParamReturnEscaping == false)
                        if (isReturnedFromDifferentFunction(sobj, nxt) ) {
                            System.err.println("Returned from different func: "+ sobj);
                            continue;
                        }
                    if (isEscapingObject(nxt)) {
                        System.err.println("Escaping obj: "+nxt);
                        SetComponent(component, Escape.getInstance());
                        return;
                    }
                }
                catch(Exception e) {
                    // System.err.println(this.solvedSummaries.get(nxt.getMethod())+" "+nxt.getMethod()+" "+nxt.getObject());
                    // System.err.println(e);
                    // throw e;
                }
                
            }
        }
        SetComponent(component, NoEscape.getInstance());
    }

    void SetComponent ( List<StandardObject> comp, EscapeState es) {
        System.err.println("comp:"+comp+" : "+es);
        for (StandardObject s: comp) {
            if (this.solvedSummaries.get(s.getMethod()) != null)
                this.solvedSummaries.get(s.getMethod()).put(s.getObject(), new EscapeStatus(es));
        }
    }
}