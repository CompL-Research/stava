package ptg;

/*
 * Enumeration to specify the type of an object.
 */

public enum ObjectType {

	/*
	 * This object has been declared in this method body.
	 */
	internal,

	/*
	 * This object has been declared elsewhere, and
	 * wasn't passed as a parameter to this method.
	 */
	external,

	/*
	 * Argument is the one that is passed on to another function.
	 * Is this actually required??
	 */
	argument,

	/*
	 * Parameter is the one that is received by the function
	 * parameter with a reference -1 will be used for 'this'.
	 */
	parameter,

	/*
	 * For an object that is returned to the caller.
	 */
	returnValue

}