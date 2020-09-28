package config;

import static config.UpdateType.*;

public class AssignStmtHandler {
	public static final UpdateType COPY = STRONG;
	public static final UpdateType ERASE = STRONG;
	public static final UpdateType INVOKE = STRONG;
	public static final UpdateType LOAD = STRONG;
	public static final UpdateType NEW = STRONG;
	public static final UpdateType STORE = WEAK;
}
