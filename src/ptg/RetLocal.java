package ptg;

import soot.Type;
import soot.jimple.internal.JimpleLocal;

public class RetLocal extends JimpleLocal {

	private static final RetLocal instance = new RetLocal("_ret", null);

	private RetLocal(String name, Type type) {
		super(name, type);
	}

	public static RetLocal getInstance() {
		return instance;
	}
}
