package aQute.jpm.main;

import junit.framework.TestCase;

public class ServiceTest extends TestCase {

	@Override
	protected void setUp() throws Exception {
		removeTestService();
	}

	public void testService() throws Exception {
		Main.main(new String[] {
			"-etu", "service", "--create", "target/test-classes/biz.aQute.jpm.daemon.jar", "dm"
		});
	}

	@Override
	protected void tearDown() throws Exception {
		removeTestService();
	}

	private void removeTestService() throws Exception {
		Main.main(new String[] {
			"-etu", "service", "--remove", "dm"
		});
	}
}
