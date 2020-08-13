package utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import es.EscapeStatus;
import ptg.ObjectNode;

public class GetListOfNoEscapeObjects {
	public static String get(HashMap<ObjectNode,EscapeStatus> summary) {
		ArrayList<Integer> arr = new ArrayList<>();
		summary.forEach((obj, es) ->{
			if(es.containsNoEscape()) arr.add(obj.ref);
		});
		Collections.sort(arr);
		String _ret = new String(arr.toString());
		return _ret;
	}
}
