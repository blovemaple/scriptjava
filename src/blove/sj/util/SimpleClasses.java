package blove.sj.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SimpleClasses {
	public static final Map<String, Class<?>> primitiveClasses;
	static {
		primitiveClasses = new HashMap<>();
		primitiveClasses.put("boolean", boolean.class);
		primitiveClasses.put("byte", byte.class);
		primitiveClasses.put("char", char.class);
		primitiveClasses.put("short", short.class);
		primitiveClasses.put("int", int.class);
		primitiveClasses.put("long", long.class);
		primitiveClasses.put("float", float.class);
		primitiveClasses.put("double", double.class);

	}

	private Map<String, Class<?>> classes = new HashMap<String, Class<?>>();

	public static interface ClassChooser {
		Class<?> chooseClass(String simpleName, Set<Class<?>> classes);
	}

	private final ClassChooser chooser;

	public SimpleClasses(ClassChooser chooser) {
		this.chooser = chooser;
	}

	public Class<?> findClass(String simpleName) throws ClassNotFoundException {
		Class<?> c;

		// 检查是否已经存在对应
		c = classes.get(simpleName);
		if (c != null)
			return c;

		// 检查是否为基本类型
		c = primitiveClasses.get(simpleName);
		if (c != null)
			return c;

		Set<Class<?>> classes = new HashSet<Class<?>>();
		Package[] pkgs = Package.getPackages();
		for (Package pkg : pkgs) {
			String tryName = pkg.getName() + "." + simpleName;
			try {
				classes.add(Class.forName(tryName));
			} catch (ClassNotFoundException e) {
			}
		}
		String tryName = simpleName;
		try {
			classes.add(Class.forName(tryName));
		} catch (ClassNotFoundException e) {
		}

		if (classes.size() == 0) {
			throw new ClassNotFoundException(simpleName);
		} else if (classes.size() > 1) {
			c = chooser.chooseClass(simpleName, classes);
		} else {
			c = classes.iterator().next();
		}

		this.classes.put(simpleName, c);
		return c;
	}
}
