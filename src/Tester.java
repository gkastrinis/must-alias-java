import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import analysis.MustAnalysis;

public class Tester {
	int testToRun = 0;

	void test(String BM) throws IOException {
		long startTime = System.currentTimeMillis();

		System.out.println("Running " + BM);
		MustAnalysis must = new MustAnalysis("resources/"+ BM +"/");

		must.applyToFixpoint();

		long endTime = System.currentTimeMillis();
		double fixpointTime = (endTime - startTime) / 1000.0;

		startTime = System.currentTimeMillis();
		must.getResults(BM + "/");
		endTime = System.currentTimeMillis();
		double metricsTime = (endTime - startTime) / 1000.0;

		System.out.println("Fixpoint time: " + fixpointTime);
		System.out.println("Metrics  time: " + metricsTime);
		//System.out.println(must.toString());
	}

	@Test
	public void dacapo2006() throws IOException {
		if (testToRun == 0) test("luindex");
	}

	//check simple move instruction	(x = y)
    @Test
	public void test1() throws IOException {
		if (testToRun == 1) test("test1");
	}

	//check simple load instruction
    @Test
	public void test2() throws IOException {
		if (testToRun == 2) test("test2");
	}

	//check move, load and store (foo5: a.f3 = b) instructions
	@Test
	public void test3() throws IOException {
		if (testToRun == 3) test("test3");
	}

	//check simple phi instructions (w = x = y)
	@Test
	public void test4() throws IOException {
		if (testToRun == 4) test("test4");
	}

	//check phi (w = x = y) with previous and next instruction
	@Test
	public void test5() throws IOException {
		if (testToRun == 5) test("test5");
	}

	//check simple intersection, just nodes (x = y and w = z)
	@Test
	public void test6() throws IOException {
		if (testToRun == 6) test("test6");
	}

	//check intersection of nodes and edges (foo7: z.f1 = x and a = b)
	@Test
	public void test7() throws IOException {
		if (testToRun == 7) test("test7");
	}

	//check intersection from multiple predecessors (foo8: z.f1 = x and r = a)
	@Test
	public void test8() throws IOException {
		if (testToRun == 8) test("test8");
	}

	//check simple call instruction with move inside collee (main: x = y, foo: a = b)
	@Test
	public void test9() throws IOException {
		if (testToRun == 9) test("test9");
	}

	//check call instruction after move (main: x = y, foo: a = b)
	@Test
	public void test10() throws IOException {
		if (testToRun == 10) test("test10");
	}

	//check call instruction with load inside collee (main: y.f1 = x, foo: b.f1 = a)
	@Test
	public void test11() throws IOException {
		if (testToRun == 11) test("test11");
	}

	//check call instruction with move inside collee, return value is argument
	@Test
	public void test12() throws IOException {
		if (testToRun == 12) test("test12");
	}

	@Test
	public void test13() throws IOException {
		if (testToRun == 13) test("test13");
	}

	@Test
	public void test14() throws IOException {
		if (testToRun == 14) test("test14");
	}

	@Test
	public void test15() throws IOException {
		if (testToRun == 15) test("test15");
	}

	@Test
	public void test16() throws IOException {
		if (testToRun == 16) test("test16");
	}

	@Test
	public void test17() throws IOException {
		if (testToRun == 17) test("test17");
	}

	@Test
	public void test18() throws IOException {
		if (testToRun == 18) test("test18");
	}

	@Test
	public void test19() throws IOException {
		if (testToRun == 19) test("test19");
	}

	@Test
	public void test20() throws IOException {
		if (testToRun == 20) test("test20");
	}

	@Test
	public void test21() throws IOException {
		if (testToRun == 21) test("test21");
	}

	@Test
	public void test22() throws IOException {
		if (testToRun == 22) test("test22");
	}

	@Test
	public void test23() throws IOException {
		if (testToRun == 23) test("test23");
	}

	@Test
	public void test24() throws IOException {
		if (testToRun == 24) test("test24");
	}
}
