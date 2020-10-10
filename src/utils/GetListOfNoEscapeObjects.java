package utils;

import es.EscapeStatus;
import ptg.ObjectNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GetListOfNoEscapeObjects {
	public static String get(HashMap<ObjectNode, EscapeStatus> summary) {
		ArrayList<Integer> arr = new ArrayList<>();
		for (Map.Entry<ObjectNode, EscapeStatus> entry : summary.entrySet()) {
			ObjectNode obj = entry.getKey();
			EscapeStatus es = entry.getValue();
			if (es.containsNoEscape()) arr.add(obj.ref);
		}
		Collections.sort(arr);
		String _ret = arr.toString();
		return _ret;
	}
}
