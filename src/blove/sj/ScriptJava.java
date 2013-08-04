package blove.sj;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import blove.sj.util.SimpleClasses.ClassChooser;

public class ScriptJava {

	public static void main(String[] args) throws InterruptedException,
			ClassNotFoundException {
		Runner runner = new Runner(System.getProperty("java.io.tmpdir"),
				"javac", new ClassChooser() {
					@Override
					public Class<?> chooseClass(String simpleName,
							Set<Class<?>> classes) {
						return ScriptJava.chooseClass(simpleName, classes);
					}
				});

		String command = null;
		try {
			while (true) {
				if (System.in.available() > 0) {
					byte[] commandByte = new byte[System.in.available()];
					System.in.read(commandByte);
					command = new String(commandByte);
				} else {
					System.out.print("Enjoy ScriptJava >");
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(System.in));
					command = reader.readLine();

					if (command == null) {
						System.out.println("Bye!");
						break;
					}
				}
				try {
					runner.run(command);
				} catch (InvocationTargetException e) {
					System.out.println("Exception throwed: " + e.getCause());
				} catch (IllegalArgumentException e) {
					System.out.println("Error: Illegal command: "
							+ e.getMessage());
				}
				String unrunedCommand = runner.getUnrunedCommand();
				if (unrunedCommand != null && unrunedCommand.length() > 0) {
					System.out.println("Waiting:");
					System.out.println(unrunedCommand);
				}
			}
		} catch (IOException e) {
			System.out.println("Error: " + e);
			e.printStackTrace();
		}
	}

	private static Class<?> chooseClass(String simpleName, Set<Class<?>> classes) {
		List<Class<?>> classList = new ArrayList<>(classes);
		System.out.println("Classes for " + simpleName + ": ");
		for (int i = 0; i < classList.size(); i++) {
			Class<?> c = classList.get(i);
			System.out.println("[" + i + "]" + c.getName());
		}
		int index = -1;
		@SuppressWarnings("resource")
		Scanner inputScanner = new Scanner(System.in);
		while (index < 0 || index >= classList.size()) {
			System.out.println("Please input index:");
			String input = inputScanner.nextLine();
			try {
				index = Integer.parseInt(input);
			} catch (NumberFormatException e) {
			}
		}
		return classList.get(index);
	}
}
// TODO 两个Scanner冲突？？