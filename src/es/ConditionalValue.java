package es;

import ptg.*;
import soot.SootField;
import soot.SootMethod;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

public class ConditionalValue extends EscapeState {
	// method will be null to denote 'caller'
	public final SootMethod method;
	public final ObjectNode object;
	public final List<SootField> fieldList;
	public final Boolean isReal;
	public final int hashcode;
	public static ConcurrentHashMap<SootMethod, ConditionalValue> retCV = new ConcurrentHashMap<>();

	public ConditionalValue(SootMethod m, ObjectNode obj, List<SootField> fl, Boolean isReal) {
//		if(m!=null && m.isJavaLibraryMethod()) {
//			System.out.println("******************************************");
//			throw new IllegalArgumentException(m.toString()+" isjavaLibraryMethod!");
//		}
		method = m;
		object = obj;
		fieldList = fl;
		this.isReal = isReal;
		int mhash;
		int mlen=0;
		if (method != null) mlen = method.getParameterCount();
		if (method == null) mhash = 0;
		else mhash = method.equivHashCode()^method.getDeclaringClass().hashCode()^mlen;
		int ohash = object.hashCode();
		int lhash = (fieldList == null) ? 0 : (fieldList.hashCode() + fieldList.size())*fieldList.size() + fieldList.hashCode();
		int hashcode = (mhash + ohash) * ohash + mhash;
		if (lhash != 0) hashcode = (hashcode + lhash) * lhash + hashcode;
		this.hashcode = hashcode;
	}

	public ConditionalValue(SootMethod m, ObjectNode obj) {
		this(m, obj, null, false);
	}

	public ConditionalValue(SootMethod m, ObjectNode obj, Boolean isArg) {
		this(m, obj, null, isArg);
	}

	public ConditionalValue addField(SootField sf) {
		List<SootField> l = (fieldList != null) ? new LinkedList<SootField>(this.fieldList) : new LinkedList<SootField>();
		l.add(sf);
		return new ConditionalValue(this.method, this.object, l, this.isReal);
	}

	public ConditionalValue addField(List<SootField> sfl) {
		List<SootField> l = new LinkedList<SootField>(this.fieldList);
		l.addAll(sfl);
		return new ConditionalValue(this.method, this.object, l, this.isReal);
	}

	@Override
	public int hashCode() {
		return this.hashcode;
	}

	/////
	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		return other instanceof ConditionalValue && this.hashcode == other.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("<");
		if (method == null) sb.append("caller,");
		else sb.append(method.toString() + ",");
		sb.append(object.toString());
		if (fieldList != null) {
			Iterator<SootField> it = fieldList.iterator();
			while (it.hasNext()) {
				sb.append("." + it.next().getName());
			}
		}
		String q = (this.isReal.booleanValue()) ? "T" : "F";
		sb.append(",[" + q + "]");
		sb.append(">");
		return sb.toString();
	}

	public SootMethod getMethod() {
		return this.method;
	}

	public int getDepth() {
		return (fieldList == null) ? 0 : this.fieldList.size();
	}

	public boolean compareAtDepth(ConditionalValue e, int depth) {
		try {
			if (!this.isReal.equals(e.isReal)) return false;
			if (!this.method.equals(e.method)) return false;
			if (this.object.type != e.object.type) return false;
			if (this.object.ref != e.object.ref) return false;
			if (depth == 0 && this.fieldList != null) return false;
			if (this.fieldList != null) {
				if (e.fieldList == null) return false;
				if (this.fieldList != null && this.fieldList.size() != depth) return false;
				for (int i = 0; i < depth && i< e.fieldList.size() ; i++) {
					if (!this.fieldList.get(i).equals(e.fieldList.get(i))) return false;
				}
			}
		} catch (Exception e2) {
			System.out.println("Comparing:" + this.toString() + " with " + e.toString() + " at depth " + depth);
			throw e2;
		}
		return true;
	}

	public EscapeState makeFalseClone() {
		return new ConditionalValue(this.method, this.object, this.fieldList, new Boolean(false));
	}

	public static ConditionalValue getRetCV(SootMethod sm) {
		if (retCV.get(sm) == null) {
			ObjectNode o = new ObjectNode(0, ObjectType.returnValue);
			retCV.put(sm, new ConditionalValue(sm,o));
		}
		return retCV.get(sm);
	}
}
