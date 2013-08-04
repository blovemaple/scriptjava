package blove.sj;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import blove.sj.special.Def;
import blove.sj.special.Syso;
import blove.sj.special.Var;
import blove.sj.util.SimpleClasses;
import blove.sj.util.SimpleClasses.ClassChooser;

public class Runner {
	public static final String DEFINE_VAR_COMMAND_PREFIX = ".def ";
	public static final String STANDART_OUT_COMMAND_PREFIX = ".syso ";

	public static final String SPECIAL_COMMAND_PREFIX = ".";
	public static final String VAR_PREFIX = "$";
	public static final String SIMPLE_CLASS_PREFIX = "#";

	public static final String COMMAND_CLASS_NAME = "ScriptJavaCommand";
	public static final String COMMAND_METHOD_NAME = "go";

	private final Path classDir;
	private final String javac;
	private final List<SpecialCommand> specials = new LinkedList<>();

	private final SimpleClasses classes;

	private Map<String, Class<?>> varTypes = new HashMap<String, Class<?>>();
	private Map<String, Object> varObjects = new HashMap<String, Object>();
	private StringBuilder unrunedCommand = new StringBuilder();

	private StringBuilder javaCommandSB;

	public Runner(String classDir, String javac, ClassChooser classChooser,
			SpecialCommand... specials) {
		checkNull(classDir);
		checkNull(javac);
		checkNull(classChooser);

		this.classDir = Paths.get(classDir);
		this.javac = javac;

		classes = new SimpleClasses(classChooser);

		for (SpecialCommand specialCommand : specials) {
			this.specials.add(specialCommand);
		}
		this.specials.add(new Def());
		this.specials.add(new Syso());
		this.specials.add(new Var());
	}

	private void checkNull(Object o) {
		if (o == null)
			throw new NullPointerException();
	}

	public SimpleClasses getSimpleClasses() {
		return classes;
	}

	public boolean hasVar(String var) {
		return varTypes.containsKey(var);
	}

	public Set<String> getVars() {
		return varTypes.keySet();
	}

	public Class<?> getVarType(String var) {
		return varTypes.get(var);
	}

	public void defineVar(String var, Class<?> type) {
		varObjects.remove(var);
		varTypes.put(var, type);
	}

	public Object getVarValue(String var) {
		return varObjects.get(var);
	}

	public void setVarValue(String var, Object value) {
		if (value != null) {
			Class<?> type = varTypes.get(var);
			if (type == null)
				throw new IllegalStateException("Var " + var + " is not exist.");
			if (!type.isInstance(value))
				throw new ClassCastException("Object for var " + var
						+ " is not assignable for its type " + type.getName());
		}

		varObjects.put(var, value);
	}

	/**
	 * 执行脚本命令。
	 * 
	 * @param command
	 *            脚本命令
	 * @throws InterruptedException
	 *             编译命令class时当前线程被中断
	 * @throws InvocationTargetException
	 *             脚本命令中抛出异常
	 * @throws IOException
	 *             读、写、编译命令class时出现异常
	 */
	public void run(String command) throws InterruptedException,
			InvocationTargetException, IOException {
		unrunedCommand.append(command);

		StringBuilder onceCommand = new StringBuilder();
		String singleCommand;
		while ((singleCommand = nextCommand()) != null) {
			SpecialCommand special = null;
			String specialHead = null;

			// 检测是否是特殊命令
			for (SpecialCommand s : specials) {
				specialHead = SPECIAL_COMMAND_PREFIX + s.getCommand();
				if (singleCommand.startsWith(specialHead + " ")
						|| singleCommand.equals(specialHead + ";")) {
					special = s;
					break;
				}
			}

			if (special == null) {
				// 是Java命令
				onceCommand.append(singleCommand);
			} else {
				// 是特殊命令
				if (onceCommand.length() > 0) {
					runJavaCommand(onceCommand.toString());
					onceCommand.setLength(0);
				}
				String args;
				if (singleCommand.startsWith(specialHead + " "))
					args = singleCommand.substring(specialHead.length() + 1,
							singleCommand.length() - 1);
				else
					args = "";
				special.go(args, this);// 去掉命令头和结尾的分号
			}
		}
		if (onceCommand.length() > 0) {
			runJavaCommand(onceCommand.toString());
		}

	}

	public String getUnrunedCommand() {
		return unrunedCommand.toString();
	}

	private String nextCommand() {
		int braceLevel = 0;
		int index = 0;
		boolean ok = false;
		findCommand: for (; index < unrunedCommand.length(); index++) {
			char c = unrunedCommand.charAt(index);
			switch (c) {
			case ';':
				if (braceLevel == 0) {
					ok = true;
					break findCommand;
				}
				break;
			case '{':
				braceLevel++;
				break;
			case '}':
				braceLevel--;
				if (braceLevel == 0) {
					ok = true;
					break findCommand;
				}
				break;
			}
		}

		if (ok) {
			String command = unrunedCommand.substring(0, index + 1);
			unrunedCommand.delete(0, index + 1);
			return command;
		} else {
			return null;
		}
	}

	private void runJavaCommand(String command) throws InterruptedException,
			InvocationTargetException, IOException {
		javaCommandSB = new StringBuilder(command);

		// 处理命令中所有变量
		Set<String> vars = processIdentifier(VAR_PREFIX, new VarProcessor());

		// 处理命令中所有简单名称类
		processIdentifier(SIMPLE_CLASS_PREFIX, new ClassProcessor());

		// 生成java源文件，以命令中所有变量作为实例变量，以语句作为方法体
		Path sourceFile = writeSource(vars);

		// 编译java源文件
		try {
			compileSource(sourceFile);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Illegal Java command: "
					+ command + " " + e.getMessage(), e);
		}

		try {
			// 生成Class对象
			ClassLoader commandClassLoader = new CommandClassLoader();
			Class<?> commandClass = commandClassLoader
					.loadClass(COMMAND_CLASS_NAME);

			// 创建实例并置入变量值
			Object commandObject = commandClass.newInstance();
			for (String var : vars) {
				Field varField = commandClass.getField(var);
				Object value = varObjects.get(var);
				if (value != null)
					varField.set(commandObject, value);
			}

			// 调用方法
			Method commandMethod = commandClass.getMethod(COMMAND_METHOD_NAME);
			commandMethod.invoke(commandObject);

			// 取变量值并写入objects
			for (String var : vars) {
				Field varField = commandClass.getField(var);
				varObjects.put(var, varField.get(commandObject));
			}
		} catch (ClassNotFoundException | InstantiationException
				| IllegalAccessException | NoSuchFieldException
				| SecurityException | IllegalArgumentException
				| NoSuchMethodException e) {
			throw new IOException(e);
		}
	}

	private interface IdentifierProcessor {
		String getReplacement(String identifier);
	}

	private class VarProcessor implements IdentifierProcessor {
		@Override
		public String getReplacement(String identifier) {
			return identifier;
		}
	}

	private class ClassProcessor implements IdentifierProcessor {
		@Override
		public String getReplacement(String identifier) {
			try {
				Class<?> c = classes.findClass(identifier);
				return c.getName();
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

	private Set<String> processIdentifier(String prefix,
			IdentifierProcessor processor) {
		Set<String> is = new HashSet<String>();

		int prefixIndex;
		while ((prefixIndex = javaCommandSB.indexOf(prefix)) >= 0) {
			int iCharStartIndex = prefixIndex + prefix.length();

			StringBuilder iSB = new StringBuilder();
			int iCharCrtIndex = iCharStartIndex;
			boolean isRealIChar = true;
			boolean firstIChar = true;
			while (iCharCrtIndex < javaCommandSB.length() && isRealIChar) {
				char iChar = javaCommandSB.charAt(iCharCrtIndex);
				isRealIChar = firstIChar ? Character
						.isJavaIdentifierStart(iChar) : Character
						.isJavaIdentifierPart(iChar);
				firstIChar = false;
				if (isRealIChar) {
					iSB.append(iChar);
					iCharCrtIndex++;
				}
			}
			if (iSB.length() > 0) {
				String i = iSB.toString();
				is.add(i);
				javaCommandSB.replace(prefixIndex,
						iCharStartIndex + i.length(),
						processor.getReplacement(i));
			}
		}

		return is;
	}

	private Path writeSource(Set<String> vars) {
		Path sourceFile = classDir.resolve(COMMAND_CLASS_NAME + ".java");
		try (BufferedWriter writer = Files.newBufferedWriter(sourceFile,
				Charset.defaultCharset())) {
			// 类头
			writer.write("public class " + COMMAND_CLASS_NAME + "{");
			writer.newLine();

			// 实例变量
			for (String var : vars) {
				writer.write("public ");
				writer.write(varTypes.get(var).getName());
				writer.write(" ");
				writer.write(var);
				writer.write(";");
				writer.newLine();
			}
			// 方法头
			writer.write("public void " + COMMAND_METHOD_NAME
					+ "() throws Exception {");
			writer.newLine();

			// 方法体
			writer.write(javaCommandSB.toString());
			writer.newLine();

			// 方法尾
			writer.write("}");
			writer.newLine();

			// 类尾
			writer.write("}");

		} catch (IOException e) {
			throw new IllegalArgumentException("Writing source file failed: "
					+ sourceFile);
		} finally {
		}
		return sourceFile;
	}

	private void compileSource(Path sourceFile) throws InterruptedException {
		try {
			ProcessBuilder compileProcess = new ProcessBuilder(Arrays.asList(
					javac, sourceFile.toString()));
			Process process = compileProcess.start();
			BufferedReader errReader = new BufferedReader(
					new InputStreamReader(process.getErrorStream()));

			StringBuilder errStrSB = new StringBuilder();
			errStrSB.append("----- javac error output start -----");
			errStrSB.append(System.lineSeparator());
			String errLine;
			while ((errLine = errReader.readLine()) != null)
				errStrSB.append(errLine).append(System.lineSeparator());
			errStrSB.append("------ javac error output end ------");

			int exitValue = process.waitFor();
			if (exitValue != 0)
				throw new IllegalArgumentException("javac error: " + exitValue
						+ System.lineSeparator() + errStrSB.toString());
		} catch (IOException e) {
			throw new IllegalArgumentException("javac error", e);
		}
	}

	private class CommandClassLoader extends ClassLoader {
		public CommandClassLoader() {
			super(ClassLoader.getSystemClassLoader());
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			Path classFile = classDir.resolve(name + ".class");
			if (Files.exists(classFile)) {

				try {
					byte[] classFileBytes = Files.readAllBytes(classDir
							.resolve(name + ".class"));
					return defineClass(name, classFileBytes, 0,
							classFileBytes.length);
				} catch (ClassFormatError | IOException e) {
					throw new ClassNotFoundException(name, e);
				}
			} else {
				return getParent().loadClass(name);
			}
		}
	}
}
