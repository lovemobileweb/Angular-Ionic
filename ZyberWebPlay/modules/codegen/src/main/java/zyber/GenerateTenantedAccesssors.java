package zyber;

import java.io.File;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class GenerateTenantedAccesssors extends VoidVisitorAdapter<Boolean> {
	static String gPackage = "zyber.server.dao";
	static String tenantColumn = "tenantId";

	private static List<File> getFiles(String inputFolder) throws IllegalArgumentException, MalformedURLException {

		File folder = new File(inputFolder);

		return Arrays.asList(folder.listFiles());
	}

	public static void main(String... argsIn) throws Exception {
//		if (argsIn.length == 3) return;
		String[] args = argsIn;
		try {
			if (args.length == 0) {
				args = new String[] {
					"C:\\Dev\\Zyber\\zyber-zyber.zyber\\ZyberWebPlay\\app\\zyber\\server\\dao\\rawaccessors",
					"C:\\Dev\\Zyber\\zyber-zyber.zyber\\ZyberWebPlay\\app\\zyber\\server\\dao",
					"C:\\Dev\\Zyber\\zyber-zyber.zyber\\ZyberWebPlay\\modules\\codegen\\src\\main\\java\\zyber\\GenerateTenantedAccesssors.java"
				};
			}
			assert (args.length == 3);
			String inputFolder = args[0];
			String outputFolder = args[1];
			String generatorPath = args[2];

			for (File f : getFiles(inputFolder)) {
				File out = new File(outputFolder + File.separator + f.getName());
				File generatorSrc = new File(generatorPath);

				if (out.exists() && out.lastModified() > generatorSrc.lastModified()
						&& out.lastModified() > f.lastModified()) {
					System.out.println("Skipping: "+out.getName()+" File newer than source and generator: " + f.getName());
					continue;
				}

				FileInputStream in = new FileInputStream(f);

				CompilationUnit cu;
				try {
					// parse the file
					cu = JavaParser.parse(in);
				} finally {
					in.close();
				}
				try {
					GenerateTenantedAccesssors ga = new GenerateTenantedAccesssors();
					ga.visit(cu, false);
					String generatedAccessorStr = ga.head.toString() + ga.imports + ga.methods + "\n" + ga.footer;
					System.out.println("File generated: " + out);
					FileUtils.writeStringToFile(out, generatedAccessorStr);
				} catch (NoAccessorException e) {

					GenerateTenantedAccesssorsInterface gai = new GenerateTenantedAccesssors().new GenerateTenantedAccesssorsInterface();
					gai.visit(cu, false);
					String generatedAccessorStr = gai.head.toString() + gai.imports + gai.methods + "\n" + gai.footer;
					System.out.println("File generated: " + out+ "(with no accessor exception)");
					FileUtils.writeStringToFile(out, generatedAccessorStr);
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
			throw new java.lang.IllegalStateException("Printed error to stdout, and re-throwing.", e);
		}

	}

	StringBuffer head = new StringBuffer();
	StringBuffer imports = new StringBuffer();
	StringBuffer methods = new StringBuffer();
	StringBuffer footer = new StringBuffer();

	Map<String, String> uses = new HashMap<String, String>();

	public String use(Class<?> useMe) {
		return use(useMe.getName());
	}

	public String use(String useMe) {
		if (implementingClassName != null && useMe.endsWith(implementingClassName)) {
			return useMe;
		}

		if (uses.containsKey(useMe)) {
			return uses.get(useMe);
		}

		String cls = useMe.substring(useMe.lastIndexOf(".") + 1);
		if (uses.values().contains(cls)) {
			return useMe;
		}
		uses.put(useMe, cls);

		if (!useMe.startsWith("java.lang")) {
			imports.append("import " + useMe + ";\n");
		}
		return cls;
	}

	// Generate accessors that are tenant specific by deleting the tenant
	// parameter from the call,
	// and then wrapping with a delegate call.
	/** declared by {@link class:FileVersion} */
	String implementingClassPackage;

	@Override
	public void visit(final PackageDeclaration n, final Boolean arg) {
		implementingClassPackage = n.getName().toStringWithoutComments();
		head.append("package " + gPackage + ";\n\n");
		uses.put(gPackage + "." + classSimpleName, gPackage + "." + classSimpleName);
		super.visit(n, arg);
	}

	@Override
	public void visit(final ImportDeclaration n, final Boolean arg) {
		super.visit(n, arg);
		if (n.getName().toString().length() > 1) {
			use(n.getName().toString());
		}
	}

	String classSimpleName;
	String implementingClassName;

	@Override
	public void visit(final ClassOrInterfaceDeclaration n, final Boolean arg) {

		// AnnotationExpr d = AnnotationExpr.class
		// n.getAnnotations();
		if(!containsAnnotation(n.getAnnotations(), "Accessor"))
			throw new NoAccessorException();
		classSimpleName = n.getName();
		implementingClassName = implementingClassPackage + "." + classSimpleName;

		head.append("\n/** Use this accessor, but do not modify it, it was genereated by " + this.getClass().getName()
				+ ".\n*\n* This accessor filters for the tenant.\n*\n* Modify the accessor:  {@link class:"
				+ implementingClassName + "}\n*/\n\n");

		methods.append("\n@SuppressWarnings(\"all\")\n");
		methods.append("public class " + classSimpleName);

		if (null != n.getExtends() && n.getExtends().size() == 1) {
			methods.append(" implements " + n.getExtends().get(0).getName());
		}
		methods.append(" {\n\n");
		methods.append("    public final " + use(implementingClassName) + " _accessor;\n");
		methods.append("    public final " + use(UUID.class) + " _tenant;\n");
		methods.append("\n");

		methods.append("    public " + classSimpleName + "(" + use("com.datastax.driver.mapping.MappingManager")
				+ " manager, " + use(UUID.class) + " _tenant) {\n");
		methods.append("        this._accessor = manager.createAccessor(" + use(implementingClassName) + ".class);\n");
		methods.append("        this._tenant = _tenant;\n");
		methods.append("    }\n");

		super.visit(n, arg);

		footer.append("}\n");

	}

	private AnnotationExpr getFirstMatchingAnnotation(List<AnnotationExpr> anno, String typeToFind) {
		if (anno == null)
			return null;
		for (AnnotationExpr a : anno) {
			if (a.getName().getName().equals(typeToFind)) {
				return a;
			}
		}
		return null;
	}
	
	private boolean containsAnnotation(List<AnnotationExpr> anno, String typeToFind){
		if (anno == null)
			return false;
		for (AnnotationExpr a : anno) {
			if (a.getName().getName().equals(typeToFind)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void visit(MethodDeclaration m, final Boolean arg) {
		super.visit(m, arg);

		String returnType = m.getType().toString();

		methods.append("    /** Originally declared by {@link class:" + implementingClassName + "} */\n");
		methods.append("    public " + returnType + " " + m.getName() + "(");

		boolean hadTenantColumn = false;
		for (Parameter p : m.getParameters()) {
			String argName = p.getId().getName();

			AnnotationExpr pa = getFirstMatchingAnnotation(p.getAnnotations(), "Param");
			if (pa != null && (pa instanceof com.github.javaparser.ast.expr.SingleMemberAnnotationExpr)
					&& ((StringLiteralExpr) ((com.github.javaparser.ast.expr.SingleMemberAnnotationExpr) pa)
							.getMemberValue()).getValue().equals(tenantColumn)) {
				hadTenantColumn = true;
				continue;
			}

			methods.append(p.getType() + " " + argName + ", ");
		}
		if (!hadTenantColumn) {
			System.out.println("WARNING: method " + implementingClassName + "." + m.getName()
					+ "(...): Does not indicate the tenant");
		}
		if (methods.toString().endsWith(", "))
			methods.delete(methods.length() - 2, methods.length());
		methods.append(") { \n");
		if (!"void".equals(returnType))
			methods.append("        return _accessor." + m.getName() + "(");
		else
			methods.append("        _accessor." + m.getName() + "(");
		for (Parameter p : m.getParameters()) {
			AnnotationExpr pa = getFirstMatchingAnnotation(p.getAnnotations(), "Param");
			String argName = p.getId().getName();
			if (pa != null && (pa instanceof com.github.javaparser.ast.expr.SingleMemberAnnotationExpr)
					&& ((StringLiteralExpr) ((com.github.javaparser.ast.expr.SingleMemberAnnotationExpr) pa)
							.getMemberValue()).getValue().equals(tenantColumn)) {
				methods.append("_tenant, ");
			} else {
				methods.append(argName + ", ");
			}

		}
		if (methods.toString().endsWith(", "))
			methods.delete(methods.length() - 2, methods.length());
		methods.append(");\n");
		methods.append("    }\n\n");
	}

	// Generate a mapper that set's the tenant on save/load.

	//FIXME code duplication
	public class GenerateTenantedAccesssorsInterface extends VoidVisitorAdapter<Boolean> {
		StringBuffer head = new StringBuffer();
		StringBuffer imports = new StringBuffer();
		StringBuffer methods = new StringBuffer();
		StringBuffer footer = new StringBuffer();
		String classSimpleName;
		String implementingClassName;
		String implementingClassPackage;
		
		@Override
		public void visit(final PackageDeclaration n, final Boolean arg) {
			implementingClassPackage = n.getName().toStringWithoutComments();
			head.append("package " + gPackage + ";\n\n");
			uses.put(gPackage + "." + classSimpleName, gPackage + "." + classSimpleName);
			super.visit(n, arg);
		}

		@Override
		public void visit(final ImportDeclaration n, final Boolean arg) {
			super.visit(n, arg);
			use(n.getName().toString());
		}
		
		public String use(String useMe) {
			if (useMe.endsWith("" + implementingClassName))
				return useMe;
			// if (useMe.endsWith(implementingClassName)) return useMe;

			if (uses.containsKey(useMe)) {
				return uses.get(useMe);
			}

			String cls = useMe.substring(useMe.lastIndexOf(".") + 1);
			if (uses.values().contains(cls)) {
				return useMe;
			}
			uses.put(useMe, cls);

			if (!useMe.startsWith("java.lang")) {
				imports.append("import " + useMe + ";\n");
			}
			return cls;
		}
		
		@Override
		public void visit(final ClassOrInterfaceDeclaration n, final Boolean arg) {

			classSimpleName = n.getName();
			implementingClassName = implementingClassPackage + "." + classSimpleName;

			head.append(
					"\n/** Use this accessor, but do not modify it, it was genereated by " + this.getClass().getName()
							+ ".\n*\n* This accessor filters for the tenant.\n*\n* Modify the accessor:  {@link class:"
							+ implementingClassName + "}\n*/\n\n");

			methods.append("public interface " + classSimpleName);

			if (null != n.getImplements() && n.getImplements().size() > 0) {
				methods.append(" extends ");
				int i;
				for (i = 0; i < n.getImplements().size() - 1; i++) {
					methods.append(" " + n.getImplements().get(i).getName() + ",");
				}
				methods.append(" " + n.getImplements().get(i).getName());
			}
			methods.append(" {\n\n");

			super.visit(n, arg);

			footer.append("}\n");
		}

		private AnnotationExpr getFirstMatchingAnnotation(List<AnnotationExpr> anno, String typeToFind) {
			if (anno == null)
				return null;
			for (AnnotationExpr a : anno) {
				if (a.getName().getName().equals(typeToFind)) {
					return a;
				}
			}
			return null;
		}
		

		@Override
		public void visit(MethodDeclaration m, final Boolean arg) {
			super.visit(m, arg);

			String returnType = m.getType().toString();

			methods.append("    /** Originally declared by {@link class:" + implementingClassName + "} */\n");
			methods.append("    public " + returnType + " " + m.getName() + "(");

			boolean hadTenantColumn = false;
			for (Parameter p : m.getParameters()) {
				String argName = p.getId().getName();

				AnnotationExpr pa = getFirstMatchingAnnotation(p.getAnnotations(), "Param");
				if (pa != null && (pa instanceof com.github.javaparser.ast.expr.SingleMemberAnnotationExpr)
						&& ((StringLiteralExpr) ((com.github.javaparser.ast.expr.SingleMemberAnnotationExpr) pa)
								.getMemberValue()).getValue().equals(tenantColumn)) {
					hadTenantColumn = true;
					continue;
				}

				methods.append(p.getType() + " " + argName + ", ");
			}
			if (!hadTenantColumn) {
				System.out.println("WARNING: method " + implementingClassName + "." + m.getName()
						+ "(...): Does not indicate the tenant");
			}
			if (methods.toString().endsWith(", "))
				methods.delete(methods.length() - 2, methods.length());
			methods.append("); \n");
		}
	}

	//FIXME we shouldn't be using exceptions for flow control
	public static class NoAccessorException extends RuntimeException {

		private static final long serialVersionUID = 1L;

	}
}
