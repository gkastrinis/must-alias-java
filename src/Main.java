import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class Main {
	public static void main(String[] args) {
		if (args.length == 0) {
			Result result = JUnitCore.runClasses(Tester.class);
			for (Failure failure : result.getFailures()) {
				System.out.println(failure.toString());
				System.out.println(failure.getException());
				failure.getException().printStackTrace();
			}
		}
		else {
			try {
				new Tester().test(args[0]);
			} catch (Exception e) {
				System.err.println(e);
			}
		}
	}
}
