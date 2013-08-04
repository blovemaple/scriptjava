package blove.sj.special;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import blove.sj.Runner;
import blove.sj.SpecialCommand;

public class Syso implements SpecialCommand {

	@Override
	public String getCommand() {
		return "syso";
	}

	@Override
	public void go(String command, Runner runner)
			throws InvocationTargetException, InterruptedException, IOException {
		String outParam;
		if (runner.hasVar(command)) {
			outParam = Runner.VAR_PREFIX + command;
		} else {
			outParam = command;
		}
		runner.run("System.out.println(" + outParam + ");");
	}

}
