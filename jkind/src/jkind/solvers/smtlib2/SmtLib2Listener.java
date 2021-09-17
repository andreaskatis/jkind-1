// Generated from /home/akatis/git/jkind-1/jkind/src/jkind/solvers/smtlib2/SmtLib2.g4 by ANTLR 4.4
package jkind.solvers.smtlib2;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link SmtLib2Parser}.
 */
public interface SmtLib2Listener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by the {@code letBody}
	 * labeled alternative in {@link SmtLib2Parser#body}.
	 * @param ctx the parse tree
	 */
	void enterLetBody(@NotNull SmtLib2Parser.LetBodyContext ctx);
	/**
	 * Exit a parse tree produced by the {@code letBody}
	 * labeled alternative in {@link SmtLib2Parser#body}.
	 * @param ctx the parse tree
	 */
	void exitLetBody(@NotNull SmtLib2Parser.LetBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SmtLib2Parser#symbol}.
	 * @param ctx the parse tree
	 */
	void enterSymbol(@NotNull SmtLib2Parser.SymbolContext ctx);
	/**
	 * Exit a parse tree produced by {@link SmtLib2Parser#symbol}.
	 * @param ctx the parse tree
	 */
	void exitSymbol(@NotNull SmtLib2Parser.SymbolContext ctx);
	/**
	 * Enter a parse tree produced by the {@code consBody}
	 * labeled alternative in {@link SmtLib2Parser#body}.
	 * @param ctx the parse tree
	 */
	void enterConsBody(@NotNull SmtLib2Parser.ConsBodyContext ctx);
	/**
	 * Exit a parse tree produced by the {@code consBody}
	 * labeled alternative in {@link SmtLib2Parser#body}.
	 * @param ctx the parse tree
	 */
	void exitConsBody(@NotNull SmtLib2Parser.ConsBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SmtLib2Parser#arg}.
	 * @param ctx the parse tree
	 */
	void enterArg(@NotNull SmtLib2Parser.ArgContext ctx);
	/**
	 * Exit a parse tree produced by {@link SmtLib2Parser#arg}.
	 * @param ctx the parse tree
	 */
	void exitArg(@NotNull SmtLib2Parser.ArgContext ctx);
	/**
	 * Enter a parse tree produced by {@link SmtLib2Parser#define}.
	 * @param ctx the parse tree
	 */
	void enterDefine(@NotNull SmtLib2Parser.DefineContext ctx);
	/**
	 * Exit a parse tree produced by {@link SmtLib2Parser#define}.
	 * @param ctx the parse tree
	 */
	void exitDefine(@NotNull SmtLib2Parser.DefineContext ctx);
	/**
	 * Enter a parse tree produced by {@link SmtLib2Parser#fn}.
	 * @param ctx the parse tree
	 */
	void enterFn(@NotNull SmtLib2Parser.FnContext ctx);
	/**
	 * Exit a parse tree produced by {@link SmtLib2Parser#fn}.
	 * @param ctx the parse tree
	 */
	void exitFn(@NotNull SmtLib2Parser.FnContext ctx);
	/**
	 * Enter a parse tree produced by {@link SmtLib2Parser#binding}.
	 * @param ctx the parse tree
	 */
	void enterBinding(@NotNull SmtLib2Parser.BindingContext ctx);
	/**
	 * Exit a parse tree produced by {@link SmtLib2Parser#binding}.
	 * @param ctx the parse tree
	 */
	void exitBinding(@NotNull SmtLib2Parser.BindingContext ctx);
	/**
	 * Enter a parse tree produced by {@link SmtLib2Parser#model}.
	 * @param ctx the parse tree
	 */
	void enterModel(@NotNull SmtLib2Parser.ModelContext ctx);
	/**
	 * Exit a parse tree produced by {@link SmtLib2Parser#model}.
	 * @param ctx the parse tree
	 */
	void exitModel(@NotNull SmtLib2Parser.ModelContext ctx);
	/**
	 * Enter a parse tree produced by {@link SmtLib2Parser#id}.
	 * @param ctx the parse tree
	 */
	void enterId(@NotNull SmtLib2Parser.IdContext ctx);
	/**
	 * Exit a parse tree produced by {@link SmtLib2Parser#id}.
	 * @param ctx the parse tree
	 */
	void exitId(@NotNull SmtLib2Parser.IdContext ctx);
	/**
	 * Enter a parse tree produced by {@link SmtLib2Parser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(@NotNull SmtLib2Parser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SmtLib2Parser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(@NotNull SmtLib2Parser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SmtLib2Parser#qid}.
	 * @param ctx the parse tree
	 */
	void enterQid(@NotNull SmtLib2Parser.QidContext ctx);
	/**
	 * Exit a parse tree produced by {@link SmtLib2Parser#qid}.
	 * @param ctx the parse tree
	 */
	void exitQid(@NotNull SmtLib2Parser.QidContext ctx);
	/**
	 * Enter a parse tree produced by the {@code symbolBody}
	 * labeled alternative in {@link SmtLib2Parser#body}.
	 * @param ctx the parse tree
	 */
	void enterSymbolBody(@NotNull SmtLib2Parser.SymbolBodyContext ctx);
	/**
	 * Exit a parse tree produced by the {@code symbolBody}
	 * labeled alternative in {@link SmtLib2Parser#body}.
	 * @param ctx the parse tree
	 */
	void exitSymbolBody(@NotNull SmtLib2Parser.SymbolBodyContext ctx);
}