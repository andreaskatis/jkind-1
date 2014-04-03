package jkind.translation.compound;

import java.util.ArrayList;
import java.util.List;

import jkind.analysis.TypeChecker;
import jkind.lustre.ArrayType;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.Node;
import jkind.lustre.RecordType;
import jkind.lustre.Type;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.visitors.ExprMapVisitor;

/**
 * Expand equalities and inequalities on records and arrays
 */
public class FlattenCompoundComparisons extends ExprMapVisitor {
	public static Node node(Node node) {
		return new FlattenCompoundComparisons().visitNode(node);
	}

	private final TypeChecker typeChecker = new TypeChecker();

	private Node visitNode(Node node) {
		typeChecker.repopulateVariableTable(node);
		List<Equation> equations = visitEquations(node.equations);
		List<Expr> assertions = visitAll(node.assertions);
		return new Node(node.id, node.inputs, node.outputs, node.locals, equations,
				node.properties, assertions);
	}

	private List<Equation> visitEquations(List<Equation> equations) {
		List<Equation> results = new ArrayList<>();
		for (Equation eq : equations) {
			results.add(new Equation(eq.location, eq.lhs, eq.expr.accept(this)));
		}
		return results;
	}

	@Override
	public Expr visit(BinaryExpr e) {
		Expr left = e.left.accept(this);
		Expr right = e.right.accept(this);
		if (e.op == BinaryOp.EQUAL || e.op == BinaryOp.NOTEQUAL) {
			Type type = getType(e.left);
			if (type instanceof ArrayType || type instanceof RecordType) {
				List<ExprType> leftExprTypes = CompoundUtil.flattenExpr(left, type);
				List<ExprType> rightExprTypes = CompoundUtil.flattenExpr(right, type);

				List<Expr> leftExprs = CompoundUtil.mapExprs(leftExprTypes);
				List<Expr> rightExprs = CompoundUtil.mapExprs(rightExprTypes);

				List<Expr> exprs = CompoundUtil.mapBinary(BinaryOp.EQUAL, leftExprs, rightExprs);
				Expr equal = CompoundUtil.conjoin(exprs);
				if (e.op == BinaryOp.EQUAL) {
					return equal;
				} else {
					return new UnaryExpr(UnaryOp.NOT, equal);
				}
			}
		}

		return new BinaryExpr(e.location, left, e.op, right);
	}

	/*
	 * We need type information to decompose equality and inequality for
	 * compound types. We do this by re-invoking the type checker. If we later
	 * run in to performance problems we can think about caching type
	 * information instead.
	 */
	private Type getType(Expr e) {
		return e.accept(typeChecker);
	}
}