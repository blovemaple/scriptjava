package blove.sj;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * 特殊命令
 * 
 * @author blove
 */
public interface SpecialCommand {
	/**
	 * 返回命令字符串。
	 * 
	 * @return 命令字符串
	 */
	String getCommand();

	/**
	 * 执行命令
	 * 
	 * @param command
	 *            命令行
	 * @param runner
	 *            runner实例
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws InvocationTargetException
	 */
	void go(String command, Runner runner) throws InvocationTargetException,
			InterruptedException, IOException;
}
