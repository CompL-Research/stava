package config;

public enum UpdateType {
	/*
	 * For strong updates which may erase an existing edge
	 */
	STRONG,

	/*
	 * For weak updates which can only create new edges
	 */
	WEAK
}
