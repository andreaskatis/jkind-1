package jkind.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jkind.lustre.Ast;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BoolExpr;
import jkind.lustre.Constant;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.ExprVisitor;
import jkind.lustre.IdExpr;
import jkind.lustre.IfThenElseExpr;
import jkind.lustre.IntExpr;
import jkind.lustre.Location;
import jkind.lustre.Node;
import jkind.lustre.NodeCallExpr;
import jkind.lustre.Program;
import jkind.lustre.RealExpr;
import jkind.lustre.SubrangeIntType;
import jkind.lustre.Type;
import jkind.lustre.TypeDef;
import jkind.lustre.UnaryExpr;
import jkind.lustre.VarDecl;
import jkind.util.Util;

public class TypeChecker implements ExprVisitor<Type> {
	private Map<String, Type> typeTable;
	private Map<String, Type> constantTable;
	private Map<String, Type> variableTable;
	private Map<String, Node> nodeTable;
	private boolean passed;

	public TypeChecker() {
		this.typeTable = new HashMap<String, Type>();
		this.constantTable = new HashMap<String, Type>();
		this.variableTable = new HashMap<String, Type>();
		this.nodeTable = new HashMap<String, Node>();
		this.passed = true;
	}

	public static boolean check(Program program) {
		return new TypeChecker().visitProgram(program);
	}

	public boolean visitProgram(Program program) {
		populateTypeTable(program.types);
		populateConstantTable(program.constants);
		nodeTable = Util.getNodeTable(program.nodes);

		for (Node node : program.nodes) {
			visitNode(node);
		}

		return passed;
	}

	private void populateTypeTable(List<TypeDef> typeDefs) {
		for (TypeDef def : typeDefs) {
			Type type = lookupBuiltinType(def.type);
			if (type == null) {
				error(def, "unknown type " + def.type);
			}
			typeTable.put(def.id, type);
		}
	}

	private void populateConstantTable(List<Constant> constants) {
		for (Constant c : constants) {
			Type actual = c.expr.accept(this);
			if (c.type == null) {
				constantTable.put(c.id, actual);
			} else {
				Type expected = lookupBuiltinType(c.type);
				compareTypeAssignment(c.expr, expected, actual);
				constantTable.put(c.id, expected);
			}
		}
	}

	public boolean visitNode(Node node) {
		repopulateVariableTable(node);

		for (Equation eq : node.equations) {
			if (eq.lhs.size() == 1) {
				checkSingleAssignment(eq);
			} else {
				checkNodeCallAssignment(eq);
			}
		}

		return passed;
	}

	private void repopulateVariableTable(Node node) {
		variableTable.clear();
		for (VarDecl v : Util.getVarDecls(node)) {
			Type type = lookupBuiltinType(v.type);
			if (type == null) {
				error(v, "unknown type " + v.type);
				type = null;
			}
			variableTable.put(v.id, type);
		}
	}

	private Type lookupBuiltinType(Type type) {
		if (type.isBuiltin()) {
			return type;
		} else if (typeTable.containsKey(type.name)) {
			return typeTable.get(type.name);
		} else {
			return null;
		}
	}

	private boolean isIntBased(Type type) {
		return type == Type.INT || type instanceof SubrangeIntType;
	}

	private void checkSingleAssignment(Equation eq) {
		Type expected = eq.lhs.get(0).accept(this);
		Type actual = eq.expr.accept(this);
		compareTypeAssignment(eq, expected, actual);
	}

	private void checkNodeCallAssignment(Equation eq) {
		if (eq.expr instanceof NodeCallExpr) {
			NodeCallExpr call = (NodeCallExpr) eq.expr;

			List<Type> expected = new ArrayList<Type>();
			for (IdExpr idExpr : eq.lhs) {
				expected.add(idExpr.accept(this));
			}

			List<Type> actual = visitNodeCallExpr(call);
			if (actual == null) {
				return;
			}

			if (expected.size() != actual.size()) {
				error(eq, "expected " + expected.size() + " values but found " + actual.size());
				return;
			}

			for (int i = 0; i < expected.size(); i++) {
				compareTypeAssignment(eq.lhs.get(i), expected.get(i), actual.get(i));
			}
		} else {
			error(eq.expr, "expected node call for multiple value assignment");
			return;
		}
	}

	@Override
	public Type visit(BinaryExpr e) {
		Type left = e.left.accept(this);
		Type right = e.right.accept(this);
		if (left == null || right == null) {
			return null;
		}

		switch (e.op) {
		case PLUS:
		case MINUS:
		case MULTIPLY:
			if (left == Type.REAL && right == Type.REAL) {
				return Type.REAL;
			}
			if (isIntBased(left) && isIntBased(right)) {
				return Type.INT;
			}
			break;

		case DIVIDE:
			if (left == Type.REAL && right == Type.REAL) {
				return Type.REAL;
			}
			break;

		case INT_DIVIDE:
			if (isIntBased(left) && isIntBased(right)) {
				return Type.INT;
			}
			break;

		case EQUAL:
		case NOTEQUAL:
			if (left == right) {
				return Type.BOOL;
			}
			if (isIntBased(left) && isIntBased(right)) {
				return Type.BOOL;
			}
			break;

		case GREATER:
		case LESS:
		case GREATEREQUAL:
		case LESSEQUAL:
			if (left == Type.REAL && right == Type.REAL) {
				return Type.BOOL;
			}
			if (isIntBased(left) && isIntBased(right)) {
				return Type.BOOL;
			}
			break;

		case OR:
		case AND:
		case XOR:
		case IMPLIES:
			if (left == Type.BOOL && right == Type.BOOL) {
				return Type.BOOL;
			}
			break;

		case ARROW:
			if (left == right) {
				return left;
			}
			if (isIntBased(left) && isIntBased(right)) {
				return joinTypes(left, right);
			}
			break;
		}

		error(e, "operator '" + e.op + "' not defined on types " + left + ", " + right);
		return null;
	}

	@Override
	public Type visit(BoolExpr e) {
		return Type.BOOL;
	}

	@Override
	public Type visit(IdExpr e) {
		if (variableTable.containsKey(e.id)) {
			return variableTable.get(e.id);
		} else if (constantTable.containsKey(e.id)) {
			return constantTable.get(e.id);
		} else {
			error(e, "unknown variable " + e.id);
			return null;
		}
	}

	@Override
	public Type visit(IfThenElseExpr e) {
		Type condType = e.cond.accept(this);
		Type thenType = e.thenExpr.accept(this);
		Type elseType = e.elseExpr.accept(this);

		compareTypeAssignment(e.cond, Type.BOOL, condType);
		return compareTypeJoin(e, thenType, elseType);
	}

	@Override
	public Type visit(IntExpr e) {
		return new SubrangeIntType(Location.NULL, e.value, e.value);
	}

	@Override
	public Type visit(NodeCallExpr e) {
		List<Type> result = visitNodeCallExpr(e);

		if (result == null) {
			return null;
		} else if (result.size() == 1) {
			return result.get(0);
		} else {
			error(e, "node returns multiple values");
			return null;
		}
	}

	public List<Type> visitNodeCallExpr(NodeCallExpr e) {
		Node node = nodeTable.get(e.node);
		if (node == null) {
			error(e, "unknown node " + e.node);
			return null;
		}

		List<Type> actual = new ArrayList<Type>();
		for (Expr arg : e.args) {
			actual.add(arg.accept(this));
		}

		List<Type> expected = new ArrayList<Type>();
		for (VarDecl input : node.inputs) {
			expected.add(lookupBuiltinType(input.type));
		}

		if (actual.size() != expected.size()) {
			error(e, "expected " + expected.size() + " arguments, but found " + actual.size());
			return null;
		}

		for (int i = 0; i < expected.size(); i++) {
			compareTypeAssignment(e.args.get(i), expected.get(i), actual.get(i));
		}

		List<Type> result = new ArrayList<Type>();
		for (VarDecl decl : node.outputs) {
			result.add(lookupBuiltinType(decl.type));
		}
		return result;
	}

	@Override
	public Type visit(RealExpr e) {
		return Type.REAL;
	}

	@Override
	public Type visit(UnaryExpr e) {
		Type type = e.expr.accept(this);
		if (type == null) {
			return null;
		}

		switch (e.op) {
		case NEGATIVE:
			if (isIntBased(type)) {
				return Type.INT;
			}
			if (type == Type.REAL) {
				return Type.REAL;
			}
			break;

		case NOT:
			if (type == Type.BOOL) {
				return type;
			}
			break;

		case PRE:
			return type;
		}

		error(e, "operator '" + e.op + "' not defined on type " + type);
		return null;
	}

	private void compareTypeAssignment(Ast ast, Type expected, Type actual) {
		if (expected == null || actual == null) {
			return;
		}

		if (!typeAssignable(expected, actual)) {
			error(ast, "expected type " + expected + " but found type " + actual);
		}
	}
	
	private boolean typeAssignable(Type expected, Type actual) {
		if (expected == actual) {
			return true;
		}

		if (expected == Type.INT && actual instanceof SubrangeIntType) {
			return true;
		}

		if (expected instanceof SubrangeIntType && actual instanceof SubrangeIntType) {
			SubrangeIntType exRange = (SubrangeIntType) expected;
			SubrangeIntType acRange = (SubrangeIntType) actual;
			return exRange.low.compareTo(acRange.low) <= 0
					&& exRange.high.compareTo(acRange.high) >= 0;
		}

		return false;
	}
	
	private Type compareTypeJoin(Ast ast, Type t1, Type t2) {
		if (t1 == null || t2 == null) {
			return null;
		}

		Type join = joinTypes(t1, t2);
		if (join == null) {
			error(ast, "cannot join types " + t1 + " and " + t2);
			return null;
		}
		return join;
	}

	private Type joinTypes(Type t1, Type t2) {
		if (t1 instanceof SubrangeIntType && t2 instanceof SubrangeIntType) {
			SubrangeIntType t1range = (SubrangeIntType) t1;
			SubrangeIntType t2range = (SubrangeIntType) t2;
			return new SubrangeIntType(Location.NULL, t1range.low.min(t2range.low),
					t1range.high.max(t2range.high));
		} else if (isIntBased(t1) && isIntBased(t2)) {
			return Type.INT;
		} else if (t1 == t2) {
			return t1;
		} else {
			return null;
		}
	}

	private void error(Ast ast, String message) {
		passed = false;
		System.out.println("Type error at line " + ast.location + " " + message);
	}
}
