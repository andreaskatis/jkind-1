// Generated from /home/akatis/git/jkindtemp/jkind-1/jkind/src/jkind/solvers/smtlib2/SmtLib2.g4 by ANTLR 4.4
package jkind.solvers.smtlib2;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class SmtLib2Parser extends Parser {
	static { RuntimeMetaData.checkVersion("4.4", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__25=1, T__24=2, T__23=3, T__22=4, T__21=5, T__20=6, T__19=7, T__18=8, 
		T__17=9, T__16=10, T__15=11, T__14=12, T__13=13, T__12=14, T__11=15, T__10=16, 
		T__9=17, T__8=18, T__7=19, T__6=20, T__5=21, T__4=22, T__3=23, T__2=24, 
		T__1=25, T__0=26, BOOL=27, INT=28, REAL=29, ID=30, WS=31, ERROR=32;
	public static final String[] tokenNames = {
		"<INVALID>", "'/'", "'to_real'", "'Bool'", "'='", "'<='", "'('", "'*'", 
		"'to_int'", "'ite'", "'define-fun'", "'Real'", "'Int'", "'mod'", "'>='", 
		"'|'", "'<'", "'>'", "'or'", "'=>'", "'let'", "'div'", "')'", "'and'", 
		"'+'", "'not'", "'-'", "BOOL", "INT", "REAL", "ID", "WS", "ERROR"
	};
	public static final int
		RULE_model = 0, RULE_define = 1, RULE_arg = 2, RULE_type = 3, RULE_body = 4, 
		RULE_binding = 5, RULE_fn = 6, RULE_symbol = 7, RULE_id = 8, RULE_qid = 9;
	public static final String[] ruleNames = {
		"model", "define", "arg", "type", "body", "binding", "fn", "symbol", "id", 
		"qid"
	};

	@Override
	public String getGrammarFileName() { return "SmtLib2.g4"; }

	@Override
	public String[] getTokenNames() { return tokenNames; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public SmtLib2Parser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class ModelContext extends ParserRuleContext {
		public DefineContext define(int i) {
			return getRuleContext(DefineContext.class,i);
		}
		public List<DefineContext> define() {
			return getRuleContexts(DefineContext.class);
		}
		public TerminalNode EOF() { return getToken(SmtLib2Parser.EOF, 0); }
		public ModelContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_model; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SmtLib2Listener ) ((SmtLib2Listener)listener).enterModel(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SmtLib2Listener ) ((SmtLib2Listener)listener).exitModel(this);
		}
	}

	public final ModelContext model() throws RecognitionException {
		ModelContext _localctx = new ModelContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_model);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(20); match(T__20);
			setState(24);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__20) {
				{
				{
				setState(21); define();
				}
				}
				setState(26);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(27); match(T__4);
			setState(28); match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class DefineContext extends ParserRuleContext {
		public ArgContext arg(int i) {
			return getRuleContext(ArgContext.class,i);
		}
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public BodyContext body() {
			return getRuleContext(BodyContext.class,0);
		}
		public List<ArgContext> arg() {
			return getRuleContexts(ArgContext.class);
		}
		public DefineContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_define; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SmtLib2Listener ) ((SmtLib2Listener)listener).enterDefine(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SmtLib2Listener ) ((SmtLib2Listener)listener).exitDefine(this);
		}
	}

	public final DefineContext define() throws RecognitionException {
		DefineContext _localctx = new DefineContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_define);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(30); match(T__20);
			setState(31); match(T__16);
			setState(32); id();
			setState(33); match(T__20);
			setState(37);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__20) {
				{
				{
				setState(34); arg();
				}
				}
				setState(39);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(40); match(T__4);
			setState(41); type();
			setState(42); body();
			setState(43); match(T__4);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArgContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public ArgContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arg; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SmtLib2Listener ) ((SmtLib2Listener)listener).enterArg(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SmtLib2Listener ) ((SmtLib2Listener)listener).exitArg(this);
		}
	}

	public final ArgContext arg() throws RecognitionException {
		ArgContext _localctx = new ArgContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_arg);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(45); match(T__20);
			setState(46); id();
			setState(47); type();
			setState(48); match(T__4);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeContext extends ParserRuleContext {
		public TypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SmtLib2Listener ) ((SmtLib2Listener)listener).enterType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SmtLib2Listener ) ((SmtLib2Listener)listener).exitType(this);
		}
	}

	public final TypeContext type() throws RecognitionException {
		TypeContext _localctx = new TypeContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_type);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(50);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__23) | (1L << T__15) | (1L << T__14))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			consume();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BodyContext extends ParserRuleContext {
		public BodyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_body; }
	 
		public BodyContext() { }
		public void copyFrom(BodyContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class LetBodyContext extends BodyContext {
		public List<BindingContext> binding() {
			return getRuleContexts(BindingContext.class);
		}
		public BodyContext body() {
			return getRuleContext(BodyContext.class,0);
		}
		public BindingContext binding(int i) {
			return getRuleContext(BindingContext.class,i);
		}
		public LetBodyContext(BodyContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SmtLib2Listener ) ((SmtLib2Listener)listener).enterLetBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SmtLib2Listener ) ((SmtLib2Listener)listener).exitLetBody(this);
		}
	}
	public static class ConsBodyContext extends BodyContext {
		public BodyContext body(int i) {
			return getRuleContext(BodyContext.class,i);
		}
		public FnContext fn() {
			return getRuleContext(FnContext.class,0);
		}
		public List<BodyContext> body() {
			return getRuleContexts(BodyContext.class);
		}
		public ConsBodyContext(BodyContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SmtLib2Listener ) ((SmtLib2Listener)listener).enterConsBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SmtLib2Listener ) ((SmtLib2Listener)listener).exitConsBody(this);
		}
	}
	public static class SymbolBodyContext extends BodyContext {
		public SymbolContext symbol() {
			return getRuleContext(SymbolContext.class,0);
		}
		public SymbolBodyContext(BodyContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SmtLib2Listener ) ((SmtLib2Listener)listener).enterSymbolBody(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SmtLib2Listener ) ((SmtLib2Listener)listener).exitSymbolBody(this);
		}
	}

	public final BodyContext body() throws RecognitionException {
		BodyContext _localctx = new BodyContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_body);
		int _la;
		try {
			setState(76);
			switch ( getInterpreter().adaptivePredict(_input,4,_ctx) ) {
			case 1:
				_localctx = new SymbolBodyContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(52); symbol();
				}
				break;
			case 2:
				_localctx = new ConsBodyContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(53); match(T__20);
				setState(54); fn();
				setState(58);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__20) | (1L << T__11) | (1L << BOOL) | (1L << INT) | (1L << REAL) | (1L << ID))) != 0)) {
					{
					{
					setState(55); body();
					}
					}
					setState(60);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(61); match(T__4);
				}
				break;
			case 3:
				_localctx = new LetBodyContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(63); match(T__20);
				setState(64); match(T__6);
				setState(65); match(T__20);
				setState(69);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__20) {
					{
					{
					setState(66); binding();
					}
					}
					setState(71);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(72); match(T__4);
				setState(73); body();
				setState(74); match(T__4);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BindingContext extends ParserRuleContext {
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public BodyContext body() {
			return getRuleContext(BodyContext.class,0);
		}
		public BindingContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_binding; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SmtLib2Listener ) ((SmtLib2Listener)listener).enterBinding(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SmtLib2Listener ) ((SmtLib2Listener)listener).exitBinding(this);
		}
	}

	public final BindingContext binding() throws RecognitionException {
		BindingContext _localctx = new BindingContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_binding);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(78); match(T__20);
			setState(79); id();
			setState(80); body();
			setState(81); match(T__4);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FnContext extends ParserRuleContext {
		public FnContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fn; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SmtLib2Listener ) ((SmtLib2Listener)listener).enterFn(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SmtLib2Listener ) ((SmtLib2Listener)listener).exitFn(this);
		}
	}

	public final FnContext fn() throws RecognitionException {
		FnContext _localctx = new FnContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_fn);
		try {
			setState(102);
			switch (_input.LA(1)) {
			case T__22:
				enterOuterAlt(_localctx, 1);
				{
				setState(83); match(T__22);
				}
				break;
			case T__2:
				enterOuterAlt(_localctx, 2);
				{
				setState(84); match(T__2);
				}
				break;
			case T__19:
				enterOuterAlt(_localctx, 3);
				{
				setState(85); match(T__19);
				}
				break;
			case T__0:
				enterOuterAlt(_localctx, 4);
				{
				setState(86); match(T__0);
				}
				break;
			case T__25:
				enterOuterAlt(_localctx, 5);
				{
				setState(87); match(T__25);
				}
				break;
			case T__5:
				enterOuterAlt(_localctx, 6);
				{
				setState(88); match(T__5);
				}
				break;
			case T__13:
				enterOuterAlt(_localctx, 7);
				{
				setState(89); match(T__13);
				}
				break;
			case T__3:
				enterOuterAlt(_localctx, 8);
				{
				setState(90); match(T__3);
				}
				break;
			case T__8:
				enterOuterAlt(_localctx, 9);
				{
				setState(91); match(T__8);
				}
				break;
			case T__1:
				enterOuterAlt(_localctx, 10);
				{
				setState(92); match(T__1);
				}
				break;
			case T__7:
				enterOuterAlt(_localctx, 11);
				{
				setState(93); match(T__7);
				}
				break;
			case T__17:
				enterOuterAlt(_localctx, 12);
				{
				setState(94); match(T__17);
				}
				break;
			case T__20:
			case T__11:
			case T__4:
			case BOOL:
			case INT:
			case REAL:
			case ID:
				enterOuterAlt(_localctx, 13);
				{
				}
				break;
			case T__12:
				enterOuterAlt(_localctx, 14);
				{
				setState(96); match(T__12);
				}
				break;
			case T__21:
				enterOuterAlt(_localctx, 15);
				{
				setState(97); match(T__21);
				}
				break;
			case T__10:
				enterOuterAlt(_localctx, 16);
				{
				setState(98); match(T__10);
				}
				break;
			case T__9:
				enterOuterAlt(_localctx, 17);
				{
				setState(99); match(T__9);
				}
				break;
			case T__24:
				enterOuterAlt(_localctx, 18);
				{
				setState(100); match(T__24);
				}
				break;
			case T__18:
				enterOuterAlt(_localctx, 19);
				{
				setState(101); match(T__18);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SymbolContext extends ParserRuleContext {
		public TerminalNode BOOL() { return getToken(SmtLib2Parser.BOOL, 0); }
		public TerminalNode REAL() { return getToken(SmtLib2Parser.REAL, 0); }
		public IdContext id() {
			return getRuleContext(IdContext.class,0);
		}
		public TerminalNode INT() { return getToken(SmtLib2Parser.INT, 0); }
		public SymbolContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_symbol; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SmtLib2Listener ) ((SmtLib2Listener)listener).enterSymbol(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SmtLib2Listener ) ((SmtLib2Listener)listener).exitSymbol(this);
		}
	}

	public final SymbolContext symbol() throws RecognitionException {
		SymbolContext _localctx = new SymbolContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_symbol);
		try {
			setState(108);
			switch (_input.LA(1)) {
			case T__11:
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(104); id();
				}
				break;
			case BOOL:
				enterOuterAlt(_localctx, 2);
				{
				setState(105); match(BOOL);
				}
				break;
			case INT:
				enterOuterAlt(_localctx, 3);
				{
				setState(106); match(INT);
				}
				break;
			case REAL:
				enterOuterAlt(_localctx, 4);
				{
				setState(107); match(REAL);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class IdContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(SmtLib2Parser.ID, 0); }
		public QidContext qid() {
			return getRuleContext(QidContext.class,0);
		}
		public IdContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_id; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SmtLib2Listener ) ((SmtLib2Listener)listener).enterId(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SmtLib2Listener ) ((SmtLib2Listener)listener).exitId(this);
		}
	}

	public final IdContext id() throws RecognitionException {
		IdContext _localctx = new IdContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_id);
		try {
			setState(112);
			switch (_input.LA(1)) {
			case T__11:
				enterOuterAlt(_localctx, 1);
				{
				setState(110); qid();
				}
				break;
			case ID:
				enterOuterAlt(_localctx, 2);
				{
				setState(111); match(ID);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class QidContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(SmtLib2Parser.ID, 0); }
		public QidContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_qid; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof SmtLib2Listener ) ((SmtLib2Listener)listener).enterQid(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof SmtLib2Listener ) ((SmtLib2Listener)listener).exitQid(this);
		}
	}

	public final QidContext qid() throws RecognitionException {
		QidContext _localctx = new QidContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_qid);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(114); match(T__11);
			setState(115); match(ID);
			setState(116); match(T__11);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3\"y\4\2\t\2\4\3\t"+
		"\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t\13\3"+
		"\2\3\2\7\2\31\n\2\f\2\16\2\34\13\2\3\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\7\3"+
		"&\n\3\f\3\16\3)\13\3\3\3\3\3\3\3\3\3\3\3\3\4\3\4\3\4\3\4\3\4\3\5\3\5\3"+
		"\6\3\6\3\6\3\6\7\6;\n\6\f\6\16\6>\13\6\3\6\3\6\3\6\3\6\3\6\3\6\7\6F\n"+
		"\6\f\6\16\6I\13\6\3\6\3\6\3\6\3\6\5\6O\n\6\3\7\3\7\3\7\3\7\3\7\3\b\3\b"+
		"\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\5"+
		"\bi\n\b\3\t\3\t\3\t\3\t\5\to\n\t\3\n\3\n\5\ns\n\n\3\13\3\13\3\13\3\13"+
		"\3\13\2\2\f\2\4\6\b\n\f\16\20\22\24\2\3\4\2\5\5\r\16\u008a\2\26\3\2\2"+
		"\2\4 \3\2\2\2\6/\3\2\2\2\b\64\3\2\2\2\nN\3\2\2\2\fP\3\2\2\2\16h\3\2\2"+
		"\2\20n\3\2\2\2\22r\3\2\2\2\24t\3\2\2\2\26\32\7\b\2\2\27\31\5\4\3\2\30"+
		"\27\3\2\2\2\31\34\3\2\2\2\32\30\3\2\2\2\32\33\3\2\2\2\33\35\3\2\2\2\34"+
		"\32\3\2\2\2\35\36\7\30\2\2\36\37\7\2\2\3\37\3\3\2\2\2 !\7\b\2\2!\"\7\f"+
		"\2\2\"#\5\22\n\2#\'\7\b\2\2$&\5\6\4\2%$\3\2\2\2&)\3\2\2\2\'%\3\2\2\2\'"+
		"(\3\2\2\2(*\3\2\2\2)\'\3\2\2\2*+\7\30\2\2+,\5\b\5\2,-\5\n\6\2-.\7\30\2"+
		"\2.\5\3\2\2\2/\60\7\b\2\2\60\61\5\22\n\2\61\62\5\b\5\2\62\63\7\30\2\2"+
		"\63\7\3\2\2\2\64\65\t\2\2\2\65\t\3\2\2\2\66O\5\20\t\2\678\7\b\2\28<\5"+
		"\16\b\29;\5\n\6\2:9\3\2\2\2;>\3\2\2\2<:\3\2\2\2<=\3\2\2\2=?\3\2\2\2><"+
		"\3\2\2\2?@\7\30\2\2@O\3\2\2\2AB\7\b\2\2BC\7\26\2\2CG\7\b\2\2DF\5\f\7\2"+
		"ED\3\2\2\2FI\3\2\2\2GE\3\2\2\2GH\3\2\2\2HJ\3\2\2\2IG\3\2\2\2JK\7\30\2"+
		"\2KL\5\n\6\2LM\7\30\2\2MO\3\2\2\2N\66\3\2\2\2N\67\3\2\2\2NA\3\2\2\2O\13"+
		"\3\2\2\2PQ\7\b\2\2QR\5\22\n\2RS\5\n\6\2ST\7\30\2\2T\r\3\2\2\2Ui\7\6\2"+
		"\2Vi\7\32\2\2Wi\7\t\2\2Xi\7\34\2\2Yi\7\3\2\2Zi\7\27\2\2[i\7\17\2\2\\i"+
		"\7\31\2\2]i\7\24\2\2^i\7\33\2\2_i\7\25\2\2`i\7\13\2\2ai\3\2\2\2bi\7\20"+
		"\2\2ci\7\7\2\2di\7\22\2\2ei\7\23\2\2fi\7\4\2\2gi\7\n\2\2hU\3\2\2\2hV\3"+
		"\2\2\2hW\3\2\2\2hX\3\2\2\2hY\3\2\2\2hZ\3\2\2\2h[\3\2\2\2h\\\3\2\2\2h]"+
		"\3\2\2\2h^\3\2\2\2h_\3\2\2\2h`\3\2\2\2ha\3\2\2\2hb\3\2\2\2hc\3\2\2\2h"+
		"d\3\2\2\2he\3\2\2\2hf\3\2\2\2hg\3\2\2\2i\17\3\2\2\2jo\5\22\n\2ko\7\35"+
		"\2\2lo\7\36\2\2mo\7\37\2\2nj\3\2\2\2nk\3\2\2\2nl\3\2\2\2nm\3\2\2\2o\21"+
		"\3\2\2\2ps\5\24\13\2qs\7 \2\2rp\3\2\2\2rq\3\2\2\2s\23\3\2\2\2tu\7\21\2"+
		"\2uv\7 \2\2vw\7\21\2\2w\25\3\2\2\2\n\32\'<GNhnr";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}