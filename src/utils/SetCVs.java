package utils;

import java.util.Iterator;
import java.util.Map;

import es.ConditionalValue;
import es.EscapeStatus;
import ptg.ObjectNode;

public class SetCVs {
	public static void set(Iterable<ObjectNode> set, ConditionalValue cv, Map<ObjectNode, EscapeStatus> summary) {
		if(set == null) return;
		Iterator<ObjectNode> i = set.iterator();
		while(i.hasNext()) {
			summary.get(i.next()).addEscapeState(cv);
		}
	}
}
