import sun.misc.Unsafe;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
class Data{
	String s;

	public Data(String st) {
		this.s = st;
	}
	public String toString() {
		return this.s;
	}
}
class Node {
	static Node a;
	
	public Data dt;
	Unsafe unsafe;

	public Node() {
		Constructor<Unsafe> unsafeConstructor;
		try {
			unsafeConstructor = Unsafe.class.getDeclaredConstructor();
			unsafeConstructor.setAccessible(true);
			try {
				this.unsafe = unsafeConstructor.newInstance();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (NoSuchMethodException | SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public void escapeObject() {
		a = this;
	}

	public Data getObject() {
		Class c = this.getClass();
		Field f;
		try {
			f = c.getField("dt");
			long idofset = unsafe.objectFieldOffset(f);
			return (Data)unsafe.getObject(this, idofset);
		} catch (NoSuchFieldException | SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	// @Override
	public void setId(Data dt) {
		Class c = this.getClass();
		Field f;
		try {
			Field[] fieldArr = c.getFields();
			f = c.getField("dt");
			long idofset = unsafe.objectFieldOffset(f);
			unsafe.putObject(this, idofset, dt);
		} catch (NoSuchFieldException | SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// int length = Math.min(id.length(), 14);
		
		// unsafe.putByte(idofset, (byte)length );
		// for (long i = 0; i < length; i++) {
		// 	System.out.println("writing:  " + id.charAt( (int)i)  + " at address: " + (idofset + i + 1));
		// 	unsafe.putChar(idofset + i + 1 , id.charAt( (int)i ));
		// }
	}
}


public class Main {
	public static void main(String[] args) {
		// Constructor<Unsafe> unsafeConstructor = Unsafe.class.getDeclaredConstructor();
		// unsafeConstructor.setAccessible(true);
		// Unsafe unsafe = unsafeConstructor.newInstance();
		Node A = new Node();
		Data dt = new Data("Hello");
		A.setId(dt);
		Data ds = A.getObject();
		System.out.println(ds);
		Node B = new Node();
		Node C = new Node();
		Node D = new Node();
	}
}
