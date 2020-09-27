package ptg;

import es.ConditionalValue;
import es.EscapeStatus;
import soot.Local;
import soot.SootField;

import java.util.*;

public class PointsToGraph {
	public Map<Local, Set<ObjectNode>> vars = null;
	public Map<ObjectNode, Map<SootField, Set<ObjectNode>>> fields = null;

	public PointsToGraph() {
		vars = new HashMap<>();
		fields = new HashMap<>();
	}

	@SuppressWarnings("unchecked")
	public PointsToGraph(PointsToGraph other) {
		vars = new HashMap<>(other.vars.size());
		fields = new HashMap<>(other.fields.size());
		for (Map.Entry<Local, Set<ObjectNode>> entry : other.vars.entrySet()) {
			vars.put(entry.getKey(), (HashSet<ObjectNode>) ((HashSet<ObjectNode>) entry.getValue()).clone());
		}
		for (Map.Entry<ObjectNode, Map<SootField, Set<ObjectNode>>> entry : other.fields.entrySet()) {
			HashMap<SootField, Set<ObjectNode>> temp = new HashMap<SootField, Set<ObjectNode>>(entry.getValue().size());
			for (Map.Entry<SootField, Set<ObjectNode>> e : entry.getValue().entrySet()) {
				temp.put(e.getKey(), (Set<ObjectNode>) ((HashSet<ObjectNode>) e.getValue()).clone());
			}
			fields.put(entry.getKey(), temp);
		}
	}

	public void addVar(Local l, ObjectNode obj) {
		if (vars.containsKey(l)) {
			vars.get(l).add(obj);
		} else {
			Set<ObjectNode> objects = new HashSet<ObjectNode>();
			objects.add(obj);
			vars.put(l, objects);
		}
	}

	public void forcePutVar(Local l, ObjectNode obj) {
		Set<ObjectNode> s = new HashSet<>();
		s.add(obj);
		vars.put(l, s);
	}

	public void makeField(ObjectNode obj, SootField f, Set<ObjectNode> objSet) {
		Map<SootField, Set<ObjectNode>> fieldMap = null;
		if (!fields.containsKey(obj)) {
			fieldMap = new HashMap<SootField, Set<ObjectNode>>();
			fieldMap.put(f, objSet);
		} else {
			fieldMap = fields.get(obj);
			if (fieldMap.containsKey(f)) {
				fieldMap.get(f).addAll(objSet);
			} else fieldMap.put(f, objSet);
		}
		fields.put(obj, fieldMap);
	}

	public void makeField(ObjectNode obj, SootField f, ObjectNode child) {
		Set<ObjectNode> objSet = new HashSet<ObjectNode>();
		objSet.add(child);
		makeField(obj, f, objSet);
	}

	public void makeField(Local l, SootField f, ObjectNode child) {
		Set<ObjectNode> ptSet = vars.get(l);
		if (ptSet == null) throw new IllegalArgumentException("Pts-to set for " + l + " doesn't exist!");
		Iterator<ObjectNode> it = ptSet.iterator();
		while (it.hasNext()) {
			ObjectNode obj = it.next();
//			System.out.println(obj + f.toString() + child);
			makeField(obj, f, child);
		}
	}

	public Iterable<ObjectNode> reachables(Local l) {
		Iterable<ObjectNode> _ret = new HashSet<ObjectNode>();
		if (vars.containsKey(l)) {
			HashSet<ObjectNode> set = (HashSet<ObjectNode>) _ret;
			Iterator<ObjectNode> it = vars.get(l).iterator();
			while (it.hasNext()) {
				set.addAll((HashSet<ObjectNode>) reachables(it.next()));
			}
		}
		return _ret;
	}

	public Iterable<ObjectNode> reachables(ObjectNode obj) {
		Iterable<ObjectNode> _ret = new HashSet<ObjectNode>();
		HashSet<ObjectNode> set = (HashSet<ObjectNode>) _ret;
		HashSet<ObjectNode> thisSet = new HashSet<ObjectNode>();
		HashSet<ObjectNode> nextSet = new HashSet<ObjectNode>();
		HashSet<ObjectNode> temp = null;
		nextSet.add(obj);
		while (!nextSet.isEmpty()) {
			temp = thisSet;
			thisSet = nextSet;
			nextSet = temp;
			set.addAll(thisSet);
			nextSet.clear();
			Iterator<ObjectNode> it = thisSet.iterator();
			while (it.hasNext()) {
				ObjectNode o = it.next();
				if (fields.containsKey(o)) {
					for (Map.Entry<SootField, Set<ObjectNode>> e : fields.get(o).entrySet()) {
						nextSet.addAll(e.getValue());
					}
				}
				nextSet.remove(o);
			}
		}
		return _ret;
	}

	@Override
	public String toString() {
		return "Vars:" + vars.toString() + "\nFields:" + fields.toString();
	}

	@Override
	public boolean equals(Object o) {
		PointsToGraph ptg = (PointsToGraph) o;
		return vars.equals(ptg.vars) && fields.equals(ptg.fields);
	}

	public void union(PointsToGraph other) {
		for (Map.Entry<Local, Set<ObjectNode>> entry : other.vars.entrySet()) {
			if (vars.containsKey(entry.getKey())) {
				if (!vars.get(entry.getKey()).equals(entry.getValue())) {
					vars.get(entry.getKey()).addAll(entry.getValue());
				}
			} else {
				vars.put(entry.getKey(), new HashSet<>(entry.getValue()));
			}
		}
		for (Map.Entry<ObjectNode, Map<SootField, Set<ObjectNode>>> entry : other.fields.entrySet()) {
			if (this.fields.containsKey(entry.getKey())) {
				Map<SootField, Set<ObjectNode>> thisFieldMap = this.fields.get(entry.getKey());
				for (Map.Entry<SootField, Set<ObjectNode>> otherFieldMapEntry : entry.getValue().entrySet()) {
					if (thisFieldMap.containsKey(otherFieldMapEntry.getKey())) {
						thisFieldMap.get(otherFieldMapEntry.getKey()).addAll(otherFieldMapEntry.getValue());
					} else {
						thisFieldMap.put(otherFieldMapEntry.getKey(), (Set<ObjectNode>) ((HashSet<ObjectNode>) otherFieldMapEntry.getValue()).clone());
					}
				}
			} else {
				Map<SootField, Set<ObjectNode>> thisFieldMap = new HashMap<SootField, Set<ObjectNode>>();
				for (Map.Entry<SootField, Set<ObjectNode>> otherFieldMapEntry : entry.getValue().entrySet()) {
					thisFieldMap.put(otherFieldMapEntry.getKey(), (Set<ObjectNode>) ((HashSet<ObjectNode>) otherFieldMapEntry.getValue()).clone());
				}
				this.fields.put(entry.getKey(), thisFieldMap);
			}
		}
	}

	public boolean containsField(Local l, SootField f) {
		/*
		 * Logic: if any of the objects in the points-to set
		 * contains the given field, the return true.
		 * else return false.
		 */
		if (!vars.containsKey(l)) return false;
		Set<ObjectNode> objSet = vars.get(l);
		Iterator<ObjectNode> it = objSet.iterator();
		while (it.hasNext()) {
			ObjectNode obj = it.next();
			if (fields.containsKey(obj) && fields.get(obj).containsKey(f)) return true;
		}
		return false;
	}

	public Set<ObjectNode> assembleFieldObjects(Local l, SootField f) {
		/*
		 * Logic: Not all of the objects pointed to by 'l'
		 * may be having the field object. Assimilate the
		 * ones you can find.
		 */
		Set<ObjectNode> objSet = new HashSet<ObjectNode>();
		if (vars.containsKey(l)) {
			Iterator<ObjectNode> it = vars.get(l).iterator();
			while (it.hasNext()) {
				Map<SootField, Set<ObjectNode>> m = fields.get(it.next());
				if (m != null && m.containsKey(f)) objSet.addAll(m.get(f));
			}
		}
//		if(objSet.isEmpty()) throw new IllegalArgumentException("[AssimilateObjects] Set empty for "+l.toString()+"."+f.toString());
		return objSet;
	}

	public void cascadeCV(Local l, ConditionalValue cv, Map<ObjectNode, EscapeStatus> summary) {
		if (!vars.containsKey(l)) return;
		HashSet<ObjectNode> done = new HashSet<ObjectNode>();
		Iterator<ObjectNode> it = vars.get(l).iterator();
		while (it.hasNext()) {
			ObjectNode o = it.next();
			if (summary.containsKey(o)) {
//				System.out.println("Adding "+cv.toString()+" to "+o.toString());
				summary.get(o).addEscapeState(cv);
//				System.out.println("verifying add: "+summary.get(o).toString());
			} else summary.put(o, new EscapeStatus(cv));
			recursiveCascadeES(o, summary, done);
			done.add(o);
		}
	}

	public void cascadeES(Local l, Map<ObjectNode, EscapeStatus> summary) {
		if (!vars.containsKey(l)) return;
		HashSet<ObjectNode> done = new HashSet<ObjectNode>();
		vars.get(l).forEach(parent -> recursiveCascadeES(parent, summary, done));
	}

	public void recursiveCascadeES(ObjectNode parent, Map<ObjectNode, EscapeStatus> summary, HashSet<ObjectNode> done) {
		if (done.contains(parent)) return;
		if (!fields.containsKey(parent)) return;
		HashSet<ObjectNode> children = new HashSet<ObjectNode>();
		for (Map.Entry<SootField, Set<ObjectNode>> entry : fields.get(parent).entrySet()) {
			/*
			 * Possible Optimization:
			 * Conditional values are immutable. So maybe every object
			 * doesn't require a fresh copy of the same conditional value.
			 */
			EscapeStatus es = summary.get(parent).makeField(entry.getKey());
			entry.getValue().forEach(object -> summary.get(object).addEscapeStatus(es));
			children.addAll(entry.getValue());
		}
		done.add(parent);
		children.remove(parent);
		children.forEach(child -> recursiveCascadeES(child, summary, done));
	}

	public void propagateES(Local lhs, Local rhs, Map<ObjectNode, EscapeStatus> summary) {
		EscapeStatus es1 = new EscapeStatus();
		vars.get(lhs).forEach(parent -> es1.addEscapeStatus(summary.get(parent)));
//		es = es.makeFalseClone();
		EscapeStatus es2 = es1.makeFalseClone();
		Set<ObjectNode> done = new HashSet<>();
		if (vars.containsKey(rhs)) vars.get(rhs).forEach(obj -> {
			summary.get(obj).addEscapeStatus(es2);
			recursivePropagateES(obj, es2, summary, done);
			done.add(obj);
		});
	}

	public void recursivePropagateES(ObjectNode obj, EscapeStatus es, Map<ObjectNode, EscapeStatus> summary, Set<ObjectNode> done) {
		if (done.contains(obj)) return;
		if (!fields.containsKey(obj)) return;
		HashSet<ObjectNode> children = new HashSet<ObjectNode>();
		Map<SootField, Set<ObjectNode>> map = fields.get(obj);
		map.forEach((f, set) -> {
			set.forEach(o -> {
				summary.get(o).addEscapeStatus(es);
				children.add(o);
			});
		});
		done.add(obj);
		children.remove(obj);
		children.forEach(child -> recursivePropagateES(child, es, summary, done));

	}

	public void setAsReturn(Local l, Map<ObjectNode, EscapeStatus> summary) {
		if (!vars.containsKey(l)) return;
		Set<ObjectNode> s = new HashSet<>();
		s.addAll(vars.get(l));
		// TODO: Incorrect. Rectify this.
		vars.put(RetLocal.getInstance(), s);
		ObjectNode o = new ObjectNode(0, ObjectType.returnValue);
		ConditionalValue ret = new ConditionalValue(null, o);
		cascadeCV(l, ret, summary);
	}

	public void cascadeEscape(Local l, Map<ObjectNode, EscapeStatus> summary) {
		if (!vars.containsKey(l)) return;
		HashSet<ObjectNode> done = new HashSet<>();
		vars.get(l).forEach(object -> {
			recursiveCascadeEscape(object, summary, done);
			done.add(object);
		});
	}

	public void recursiveCascadeEscape(ObjectNode object, Map<ObjectNode, EscapeStatus> summary, HashSet<ObjectNode> done) {
		if (done.contains(object)) return;
		summary.get(object).setEscape();
		done.add(object);
		HashSet<ObjectNode> children = new HashSet<>();
		if (fields.containsKey(object)) {
			fields.get(object).forEach((field, objSet) -> children.addAll(objSet));
		}
		children.remove(object);
		children.forEach(child -> recursiveCascadeEscape(child, summary, done));
	}

	public void storeStmtArrayRef(Local lhs, Local rhs) {
		if (!vars.containsKey(lhs)) {
			throw new IllegalArgumentException("ptset for " + lhs.toString() + " Does not exist!");
		}
		if (!vars.containsKey(rhs)) {
			throw new IllegalArgumentException("ptset for " + rhs.toString() + " Does not exist!");
		}
		vars.get(lhs).forEach(parent -> {
			if (!fields.containsKey(parent)) fields.put(parent, new HashMap<>());
			Map<SootField, Set<ObjectNode>> map = fields.get(parent);
			if (!map.containsKey(ArrayField.instance)) {
				HashSet<ObjectNode> set = new HashSet<>();
				set.addAll(vars.get(rhs));
				map.put(ArrayField.instance, set);
			} else {
				map.get(ArrayField.instance).addAll(vars.get(rhs));
			}
		});
	}

	public void storeStmtArrayRef(Local lhs, ObjectNode obj) {
		if (!vars.containsKey(lhs)) {
			throw new IllegalArgumentException("ptset for " + lhs.toString() + " Does not exist!");
		}
		vars.get(lhs).forEach(parent -> {
			if (!fields.containsKey(parent)) fields.put(parent, new HashMap<>());
			Map<SootField, Set<ObjectNode>> map = fields.get(parent);
			if (!map.containsKey(ArrayField.instance)) {
				HashSet<ObjectNode> set = new HashSet<>();
				set.add(obj);
				map.put(ArrayField.instance, set);
			} else {
				map.get(ArrayField.instance).add(obj);
			}
		});

	}

	public boolean isEmpty() {
		return this.vars.isEmpty() && this.fields.isEmpty();
	}

}
