package utils;

import es.EscapeStatus;
import ptg.ObjectNode;
import ptg.ObjectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GetListOfNoEscapeObjects {
	public static String get(
			HashMap<ObjectNode, EscapeStatus> summary,
			ArrayList<ObjectNode> stackOrder) {
		ArrayList<Integer> arr = new ArrayList<>();
		for (ObjectNode obj : stackOrder) {
			if (obj.type != ObjectType.internal)
				continue;
			EscapeStatus es = summary.get(obj);
			if (es.containsNoEscape())
				arr.add(obj.ref);
		}

		String _ret = arr.toString();
		return _ret;
	}
}
