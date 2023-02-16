package data;

import es.EscapeStatus;
import ptg.*;
import soot.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalAnalysisData {
    public static Map<SootMethod, PointsToGraph> ptgs;
	public static Map<SootMethod, HashMap<ObjectNode, EscapeStatus>> summaries;
	public static Map<SootMethod, ArrayList<ObjectNode>> stackOrders;
	public static LinkedHashMap<Body, Analysis> analysis;
	public static List<SootMethod> noBCIMethods;

	// If the methods have been processed interprocedurally
	public static Set<SootMethod> methodsProcessed;
    public static Set<SootMethod> methodsProcessing;

    public GlobalAnalysisData() {
        analysis = new LinkedHashMap<>();
		ptgs = new ConcurrentHashMap<>();
		summaries = new ConcurrentHashMap<>();
		stackOrders = new ConcurrentHashMap<>();
		noBCIMethods = new ArrayList<>();
		methodsProcessed = new HashSet<SootMethod>();
        methodsProcessing = new HashSet<SootMethod>();
    }
}
