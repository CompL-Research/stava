package resolver;

import es.*;
import ptg.ObjectNode;
import ptg.ObjectType;
import ptg.PointsToGraph;
import ptg.RetLocal;
import soot.MethodOrMethodContext;
import soot.SootField;
import soot.SootMethod;
import soot.Scene;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.util.*;

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

    List<StandardObject> reverseTopoOrder;
    
    public ReworkedResolver(Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> existingSummaries,
            Map<SootMethod, PointsToGraph> ptgs) {
        this.existingSummaries = existingSummaries;
        this.ptgs = ptgs;
        
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

            for (ObjectNode obj: methodInfo.keySet()) {

                EscapeStatus status = methodInfo.get(obj);
                HashSet<EscapeState> newStates = new HashSet<>();

                for (EscapeState state : status.status ) {
                    if ( state instanceof ConditionalValue) {

                        ConditionalValue cstate = (ConditionalValue)state;
                        if (cstate.method != null || cstate.object.type!=ObjectType.argument) {
                            newStates.add(state);
                            continue;
                        }
                        int parameternumber = cstate.object.ref;
                        if(parameternumber <0) {
                            newStates.add(state);
                            continue;
                        }
                        Iterator<Edge> iter = cg.edgesInto(key);

                        while(iter.hasNext()) {
                            Edge edge = iter.next();
                            System.out.println(key+" "+obj+" "+cstate+" " + +parameternumber + " "+edge.src() );
                            List<ObjectNode> objects = GetObjects(edge.srcUnit(), parameternumber, edge.src());
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

                System.out.println(key+" "+obj+"From: "+ status.status);
                status.status = newStates;
                System.out.println(key+" "+obj+"TO: "+ newStates);

            }
        }
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
        Value arg = expr.getArg(num);
        try {
            for (ObjectNode o: this.ptgs.get(src).vars.get(arg)) {
                objs.add(o);
            }
        }
        catch (Exception e) {
            System.out.println(src+" "+arg);
            throw e;
        }
        System.out.println("Unit: "+u+" : "+objs);
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
        for (SootMethod key: this.existingSummaries.keySet() ) {
            HashMap<ObjectNode, EscapeStatus> methodInfo = this.existingSummaries.get(key);

            for (ObjectNode obj: methodInfo.keySet()) {
                EscapeStatus status = methodInfo.get(obj);
                Set <StandardObject> target = new HashSet<>();
                for (EscapeState state : status.status ) {
                    if ( state instanceof ConditionalValue) {
                        ConditionalValue cstate = (ConditionalValue)state;
                        if ( cstate.object.equals(new ObjectNode(0, ObjectType.returnValue)) 
                            && cstate.method == null) {
                                cstate.method = key;
                        }

                        if ( cstate.method != null || true) {
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
                                System.err.println(cstate.method+" "+cstate.object);
                                System.err.println(e);
                                continue;
                            }
                        }
                    }
                }
                this.graph.put(objMap.get(key).get(obj), target);
            }
        }

        for (StandardObject key: this.graph.keySet()) {
            for (StandardObject val: this.graph.get(key)) {
                if ( ! this.revgraph.containsKey(val))
                    this.revgraph.put(val, new HashSet<>());
                this.revgraph.get(val).add(key);
            }
        }
        printGraph(this.graph);
        printGraph(this.revgraph);
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

        System.out.println("Topo Order: "+this.reverseTopoOrder);
        for (int i= 0 ;i<this.reverseTopoOrder.size();i++) {
            StandardObject u = this.reverseTopoOrder.get(this.reverseTopoOrder.size() -1 -i);
            if (used.containsKey(u) && used.get(u) == true)
                continue;
            List<StandardObject> component = new ArrayList<>();
            dfs2(u, used, component);
            components.add(component);
            System.out.println("Compo:" + component);
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
     *  null. and this code simply ignores such statements.
     * 
     *  Second, return statements should be marked as escaping iff object
     *  returned is allocated in that function.
     */
    void resolve(List<StandardObject> component) {
        List<EscapeState> conds = new ArrayList<>();
        for (StandardObject sobj : component) {
            if (isReturnObject(sobj)) 
            {
                SetComponent(component, Escape.getInstance());
                return;
            }
            for (StandardObject nxt: this.graph.get(sobj)) {
                if (nxt.getMethod().isJavaLibraryMethod())
                    continue;
                
                EscapeStatus es = this.existingSummaries.get(nxt.getMethod()).get(nxt.getObject());
                if (es != null && es.doesEscape()) {
                    SetComponent(component, Escape.getInstance());
                    return;
                }
            }
        }
        SetComponent(component, NoEscape.getInstance());
    }

    void SetComponent ( List<StandardObject> comp, EscapeState es) {
        System.out.println(es);
        for (StandardObject s: comp) {
            if (this.existingSummaries.get(s.getMethod()) != null)
                this.existingSummaries.get(s.getMethod()).put(s.getObject(), new EscapeStatus(es));
        }
    }
}