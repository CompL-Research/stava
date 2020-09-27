package utils;

import es.EscapeStatus;
import ptg.ObjectNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class GetListOfNoEscapeObjects {
	public static String get(HashMap<ObjectNode, EscapeStatus> summary) {
		ArrayList<Integer> arr = new ArrayList<>();
		summary.forEach((obj, es) -> {
			if (es.containsNoEscape()) arr.add(obj.ref);
		});
		Collections.sort(arr);
		String _ret = arr.toString();
		return _ret;
	}
}
