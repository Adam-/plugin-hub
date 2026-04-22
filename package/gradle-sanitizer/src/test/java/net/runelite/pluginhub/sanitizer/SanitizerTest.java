package net.runelite.pluginhub.sanitizer;

import java.io.IOException;
import org.junit.Test;

public class SanitizerTest
{
	@Test
	public void testSanitizer() throws IOException
	{
		var s = new Sanitizer();
		try (var in = getClass().getResourceAsStream("build.gradle"))
		{
			s.run(in, System.out);
		}
	}
}
