package net.runelite.pluginhub.sanitizer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class Sanitizer
{
	public static void main(String[] args) throws IOException
	{
		new Sanitizer().run(System.in, System.out);
	}

	public void run(InputStream in, OutputStream out) throws IOException
	{
		CharStream charStream = CharStreams.fromStream(in);
		gradleLexer lexer = new gradleLexer(charStream);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		gradleParser parser = new gradleParser(tokens);

		ParseTree tree = parser.file();

//		System.out.println(tree.toStringTree(parser));

		Visitor v = new Visitor();
		v.visit(tree);

		var ps = new PrintStream(out);
		ps.println("""
			plugins {
			    id 'java'
			}
			
			repositories {
			    mavenLocal()
			    maven {
			        url = 'https://repo.runelite.net'
			        content {
			            includeGroupByRegex("net\\\\.runelite.*")
			        }
			    }
			    mavenCentral()
			}
			
			tasks.withType(JavaCompile).configureEach {
				options.encoding = 'UTF-8'
				options.release.set(11)
			}
			""");

		ps.println("dependencies {");
		for (var dep : v.deps) {
			ps.println("  " + dep.config() + " '" + dep.group() + ":" +  dep.artifact() + ":" + dep.version() + "'");
		}
		ps.println("}");
	}
}

record Dependency(String config, String group, String artifact, String version)
{
}

class Visitor extends gradleBaseVisitor<Void>
{
	List<Dependency> deps = new ArrayList<>();

	@Override
	public Void visitDependencyStmt(gradleParser.DependencyStmtContext ctx)
	{
		String config = ctx.configuration().getText();

		String group = null;
		String name = null;
		String version = null;

		gradleParser.MapNotationContext map = ctx.mapNotation();
		if (map != null)
		{
			if (map.GROUP() != null)
			{
				gradleParser.ValueContext vctx = map.value(0);
				if (vctx.IDENTIFIER() != null) group = vctx.IDENTIFIER().getText();
				else if (vctx.STRING() != null) {
					var s = vctx.STRING().getText();
					group = s.substring(1, s.length() - 1);
				}
			}
			if (map.NAME() != null)
			{
				gradleParser.ValueContext vctx = map.value(1);
				if (vctx.IDENTIFIER() != null) name = vctx.IDENTIFIER().getText();
				else if (vctx.STRING() != null) {
					var s = vctx.STRING().getText();
					name = s.substring(1, s.length() - 1);
				}
			}
			if (map.VERSION() != null)
			{
				gradleParser.ValueContext vctx = map.value(2);
				if (vctx.IDENTIFIER() != null) version = vctx.IDENTIFIER().getText();
				else if (vctx.STRING() != null) {
					var s = vctx.STRING().getText();
					version = s.substring(1, s.length() - 1);
				}
			}
		}

		// string notation fallback: 'group:name:version'
		if (ctx.stringNotation() != null) {
			String raw = ctx.stringNotation().getText()
				.replace("'", "");

			String[] parts = raw.split(":");
			if (parts.length >= 3) {
				group = parts[0];
				name = parts[1];
				version = parts[2];
			}
		}

		deps.add(new Dependency(config, group, name, version));

		return null;
	}

	@Override
	public Void visitVersionAssignment(gradleParser.VersionAssignmentContext ctx)
	{
		String value = ctx.VERSION().getText();
		return null;
	}
}