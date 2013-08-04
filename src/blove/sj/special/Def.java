package blove.sj.special;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import blove.sj.Runner;
import blove.sj.SpecialCommand;

public class Def implements SpecialCommand {

	@Override
	public String getCommand() {
		return "def";
	}

	@Override
	public void go(String command, Runner runner)
			throws InvocationTargetException, InterruptedException, IOException {
		String[] typeAndVar = command.split(" ");
		if (typeAndVar.length != 2 && typeAndVar.length != 3) {
			throw new IllegalArgumentException("Illegal define command: "
					+ command);
		}

		Class<?> c;

		String type = typeAndVar[0];
		try {
			c = Class.forName(type);
		} catch (ClassNotFoundException e) {
			try {
				c = runner.getSimpleClasses().findClass(type);
			} catch (ClassNotFoundException e1) {
				throw new IllegalArgumentException(e1);
			}
		}

		String var = typeAndVar[1];

		runner.defineVar(var, c);

		if (typeAndVar.length == 3) {
			runner.run(Runner.VAR_PREFIX + var + "=" + typeAndVar[2] + ";");
		}
	}

}
