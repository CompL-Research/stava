package analyser;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import soot.*;
import main.CHATransform;
import ptg.ObjectNode;
import ptg.ObjectType;
import ptg.PointsToGraph;
import soot.jimple.Constant;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JStaticInvokeExpr;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class MethodsLinkingAnalyser extends SceneTransformer {
    public static ConcurrentHashMap<SootMethod, List<Local> > nativeLocals = new ConcurrentHashMap<>();

	public static ArrayList<String> whitelistedNatives = new ArrayList<> (
		Arrays.asList("<java.lang.System: arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V>")
	);

	public static ArrayList<String> blacklistedNatives = new ArrayList<> (
		Arrays.asList("<sun.misc.Unsafe: putObject(Ljava/lang/Object;JLjava/lang/Object;)V>",
						"<sun.misc.Unsafe: putObjectVolatile(Ljava/lang/Object;JLjava/lang/Object;)V>",
						"<sun.misc.Unsafe: compareAndSwapObject(Ljava/lang/Object;JLjava/lang/Object;Ljava/lang/Object;)Z>")
	);
	

    private Set<SootMethod> methodsProcessed;
    private Set<SootMethod> methodsProcessing;

    public MethodsLinkingAnalyser() {
        methodsProcessed = new HashSet<SootMethod>();
        methodsProcessing = new HashSet<SootMethod>();
    }

    @Override
    protected void internalTransform(String arg0, Map<String, String> arg1) {
        //  Get the call graph, and start processing the methods
        CallGraph cg = Scene.v().getCallGraph();
        SootClass mainClass = Scene.v().getMainClass();
        SootMethod methodCaller = mainClass.getMethodByName("main");
        // Iterator<MethodOrMethodContext> callerEdges = cg.sourceMethods();

        // while (callerEdges.hasNext()) {
        //     SootMethod methodCaller = (SootMethod) callerEdges.next();
        //     System.out.println("PRIYAM method caller: " + methodCaller);
            processMethod(methodCaller);
        // }
    }

    private void processMethod(SootMethod methodCaller) {
        if (methodCaller.isJavaLibraryMethod()) {
            return;
        }

        if (methodsProcessed.contains(methodCaller)) {
            // Method already computed
            // we have the results ready
            return;
        }

        if (methodsProcessing.contains(methodCaller)) {
            // Recursive loop
            // ASK - See what to do
            return;
        }

        methodsProcessing.add(methodCaller);

        System.out.println("PRIYAM Method Name: "+ methodCaller.getBytecodeSignature() + ":" + methodCaller.getName());
        Body body = methodCaller.getActiveBody();
        PatchingChain<Unit> units = body.getUnits();

        for (Unit u : units) {
            // System.out.println("PRIYAM unit: " + u);
            if (u instanceof JInvokeStmt) {
                JInvokeStmt stmt = (JInvokeStmt) u;
                InvokeExpr expr = stmt.getInvokeExpr();
                handleExpr(methodCaller, u, expr);
            }
        }

        methodsProcessing.remove(methodCaller);
        methodsProcessed.add(methodCaller);
    }

    public void handleExpr(
        SootMethod methodCaller, Unit u, InvokeExpr expr) {        
        PointsToGraph ptg = StaticAnalyser.ptgs.get(methodCaller);

        if (ptg == null) {
            // If no points to graph, no need to process further
            return;
        }

        CallGraph cg = Scene.v().getCallGraph();

        Iterator<Edge> iedges = cg.edgesOutOf(u);
        List<Value> args = expr.getArgs();

        List<Edge> edges = new ArrayList<>();
        if (!iedges.hasNext()) {
            iedges = CHATransform.getCHA().edgesOutOf(u);
        }

        while (iedges.hasNext()) {
            edges.add(iedges.next());
        }

        if (edges.size() == 0) {
            // System.out.println("Empty edges: " + expr + ", function incoming edges: " + cg.edgesInto(methodCaller).hasNext() +
            //         " Method: " + methodCaller.getBytecodeSignature());
            edges.add(new Edge(methodCaller, u, expr.getMethod(), Kind.SPECIAL));
        }

        if (expr instanceof InstanceInvokeExpr) {
            // ASK - No need to handle instance invoke expr?
        } else if (expr instanceof JStaticInvokeExpr) {
        } else {
            System.err.println("Unidentified invoke expr: " + expr.toString());
            throw new IllegalArgumentException(expr.toString());
        }

        for (Edge edge : edges) {
            /* 1.
             * We traverse and find the caller callie relationship
             * between ObjectNodes
             */
            SootMethod method = edge.tgt();
            int paramCount = method.getParameterCount();

            // Recursion to first process the method
            // if not already processed
            if (!methodsProcessed.contains(method)) {
                processMethod(method);
            }

            Map<ObjectNode, Set<ObjectNode>> paramMapping = new HashMap<ObjectNode,Set<ObjectNode>>();
            for (int i = 0; i < paramCount; i++) {
                ObjectNode obj = new ObjectNode(i, ObjectType.parameter);
				// ConditionalValue cv = new ConditionalValue(method, obj, true);

                if (edge.kind() == Kind.REFL_INVOKE)
                    paramMapping.put(obj, ptg.vars.get((Local) args.get(1)));
                else if (edge.kind() == Kind.REFL_CONSTR_NEWINSTANCE)
                    paramMapping.put(obj, ptg.vars.get((Local) args.get(0)));
                else {
                    Value arg = args.get(i);
                    if (arg.getType() instanceof RefType || arg.getType() instanceof ArrayType)
                        if (!(arg instanceof Constant)) { // Notice the not(!)
                            // ptg.addParametricEdge((Local) args.get(i), cv);
                            System.out.println("args get i: " + args.get(i));
                            paramMapping.put(obj, ptg.vars.get((Local) args.get(i)));
                        }
                }
            }

            // System.out.println("PRIYAM PARAMS MAPPING:  " + paramMapping);
            
            /* 2.
             * Now, loop in the callie method's ptg to find if there
             * exists any relationship/node between the params
             * If exists, add the realtion for corresponding values in
             * paramsMapping also
             */
            PointsToGraph calliePTG = StaticAnalyser.ptgs.get(method);
            System.out.println("PRIYAM METHOD: " + method);
            System.out.println("PRIYAM PTGS: " + StaticAnalyser.ptgs);
            System.out.println("PRIYAM calliePTG: " + calliePTG);
            // If ptg gives error, ensure StaticAnalysis has been done

            for (int i = 0; i < paramCount; i++) {
                ObjectNode obj = new ObjectNode(i, ObjectType.parameter);
                Map<SootField, Set<ObjectNode>> pointingTo = calliePTG.fields.get(obj);

                if (pointingTo == null) {
                    continue;
                }

                for (Map.Entry<SootField, Set<ObjectNode>> entry : pointingTo.entrySet()) {
                    for (ObjectNode fieldObj : entry.getValue()) {
                        System.out.println("There exists an edge from: " + obj + " to " + fieldObj + " by " + entry.getKey());
                        if (fieldObj.type != ObjectType.parameter) {
                            continue;                            
                        }

                        if (!paramMapping.containsKey(obj) || !paramMapping.containsKey(fieldObj)) {
                            // If paramsMapping does not have the object, it can happen if null is passed
                            continue;
                        }

                        // Find paramsMapping for obj
                        // Find paramsMapping for fieldObj
                        // Add an edge from objs to fieldObjs
                        for (ObjectNode objInCaller : paramMapping.get(obj)) {
                            for (ObjectNode fieldObjInCaller : paramMapping.get(fieldObj)) {
                                System.out.println("There should exists an edge from: " + objInCaller + " to " + fieldObjInCaller + " by " + entry.getKey());
                                ptg.WEAK_makeField(objInCaller, entry.getKey(), fieldObjInCaller);
                            }
                        }                        
                    }
                }
            }

            System.out.println("AFTER PRIYAM PTGS: " + StaticAnalyser.ptgs + "\n");
        }
    }
}
