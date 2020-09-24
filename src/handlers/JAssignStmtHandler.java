package handlers;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import es.ConditionalValue;
import es.Escape;
import es.EscapeStatus;
import es.NoEscape;
//import exceptions.AnalyserPanicException;
import ptg.ArrayField;
import ptg.InvalidBCIObjectNode;
import ptg.ObjectFactory;
import ptg.ObjectNode;
import ptg.ObjectType;
import ptg.PointsToGraph;
import soot.Local;
import soot.PrimType;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.ClassConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.NullConstant;
import soot.jimple.StaticFieldRef;
import soot.jimple.StringConstant;
import soot.jimple.internal.AbstractInvokeExpr;
import soot.jimple.internal.JArrayRef;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JCastExpr;
import soot.jimple.internal.JInstanceFieldRef;
import soot.jimple.internal.JNewArrayExpr;
import soot.jimple.internal.JNewExpr;
import soot.jimple.internal.JNewMultiArrayExpr;
import utils.IsMultiThreadedClass;
import utils.getBCI;

public class JAssignStmtHandler{
	public static void handle(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		{
			/*
			 * JAssignStmt Example:
			 * $r2 = new java.lang.String
			 * It has an lhs and rhs. The changes will be determined by
			 * what exactly lhs and rhs are.
			 */
			JAssignStmt stmt = (JAssignStmt) u;
			Value lhs = stmt.getLeftOp();
			Value rhs = stmt.getRightOp();
			if(lhs.getType() instanceof PrimType) {
				if(rhs instanceof InvokeExpr) {
					JInvokeStmtHandler.handleExpr((InvokeExpr)rhs, ptg, summary);
				}
				return;
			} else if(lhs instanceof Local) {
				lhsIsLocal(rhs, u, ptg, summary);
			} else if(lhs instanceof JInstanceFieldRef) {
				lhsIsJInstanceFieldRef(rhs, u, ptg, summary);
			} else if(lhs instanceof JArrayRef) {
				// arr[0]
				if(rhs instanceof StringConstant) {
					// "this is a string constant"
					storeStringConstantToArrayRefStmt(u, ptg, summary);					
				} else if(rhs instanceof ClassConstant) {
					// Integer
					storeClassConstantToArrayRef(u, ptg, summary);
				} else if(rhs instanceof Local) {
					// r0
					lhsArrayRef(u, ptg, summary);
				} else if(rhs instanceof NullConstant) {
					// null
					// nothing to do here!
				} else error(u);
			} else if(lhs instanceof StaticFieldRef) {
				// staticClass.field
				if(rhs instanceof StringConstant || rhs instanceof NullConstant) {
					// Nothing to do!
				} else if(rhs instanceof Local) {
					//
					StaticStoreStmt(u, ptg, summary);
				} else error(u);
			} else {
				error(u);
			}
		}		
	}
	
	private static void lhsIsLocal(Value rhs, Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		if(rhs instanceof JNewExpr) {
			// new String()
			JNewStmt(u, ptg, summary);
		} else if(rhs instanceof JNewArrayExpr || rhs instanceof JNewMultiArrayExpr) {
			// new Integer()[5] or new Integer()[10][2]
			JNewArrayStmt(u, ptg, summary);
		} else if(rhs instanceof NullConstant) {
			// null
			EraseStmt(u, ptg, summary);
		} else if(rhs instanceof Local) {
			// r0
			CopyStmt(u, ptg, summary);
		} else if(rhs instanceof JInstanceFieldRef) {
			// ro.field
			LoadStmt(u, ptg, summary);
		} else if(rhs instanceof StaticFieldRef) {
			// System.out
			StaticLoadStmt(u, ptg, summary);
		} else if(rhs instanceof InvokeExpr) {
			// foo()
			InvokeExpr(u, ptg, summary);
		} else if(rhs instanceof JArrayRef) {
			// args[0]
			rhsArrayRef(u, ptg, summary);
		} else if(rhs instanceof JCastExpr) {
			// (Set<ObjectNode>) new HashSet<ObjectNode>()
			rhsCastExpr(u, ptg, summary);
		} else if(rhs instanceof StringConstant || rhs instanceof ClassConstant) {
			// "hello" or Integer
			storeConstantToLocal(u, ptg, summary);
		} else error(u);
	}
	
	private static void lhsIsJInstanceFieldRef(Value rhs, Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		if(rhs instanceof StringConstant) {
			// "this is a string constant"
			storeStringConstantToInstanceFieldRefStmt(u, ptg, summary);					
		} else if(rhs instanceof NullConstant) {
			// null
//			eraseFieldRefStmt(u, ptg, summary);
		} else if(rhs instanceof Local) {
			// r0
			StoreStmt(u, ptg, summary);
		} else error(u);		
	}
	
	private static void storeConstantToLocal(Unit u, PointsToGraph ptg,
			HashMap<ObjectNode, EscapeStatus> summary) {
		// TODO: ObjectFactory no longer has responsibility to figure out ObjectType
		ObjectNode obj = ObjectFactory.getObj(u);
		if(obj.type!=ObjectType.internal) {
			throw new IllegalArgumentException("Object received from factory is not of required type: internal");			
		}
		Local lhs = (Local)((JAssignStmt)u).getLeftOp();
		ptg.addVar(lhs, obj);
		EscapeStatus es = new EscapeStatus(Escape.getInstance());
		if(obj instanceof InvalidBCIObjectNode) es.setEscape();
		summary.put(obj, es);		
	}
	
	private static void storeStringConstantToInstanceFieldRefStmt(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		JInstanceFieldRef lhs = (JInstanceFieldRef)((JAssignStmt)u).getLeftOp();
		ObjectNode obj = ObjectFactory.getObj(u);
		if(obj.type!=ObjectType.internal) {
			System.out.println("At unit:"+u);
			throw new IllegalArgumentException("Object received from factory is not of required type: internal");
		}
		ptg.makeField((Local)lhs.getBase(), lhs.getField(), obj);
		EscapeStatus es = new EscapeStatus();
		if(obj instanceof InvalidBCIObjectNode) es.setEscape();
		ptg.vars.get((Local)lhs.getBase()).forEach(parent -> es.addEscapeStatus(summary.get(parent)));
		summary.put(obj, es);
	}

	private static void storeStringConstantToArrayRefStmt(Unit u, PointsToGraph ptg,
			HashMap<ObjectNode, EscapeStatus> summary) {
		JArrayRef lhs = (JArrayRef)((JAssignStmt)u).getLeftOp();
		ObjectNode obj = ObjectFactory.getObj(u);
		if(obj.type!=ObjectType.internal) {
			throw new IllegalArgumentException("Object received from factory is not of required type: internal");
		}
		ptg.storeStmtArrayRef((Local)lhs.getBase(), obj);
		EscapeStatus es = new EscapeStatus();
		if(obj instanceof InvalidBCIObjectNode) es.setEscape();
		ptg.vars.get((Local)lhs.getBase()).forEach(parent -> es.addEscapeStatus(summary.get(parent)));
		summary.put(obj, es);
	}

	private static void storeClassConstantToArrayRef(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		JArrayRef lhs = (JArrayRef)((JAssignStmt)u).getLeftOp();
		ObjectNode obj = ObjectFactory.getObj(u);
		if(obj.type!=ObjectType.internal) {
			System.out.println("at unit:"+u);
			throw new IllegalArgumentException("Object received from factory is not of required type: external");
		}
		ptg.storeStmtArrayRef((Local)lhs.getBase(), obj);
		summary.put(obj, new EscapeStatus(Escape.getInstance()));
	}

	private static void eraseFieldRefStmt(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		JInstanceFieldRef lhs = (JInstanceFieldRef)((JAssignStmt)u).getLeftOp();
		if(!ptg.vars.containsKey((Local)lhs.getBase())) return;
		ptg.vars.get((Local)lhs.getBase()).forEach(obj -> {
			if(ptg.fields.containsKey(obj)) {
				Map<SootField, Set<ObjectNode>> map = ptg.fields.get(obj);
				if(map.containsKey(lhs.getField())) {
					map.get(lhs.getField()).clear();
					map.remove(lhs.getField());
				}
			}
		});
	}

	private static void rhsCastExpr(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		// TODO: put a check for cast to a runnable class.
		// a = (Cast?)b
		Value op = ((JCastExpr)((JAssignStmt)u).getRightOp()).getOp();
		// a = (Cast) null;
		if(op instanceof NullConstant) return;
		Local rhs;
		try {
			rhs = (Local)op;
		} catch (Exception e) {
			System.out.println("Unable to cast rhs to Local at: "+u.toString());
			throw e;
		}
		CopyStmtHelper(u, (Local)((JAssignStmt)u).getLeftOp(), rhs, ptg, summary);
	}

	private static void CopyStmt(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		Local lhs = (Local)((JAssignStmt)u).getLeftOp();
		Local rhs = (Local)((JAssignStmt)u).getRightOp();
		CopyStmtHelper(u, lhs, rhs, ptg, summary);
	}
	
	private static void CopyStmtHelper(Unit u, Local lhs, Local rhs, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary){
		Set<ObjectNode> ptSet = null;
		if(ptg.vars.containsKey(rhs)) {
			ptSet = (Set<ObjectNode>)((HashSet<ObjectNode>)ptg.vars.get(rhs)).clone();			
		} else {
			// rhs is a field variable
			// rhs could be null
			System.out.println("[Copystmthelper] "+u.toString());
			System.out.println("[Copystmthelper] "+ptg.toString());
			throw new IllegalArgumentException("");
//			ObjectNode obj = new ObjectNode(utils.getBCI.get(u), ObjectType.external);
//			ptSet = new HashSet<ObjectNode>();
//			summary.put(obj, new EscapeStatus(Escape.getInstance()));
		}
		if(ptg.vars.containsKey(lhs)) {
			ptg.vars.get(lhs).addAll(ptSet);
		} else {
			ptg.vars.put(lhs, ptSet);					
		}
	}

	/*
	 * Undoubtedly a new object creation. Object will be internal and ES will be does not escape.
	 */
	public static void JNewStmt(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		
		// check if rhs is runnable
		Value rhs = ((JAssignStmt) u).getRightOp();
		Value lhs = ((JAssignStmt) u).getLeftOp();
		EscapeStatus es;
		if(IsMultiThreadedClass.check(((JNewExpr)rhs).getBaseType().getSootClass())){
			es = new EscapeStatus(Escape.getInstance());
		} else {
			es = new EscapeStatus(NoEscape.getInstance());
		}
		
		ObjectNode obj;
		try {
			obj = new ObjectNode(getBCI.get(u), ObjectType.internal);
		} catch (Exception e1) {
			obj = InvalidBCIObjectNode.getInstance(ObjectType.internal);
			es.setEscape();
		}
		if(ptg.vars.containsKey(lhs)) {
			ptg.vars.get(lhs).add(obj);
		} else {
			try {
				ptg.forcePutVar((Local)lhs, obj);
			} catch (Exception e) {
				System.out.println(lhs+" may not be a local. Typecast must have failed!");
				throw new InvalidParameterException(lhs.toString()+" may not be a local. Typecast must have failed!");
			}			
		}
		if(!summary.containsKey(obj))summary.put(obj, es);
	}

	private static void JNewArrayStmt(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		// check if rhs is runnable
//		Value rhs = ((JAssignStmt) u).getRightOp();
		Value lhs = ((JAssignStmt) u).getLeftOp();
		EscapeStatus es;
		es = new EscapeStatus(NoEscape.getInstance());
		ObjectNode obj = new ObjectNode(getBCI.get(u), ObjectType.internal);
		try {
			ptg.forcePutVar((Local)lhs, obj);
		} catch (Exception e) {
			System.out.println(lhs+" may not be a local. Typecast must have failed!");
			throw new InvalidParameterException(lhs.toString()+" may not be a local. Typecast must have failed!");
		}
		if(!summary.containsKey(obj))summary.put(obj, es);
	}

	private static void lhsArrayRef(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		JArrayRef lhs = (JArrayRef)((JAssignStmt)u).getLeftOp();
		Local rhs = (Local)((JAssignStmt)u).getRightOp();
		
		try {
			ptg.storeStmtArrayRef((Local)lhs.getBase(), rhs);
			ptg.propagateES((Local)lhs.getBase(), rhs, summary);
		} catch (Exception e) {
			System.out.println(e);
			throw new IllegalArgumentException(lhs.getBase().toString()+" probably could not be cast to Local at "+ u.toString());
		}
	}
	
	/*
	 * Warning: This implementation might add 2 objects for an lhs based on the
	 * ObjectType of the base, as in a case where the points-to set of the 
	 * base containing objects of both internal and external type.
	 */
	private static void rhsArrayRef(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {		
		Local lhs = (Local)((JAssignStmt)u).getLeftOp();
		Value rhs = ((JAssignStmt)u).getRightOp();
		JArrayRef arrayRef = (JArrayRef) rhs;
		Value base = arrayRef.getBase();
		if(!ptg.vars.containsKey(base)) {
			// The base might be a field variable for the object.
			ObjectNode obj = new ObjectNode(utils.getBCI.get(u), ObjectType.external);
			ptg.forcePutVar(lhs, obj);
			summary.put(obj, new EscapeStatus(Escape.getInstance()));
			return;
//			throw new AnalyserPanicException("Points-to set for "+base.toString()+" doesn't exist!");
		}

		Set<ObjectNode> objs = ptg.vars.get(base);
		ObjectNode internalobj = new ObjectNode(getBCI.get(u), ObjectType.internal);
		ObjectNode externalobj = new ObjectNode(getBCI.get(u), ObjectType.external);
				
		Iterator<ObjectNode> iterator = objs.iterator();
		
		SootField f = ArrayField.instance;
		while(iterator.hasNext()) {
			ObjectNode parent = iterator.next();
			ObjectNode child = null;
			
			switch(parent.type){
			case internal: child = internalobj; break;
			case parameter:
			case external: child = externalobj; break;
			default: throw new InvalidParameterException("Array of argument makes no sense!");
			}

			ptg.makeField(parent, f, child);
			EscapeStatus es = summary.get(parent).makeField(f);
			if(summary.containsKey(child)) {
				summary.get(child).status.addAll(es.status);
			} else {
				summary.put(child, es);
			}
			
			if(ptg.vars.containsKey(lhs) && !ptg.vars.get(lhs).contains(child)) {
				ptg.vars.get(lhs).add(child);
			} else {
				ptg.forcePutVar(lhs, child);
			}
		}
	}
	
	/*
	 * Has a field reference in lhs. will have a local on rhs.
	 * A field object will NOT be created as there will already
	 * be a points-to set of rhs.
	 * This needs to be added as a field object set for every
	 * parent object.
	 * 
	 * pseudocode:
	 * for (object in parent object set){
	 * 				field_name
	 * 		object ------------> rhs object set copy.
	 * 		es(rhs) U= es(object).field_name
	 * }
	 */
	public static void StoreStmt(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		// Store case
		JInstanceFieldRef lhs = (JInstanceFieldRef)((JAssignStmt)u).getLeftOp();
		Local rhs = (Local) ((JAssignStmt)u).getRightOp();
		HashSet<ObjectNode> ptSet = (HashSet<ObjectNode>)ptg.vars.get(lhs.getBase());
		if(ptSet == null) {
			// the lhs.base must be a field variable.
			// simply set rhs to escape
			ptg.cascadeEscape(rhs, summary);
			return;
//			throw new IllegalArgumentException("[JAssignStmtHandler] ptset for "+lhs.getBase().toString()+" of "+u.toString()+" not found!");
		}
		// add field object for every parent object.
		Set<ObjectNode> objSet = ptg.vars.get(rhs);
		if(objSet==null) {
			// rhs was probably set to null before this. Hence the points to set is not found.
			// hence it needs to be set to an empty set!
			// throw new IllegalArgumentException("[JAssignStmtHandler] ptset for "+rhs.toString()+" of "+u.toString()+" not found!");			
			ptSet.forEach(parent -> {
				ptg.makeField(parent, lhs.getField(), new HashSet<>());
			});
		} else {
			ptSet.forEach(parent -> {
				ptg.makeField(parent, lhs.getField(), (Set<ObjectNode>)((HashSet<ObjectNode>)objSet).clone());
			});
			ptg.propagateES((Local)lhs.getBase(), rhs, summary);
		}
	}
	
	public static void LoadStmt(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		JAssignStmt stmt = (JAssignStmt) u;
		Local lhs = (Local)stmt.getLeftOp();
		JInstanceFieldRef rhs = (JInstanceFieldRef)stmt.getRightOp();
		if(ptg.vars.containsKey((Local)rhs.getBase())) {
			/*
			 * if(field already exists){
			 * 		assimilate and add
			 * } else {
			 * 		create new external object.
			 * 		add field to parent.
			 * 		make escape status as combination of all parents' 
			 * 		escape status
			 * }
			 */
			if(ptg.containsField((Local)rhs.getBase(), rhs.getField())) {
				// lhs it exists already
				// assemble field objects
				ptg.vars.put(lhs, ptg.assembleFieldObjects((Local)rhs.getBase(), rhs.getField()));
				// TODO: that's all? no need for make field?
			} else {
				ObjectNode obj = new ObjectNode(getBCI.get(u), ObjectType.external);
				ptg.addVar(lhs, obj);
				ptg.makeField((Local)rhs.getBase(), rhs.getField(), obj);
				
				//assimilate parents' es
				EscapeStatus parentsES = new EscapeStatus();
				Set<ObjectNode> parentsObjSet = ptg.vars.get((Local)rhs.getBase());
				Iterator<ObjectNode> it = parentsObjSet.iterator();
				while(it.hasNext()) {
					parentsES.addEscapeStatus(summary.get(it.next()));
				}
				// make field
				EscapeStatus es = parentsES.makeField(rhs.getField());
				summary.put(obj, es);
			}
		} else {
			// might be a field variable, and hence has no definiton
			// set to escape
			ObjectNode obj = new ObjectNode(utils.getBCI.get(u), ObjectType.external);
			EscapeStatus es = new EscapeStatus(Escape.getInstance());
			ptg.forcePutVar(lhs, obj);
			summary.put(obj, es);
			// lhs = obj done
			// TODO: base.field = obj
//			System.out.println(rhs.getBase() + " not found");
//			System.out.println("Handler Panic at Load Stmt!!");
//			throw new InvalidParameterException("Handler Panic at Load Stmt!!");
		}
	}
	
	public static void StaticLoadStmt(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		Local lhs = (Local)((JAssignStmt)u).getLeftOp();
		ObjectNode obj = new ObjectNode(getBCI.get(u), ObjectType.external);
		EscapeStatus es = new EscapeStatus(Escape.getInstance());
		ptg.forcePutVar(lhs, obj);
		summary.put(obj, es);
	}
	
	public static void StaticStoreStmt(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		Iterator<ObjectNode> it = ptg.reachables((Local)((JAssignStmt)u).getRightOp()).iterator();
		while(it.hasNext()) {
			summary.get(it.next()).setEscape();
		}
	}
	
	public static void EraseStmt(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		Value lhs = ((JAssignStmt) u).getLeftOp();
		if(lhs instanceof StaticFieldRef) {
			// Ignore - [Verified]
		} else if (lhs instanceof Local) {
			if(ptg.vars.containsKey(lhs)) {
				// do nothing
			} else {
				// ensure empty set for this var
				ptg.vars.put((Local)lhs, new HashSet<ObjectNode>());
			}
		} else {
			System.out.println("Unidentified case at: "+ u);
			throw new IllegalArgumentException(u.toString());
		}
	}
	
	public static void InvokeExpr(Unit u, PointsToGraph ptg, HashMap<ObjectNode, EscapeStatus> summary) {
		Local lhs = (Local)((JAssignStmt)u).getLeftOp();
		Value rhs = ((JAssignStmt)u).getRightOp();
		AbstractInvokeExpr expr = (AbstractInvokeExpr)rhs;
		SootMethod m = expr.getMethod();
		JInvokeStmtHandler.handleExpr(expr, ptg, summary);
		EscapeStatus es = new EscapeStatus(new ConditionalValue(m, new ObjectNode(0, ObjectType.returnValue), new Boolean(true)));
		ObjectNode n;
		try {
			n = new ObjectNode(getBCI.get(u), ObjectType.external);
		} catch (Exception e) {
			System.out.println("Making it an invalid obj at:"+u);
			n = InvalidBCIObjectNode.getInstance(ObjectType.external);
		}
		ptg.addVar(lhs, n);
		summary.put(n, es);
//		if(!m.isJavaLibraryMethod()) {
//			System.out.println(m.toString()+" is not a library method");
//		} 
//		else {
//			summary.put(n, new EscapeStatus());
//		} 
	}

	private static void error(Unit u) {
		JAssignStmt stmt = (JAssignStmt) u;
		Value lhs = stmt.getLeftOp();
		Value rhs = stmt.getRightOp();		
		String error = new String("Unidentified assignstmt case with "+u.toString()+" "+lhs.getClass()+","+ rhs.getClass());
		System.out.println(error);
		throw new IllegalArgumentException(error);		
	}

}
