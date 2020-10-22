package utils;

import es.EscapeStatus;
import ptg.ObjectNode;
import soot.SootMethod;

import java.util.HashMap;
import java.util.Map;

public class KillCallerOnly {

	/*
	 * Meant to kill all the CallerOnly conditional values.
	 */
	public static HashMap<SootMethod, HashMap<ObjectNode, EscapeStatus>> kill(HashMap<SootMethod, HashMap<ObjectNode, EscapeStatus>> solvedSummaries) {
		HashMap<SootMethod, HashMap<ObjectNode, EscapeStatus>> _ret = new HashMap<>();
		for (Map.Entry<SootMethod, HashMap<ObjectNode, EscapeStatus>> entry : solvedSummaries.entrySet()) {
			SootMethod method = entry.getKey();
			HashMap<ObjectNode, EscapeStatus> map = entry.getValue();
			HashMap<ObjectNode, EscapeStatus> q = new HashMap<>();
			for (Map.Entry<ObjectNode, EscapeStatus> e : map.entrySet()) {
				ObjectNode obj = e.getKey();
				EscapeStatus es = e.getValue();
				q.put(obj, (es.isCallerOnly()) ? getEscape() : es);
			}
			_ret.put(method, q);
		}
		return _ret;
	}

	public static EscapeStatus getEscape(){
		EscapeStatus _ret = new EscapeStatus();
		_ret.setEscape();
		return _ret;
	}
}
