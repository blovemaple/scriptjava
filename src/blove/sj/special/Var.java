package blove.sj.special;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import blove.sj.Runner;
import blove.sj.SpecialCommand;

public class Var implements SpecialCommand {

	@Override
	public String getCommand() {
		return "var";
	}

	@Override
	public void go(String command, Runner runner)
			throws InvocationTargetException, InterruptedException, IOException {
		String[] vars;
		if (command.length() == 0)
			vars = runner.getVars().toArray(new String[] {});
		else
			vars = command.split(" ");

		if (vars.length > 0) {
			for (String var : vars) {
				System.out.print(var);
				System.out.print(" : ");
				System.out.print(runner.getVarType(var).getName());
				System.out.print(" : ");
				System.out.print(runner.getVarValue(var));
				System.out.println();
			}
		} else {
			System.out.println("(No vars)");
		}
	}

}
