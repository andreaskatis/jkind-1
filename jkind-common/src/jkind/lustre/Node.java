package jkind.lustre;

import java.util.List;

import jkind.Assert;
import jkind.lustre.visitors.AstVisitor;
import jkind.util.Util;

public class Node extends Ast {
	final public String id;
	final public List<VarDecl> inputs;
	final public List<VarDecl> outputs;
	final public List<VarDecl> locals;
	final public List<Equation> equations;
	final public List<String> properties;
	final public List<Expr> assertions;
	final public List<String> realizabilities;

	public Node(Location location, String id, List<VarDecl> inputs, List<VarDecl> outputs,
			List<VarDecl> locals, List<Equation> equations, List<String> properties,
			List<Expr> assertions, List<String> realizabilities) {
		super(location);
		Assert.isNotNull(id);
		this.id = id;
		this.inputs = Util.safeList(inputs);
		this.outputs = Util.safeList(outputs);
		this.locals = Util.safeList(locals);
		this.equations = Util.safeList(equations);
		this.properties = Util.safeList(properties);
		this.assertions = Util.safeList(assertions);
		this.realizabilities = Util.safeList(realizabilities);
	}

	public Node(String id, List<VarDecl> inputs, List<VarDecl> outputs, List<VarDecl> locals,
			List<Equation> equations, List<String> properties, List<Expr> assertions, List<String> realizabilities) {
		this(Location.NULL, id, inputs, outputs, locals, equations, properties, assertions, realizabilities);
	}

	public Node(String id, List<VarDecl> inputs, List<VarDecl> outputs, List<VarDecl> locals,
			List<Equation> equations, List<String> properties, List<Expr> assertions) {
		this(Location.NULL, id, inputs, outputs, locals, equations, properties, assertions, null);
	}
	
	public Node(String id, List<VarDecl> inputs, List<VarDecl> outputs, List<VarDecl> locals,
			List<Equation> equations, List<String> properties) {
		this(Location.NULL, id, inputs, outputs, locals, equations, properties, null, null);
	}
	
	public Node(String id, List<VarDecl> inputs, List<VarDecl> outputs, List<VarDecl> locals,
			List<Equation> equations) {
		this(Location.NULL, id, inputs, outputs, locals, equations, null, null, null);
	}

	@Override
	public <T, S extends T> T accept(AstVisitor<T, S> visitor) {
		return visitor.visit(this);
	}
}