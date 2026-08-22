// Generated from Cobol85Preprocessor.g4 by ANTLR 4.13.2
package io.proleap.benchmark.antlr;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class Cobol85PreprocessorParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		ADATA=1, ADV=2, ALIAS=3, ANSI=4, ANY=5, APOST=6, AR=7, ARITH=8, AUTO=9, 
		AWO=10, BIN=11, BLOCK0=12, BUF=13, BUFSIZE=14, BY=15, CBL=16, CBLCARD=17, 
		CICS=18, CO=19, COBOL2=20, COBOL3=21, CODEPAGE=22, COMPAT=23, COMPILE=24, 
		COPY=25, CP=26, CPP=27, CPSM=28, CS=29, CURR=30, CURRENCY=31, DATA=32, 
		DATEPROC=33, DBCS=34, DD=35, DEBUG=36, DECK=37, DIAGTRUNC=38, DLI=39, 
		DLL=40, DP=41, DTR=42, DU=43, DUMP=44, DYN=45, DYNAM=46, EDF=47, EJECT=48, 
		EJPD=49, EN=50, ENGLISH=51, END_EXEC=52, EPILOG=53, EXCI=54, EXEC=55, 
		EXIT=56, EXP=57, EXPORTALL=58, EXTEND=59, FASTSRT=60, FEPI=61, FLAG=62, 
		FLAGSTD=63, FSRT=64, FULL=65, GDS=66, GRAPHIC=67, HOOK=68, IN=69, INTDATE=70, 
		JA=71, JP=72, KA=73, LANG=74, LANGUAGE=75, LC=76, LEASM=77, LENGTH=78, 
		LIB=79, LILIAN=80, LIN=81, LINECOUNT=82, LINKAGE=83, LIST=84, LM=85, LONGMIXED=86, 
		LONGUPPER=87, LPARENCHAR=88, LU=89, MAP=90, MARGINS=91, MAX=92, MD=93, 
		MDECK=94, MIG=95, MIXED=96, NAME=97, NAT=98, NATIONAL=99, NATLANG=100, 
		NN=101, NO=102, NOADATA=103, NOADV=104, NOALIAS=105, NOAWO=106, NOBLOCK0=107, 
		NOC=108, NOCBLCARD=109, NOCICS=110, NOCMPR2=111, NOCOMPILE=112, NOCPSM=113, 
		NOCURR=114, NOCURRENCY=115, NOD=116, NODATEPROC=117, NODBCS=118, NODE=119, 
		NODEBUG=120, NODECK=121, NODIAGTRUNC=122, NODLL=123, NODU=124, NODUMP=125, 
		NODP=126, NODTR=127, NODYN=128, NODYNAM=129, NOEDF=130, NOEJPD=131, NOEPILOG=132, 
		NOEXIT=133, NOEXP=134, NOEXPORTALL=135, NOF=136, NOFASTSRT=137, NOFEPI=138, 
		NOFLAG=139, NOFLAGMIG=140, NOFLAGSTD=141, NOFSRT=142, NOGRAPHIC=143, NOHOOK=144, 
		NOLENGTH=145, NOLIB=146, NOLINKAGE=147, NOLIST=148, NOMAP=149, NOMD=150, 
		NOMDECK=151, NONAME=152, NONUM=153, NONUMBER=154, NOOBJ=155, NOOBJECT=156, 
		NOOFF=157, NOOFFSET=158, NOOPSEQUENCE=159, NOOPT=160, NOOPTIMIZE=161, 
		NOOPTIONS=162, NOP=163, NOPFD=164, NOPROLOG=165, NORENT=166, NOS=167, 
		NOSEP=168, NOSEPARATE=169, NOSEQ=170, NOSOURCE=171, NOSPIE=172, NOSQL=173, 
		NOSQLC=174, NOSQLCCSID=175, NOSSR=176, NOSSRANGE=177, NOSTDTRUNC=178, 
		NOSEQUENCE=179, NOTERM=180, NOTERMINAL=181, NOTEST=182, NOTHREAD=183, 
		NOTRIG=184, NOVBREF=185, NOWD=186, NOWORD=187, NOX=188, NOXREF=189, NOZWB=190, 
		NS=191, NSEQ=192, NSYMBOL=193, NUM=194, NUMBER=195, NUMPROC=196, OBJ=197, 
		OBJECT=198, OF=199, OFF=200, OFFSET=201, ON=202, OP=203, OPMARGINS=204, 
		OPSEQUENCE=205, OPT=206, OPTFILE=207, OPTIMIZE=208, OPTIONS=209, OUT=210, 
		OUTDD=211, PFD=212, PPTDBG=213, PGMN=214, PGMNAME=215, PROCESS=216, PROLOG=217, 
		QUOTE=218, RENT=219, REPLACE=220, REPLACING=221, RMODE=222, RPARENCHAR=223, 
		SEP=224, SEPARATE=225, SEQ=226, SEQUENCE=227, SHORT=228, SIZE=229, SOURCE=230, 
		SP=231, SPACE=232, SPIE=233, SQL=234, SQLC=235, SQLCCSID=236, SQLIMS=237, 
		SKIP1=238, SKIP2=239, SKIP3=240, SS=241, SSR=242, SSRANGE=243, STD=244, 
		SUPPRESS=245, SYSEIB=246, SZ=247, TERM=248, TERMINAL=249, TEST=250, THREAD=251, 
		TITLE=252, TRIG=253, TRUNC=254, UE=255, UPPER=256, VBREF=257, WD=258, 
		WORD=259, XMLPARSE=260, XMLSS=261, XOPTS=262, XP=263, XREF=264, YEARWINDOW=265, 
		YW=266, ZWB=267, C_CHAR=268, D_CHAR=269, E_CHAR=270, F_CHAR=271, H_CHAR=272, 
		I_CHAR=273, M_CHAR=274, N_CHAR=275, Q_CHAR=276, S_CHAR=277, U_CHAR=278, 
		W_CHAR=279, X_CHAR=280, COMMENTTAG=281, COMMACHAR=282, DOT=283, DOUBLEEQUALCHAR=284, 
		NONNUMERICLITERAL=285, NUMERICLITERAL=286, IDENTIFIER=287, FILENAME=288, 
		NEWLINE=289, COMMENTLINE=290, WS=291, TEXT=292;
	public static final int
		RULE_startRule = 0, RULE_compilerOptions = 1, RULE_compilerXOpts = 2, 
		RULE_compilerOption = 3, RULE_execCicsStatement = 4, RULE_execSqlStatement = 5, 
		RULE_execSqlImsStatement = 6, RULE_copyStatement = 7, RULE_copySource = 8, 
		RULE_copyLibrary = 9, RULE_replacingPhrase = 10, RULE_replaceArea = 11, 
		RULE_replaceByStatement = 12, RULE_replaceOffStatement = 13, RULE_replaceClause = 14, 
		RULE_directoryPhrase = 15, RULE_familyPhrase = 16, RULE_replaceable = 17, 
		RULE_replacement = 18, RULE_ejectStatement = 19, RULE_skipStatement = 20, 
		RULE_titleStatement = 21, RULE_pseudoText = 22, RULE_charData = 23, RULE_charDataSql = 24, 
		RULE_charDataLine = 25, RULE_cobolWord = 26, RULE_literal = 27, RULE_filename = 28, 
		RULE_charDataKeyword = 29;
	private static String[] makeRuleNames() {
		return new String[] {
			"startRule", "compilerOptions", "compilerXOpts", "compilerOption", "execCicsStatement", 
			"execSqlStatement", "execSqlImsStatement", "copyStatement", "copySource", 
			"copyLibrary", "replacingPhrase", "replaceArea", "replaceByStatement", 
			"replaceOffStatement", "replaceClause", "directoryPhrase", "familyPhrase", 
			"replaceable", "replacement", "ejectStatement", "skipStatement", "titleStatement", 
			"pseudoText", "charData", "charDataSql", "charDataLine", "cobolWord", 
			"literal", "filename", "charDataKeyword"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, "'('", null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, "')'", null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, "'*>'", "','", "'.'", "'=='"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "ADATA", "ADV", "ALIAS", "ANSI", "ANY", "APOST", "AR", "ARITH", 
			"AUTO", "AWO", "BIN", "BLOCK0", "BUF", "BUFSIZE", "BY", "CBL", "CBLCARD", 
			"CICS", "CO", "COBOL2", "COBOL3", "CODEPAGE", "COMPAT", "COMPILE", "COPY", 
			"CP", "CPP", "CPSM", "CS", "CURR", "CURRENCY", "DATA", "DATEPROC", "DBCS", 
			"DD", "DEBUG", "DECK", "DIAGTRUNC", "DLI", "DLL", "DP", "DTR", "DU", 
			"DUMP", "DYN", "DYNAM", "EDF", "EJECT", "EJPD", "EN", "ENGLISH", "END_EXEC", 
			"EPILOG", "EXCI", "EXEC", "EXIT", "EXP", "EXPORTALL", "EXTEND", "FASTSRT", 
			"FEPI", "FLAG", "FLAGSTD", "FSRT", "FULL", "GDS", "GRAPHIC", "HOOK", 
			"IN", "INTDATE", "JA", "JP", "KA", "LANG", "LANGUAGE", "LC", "LEASM", 
			"LENGTH", "LIB", "LILIAN", "LIN", "LINECOUNT", "LINKAGE", "LIST", "LM", 
			"LONGMIXED", "LONGUPPER", "LPARENCHAR", "LU", "MAP", "MARGINS", "MAX", 
			"MD", "MDECK", "MIG", "MIXED", "NAME", "NAT", "NATIONAL", "NATLANG", 
			"NN", "NO", "NOADATA", "NOADV", "NOALIAS", "NOAWO", "NOBLOCK0", "NOC", 
			"NOCBLCARD", "NOCICS", "NOCMPR2", "NOCOMPILE", "NOCPSM", "NOCURR", "NOCURRENCY", 
			"NOD", "NODATEPROC", "NODBCS", "NODE", "NODEBUG", "NODECK", "NODIAGTRUNC", 
			"NODLL", "NODU", "NODUMP", "NODP", "NODTR", "NODYN", "NODYNAM", "NOEDF", 
			"NOEJPD", "NOEPILOG", "NOEXIT", "NOEXP", "NOEXPORTALL", "NOF", "NOFASTSRT", 
			"NOFEPI", "NOFLAG", "NOFLAGMIG", "NOFLAGSTD", "NOFSRT", "NOGRAPHIC", 
			"NOHOOK", "NOLENGTH", "NOLIB", "NOLINKAGE", "NOLIST", "NOMAP", "NOMD", 
			"NOMDECK", "NONAME", "NONUM", "NONUMBER", "NOOBJ", "NOOBJECT", "NOOFF", 
			"NOOFFSET", "NOOPSEQUENCE", "NOOPT", "NOOPTIMIZE", "NOOPTIONS", "NOP", 
			"NOPFD", "NOPROLOG", "NORENT", "NOS", "NOSEP", "NOSEPARATE", "NOSEQ", 
			"NOSOURCE", "NOSPIE", "NOSQL", "NOSQLC", "NOSQLCCSID", "NOSSR", "NOSSRANGE", 
			"NOSTDTRUNC", "NOSEQUENCE", "NOTERM", "NOTERMINAL", "NOTEST", "NOTHREAD", 
			"NOTRIG", "NOVBREF", "NOWD", "NOWORD", "NOX", "NOXREF", "NOZWB", "NS", 
			"NSEQ", "NSYMBOL", "NUM", "NUMBER", "NUMPROC", "OBJ", "OBJECT", "OF", 
			"OFF", "OFFSET", "ON", "OP", "OPMARGINS", "OPSEQUENCE", "OPT", "OPTFILE", 
			"OPTIMIZE", "OPTIONS", "OUT", "OUTDD", "PFD", "PPTDBG", "PGMN", "PGMNAME", 
			"PROCESS", "PROLOG", "QUOTE", "RENT", "REPLACE", "REPLACING", "RMODE", 
			"RPARENCHAR", "SEP", "SEPARATE", "SEQ", "SEQUENCE", "SHORT", "SIZE", 
			"SOURCE", "SP", "SPACE", "SPIE", "SQL", "SQLC", "SQLCCSID", "SQLIMS", 
			"SKIP1", "SKIP2", "SKIP3", "SS", "SSR", "SSRANGE", "STD", "SUPPRESS", 
			"SYSEIB", "SZ", "TERM", "TERMINAL", "TEST", "THREAD", "TITLE", "TRIG", 
			"TRUNC", "UE", "UPPER", "VBREF", "WD", "WORD", "XMLPARSE", "XMLSS", "XOPTS", 
			"XP", "XREF", "YEARWINDOW", "YW", "ZWB", "C_CHAR", "D_CHAR", "E_CHAR", 
			"F_CHAR", "H_CHAR", "I_CHAR", "M_CHAR", "N_CHAR", "Q_CHAR", "S_CHAR", 
			"U_CHAR", "W_CHAR", "X_CHAR", "COMMENTTAG", "COMMACHAR", "DOT", "DOUBLEEQUALCHAR", 
			"NONNUMERICLITERAL", "NUMERICLITERAL", "IDENTIFIER", "FILENAME", "NEWLINE", 
			"COMMENTLINE", "WS", "TEXT"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "Cobol85Preprocessor.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public Cobol85PreprocessorParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class StartRuleContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(Cobol85PreprocessorParser.EOF, 0); }
		public List<CompilerOptionsContext> compilerOptions() {
			return getRuleContexts(CompilerOptionsContext.class);
		}
		public CompilerOptionsContext compilerOptions(int i) {
			return getRuleContext(CompilerOptionsContext.class,i);
		}
		public List<CopyStatementContext> copyStatement() {
			return getRuleContexts(CopyStatementContext.class);
		}
		public CopyStatementContext copyStatement(int i) {
			return getRuleContext(CopyStatementContext.class,i);
		}
		public List<ExecCicsStatementContext> execCicsStatement() {
			return getRuleContexts(ExecCicsStatementContext.class);
		}
		public ExecCicsStatementContext execCicsStatement(int i) {
			return getRuleContext(ExecCicsStatementContext.class,i);
		}
		public List<ExecSqlStatementContext> execSqlStatement() {
			return getRuleContexts(ExecSqlStatementContext.class);
		}
		public ExecSqlStatementContext execSqlStatement(int i) {
			return getRuleContext(ExecSqlStatementContext.class,i);
		}
		public List<ExecSqlImsStatementContext> execSqlImsStatement() {
			return getRuleContexts(ExecSqlImsStatementContext.class);
		}
		public ExecSqlImsStatementContext execSqlImsStatement(int i) {
			return getRuleContext(ExecSqlImsStatementContext.class,i);
		}
		public List<ReplaceOffStatementContext> replaceOffStatement() {
			return getRuleContexts(ReplaceOffStatementContext.class);
		}
		public ReplaceOffStatementContext replaceOffStatement(int i) {
			return getRuleContext(ReplaceOffStatementContext.class,i);
		}
		public List<ReplaceAreaContext> replaceArea() {
			return getRuleContexts(ReplaceAreaContext.class);
		}
		public ReplaceAreaContext replaceArea(int i) {
			return getRuleContext(ReplaceAreaContext.class,i);
		}
		public List<EjectStatementContext> ejectStatement() {
			return getRuleContexts(EjectStatementContext.class);
		}
		public EjectStatementContext ejectStatement(int i) {
			return getRuleContext(EjectStatementContext.class,i);
		}
		public List<SkipStatementContext> skipStatement() {
			return getRuleContexts(SkipStatementContext.class);
		}
		public SkipStatementContext skipStatement(int i) {
			return getRuleContext(SkipStatementContext.class,i);
		}
		public List<TitleStatementContext> titleStatement() {
			return getRuleContexts(TitleStatementContext.class);
		}
		public TitleStatementContext titleStatement(int i) {
			return getRuleContext(TitleStatementContext.class,i);
		}
		public List<CharDataLineContext> charDataLine() {
			return getRuleContexts(CharDataLineContext.class);
		}
		public CharDataLineContext charDataLine(int i) {
			return getRuleContext(CharDataLineContext.class,i);
		}
		public List<TerminalNode> NEWLINE() { return getTokens(Cobol85PreprocessorParser.NEWLINE); }
		public TerminalNode NEWLINE(int i) {
			return getToken(Cobol85PreprocessorParser.NEWLINE, i);
		}
		public StartRuleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_startRule; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterStartRule(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitStartRule(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitStartRule(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StartRuleContext startRule() throws RecognitionException {
		StartRuleContext _localctx = new StartRuleContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_startRule);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(74);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & -2310346608841326594L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -8193L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -288230376151711745L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -9042383626829825L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 85597355895L) != 0)) {
				{
				setState(72);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
				case 1:
					{
					setState(60);
					compilerOptions();
					}
					break;
				case 2:
					{
					setState(61);
					copyStatement();
					}
					break;
				case 3:
					{
					setState(62);
					execCicsStatement();
					}
					break;
				case 4:
					{
					setState(63);
					execSqlStatement();
					}
					break;
				case 5:
					{
					setState(64);
					execSqlImsStatement();
					}
					break;
				case 6:
					{
					setState(65);
					replaceOffStatement();
					}
					break;
				case 7:
					{
					setState(66);
					replaceArea();
					}
					break;
				case 8:
					{
					setState(67);
					ejectStatement();
					}
					break;
				case 9:
					{
					setState(68);
					skipStatement();
					}
					break;
				case 10:
					{
					setState(69);
					titleStatement();
					}
					break;
				case 11:
					{
					setState(70);
					charDataLine();
					}
					break;
				case 12:
					{
					setState(71);
					match(NEWLINE);
					}
					break;
				}
				}
				setState(76);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(77);
			match(EOF);
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

	@SuppressWarnings("CheckReturnValue")
	public static class CompilerOptionsContext extends ParserRuleContext {
		public TerminalNode PROCESS() { return getToken(Cobol85PreprocessorParser.PROCESS, 0); }
		public TerminalNode CBL() { return getToken(Cobol85PreprocessorParser.CBL, 0); }
		public List<CompilerOptionContext> compilerOption() {
			return getRuleContexts(CompilerOptionContext.class);
		}
		public CompilerOptionContext compilerOption(int i) {
			return getRuleContext(CompilerOptionContext.class,i);
		}
		public List<CompilerXOptsContext> compilerXOpts() {
			return getRuleContexts(CompilerXOptsContext.class);
		}
		public CompilerXOptsContext compilerXOpts(int i) {
			return getRuleContext(CompilerXOptsContext.class,i);
		}
		public List<TerminalNode> COMMACHAR() { return getTokens(Cobol85PreprocessorParser.COMMACHAR); }
		public TerminalNode COMMACHAR(int i) {
			return getToken(Cobol85PreprocessorParser.COMMACHAR, i);
		}
		public CompilerOptionsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_compilerOptions; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterCompilerOptions(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitCompilerOptions(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitCompilerOptions(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CompilerOptionsContext compilerOptions() throws RecognitionException {
		CompilerOptionsContext _localctx = new CompilerOptionsContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_compilerOptions);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(79);
			_la = _input.LA(1);
			if ( !(_la==CBL || _la==PROCESS) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(85); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					setState(85);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case ADATA:
					case ADV:
					case APOST:
					case AR:
					case ARITH:
					case AWO:
					case BLOCK0:
					case BUF:
					case BUFSIZE:
					case CBLCARD:
					case CICS:
					case COBOL2:
					case COBOL3:
					case CODEPAGE:
					case COMPILE:
					case CP:
					case CPP:
					case CPSM:
					case CURR:
					case CURRENCY:
					case DATA:
					case DATEPROC:
					case DBCS:
					case DEBUG:
					case DECK:
					case DIAGTRUNC:
					case DLL:
					case DP:
					case DTR:
					case DU:
					case DUMP:
					case DYN:
					case DYNAM:
					case EDF:
					case EPILOG:
					case EXIT:
					case EXP:
					case EXPORTALL:
					case FASTSRT:
					case FEPI:
					case FLAG:
					case FLAGSTD:
					case FSRT:
					case GDS:
					case GRAPHIC:
					case INTDATE:
					case LANG:
					case LANGUAGE:
					case LC:
					case LEASM:
					case LENGTH:
					case LIB:
					case LIN:
					case LINECOUNT:
					case LINKAGE:
					case LIST:
					case MAP:
					case MARGINS:
					case MD:
					case MDECK:
					case NAME:
					case NATLANG:
					case NOADATA:
					case NOADV:
					case NOAWO:
					case NOBLOCK0:
					case NOC:
					case NOCBLCARD:
					case NOCICS:
					case NOCMPR2:
					case NOCOMPILE:
					case NOCPSM:
					case NOCURR:
					case NOCURRENCY:
					case NOD:
					case NODATEPROC:
					case NODBCS:
					case NODE:
					case NODEBUG:
					case NODECK:
					case NODIAGTRUNC:
					case NODLL:
					case NODU:
					case NODUMP:
					case NODP:
					case NODTR:
					case NODYN:
					case NODYNAM:
					case NOEDF:
					case NOEPILOG:
					case NOEXIT:
					case NOEXP:
					case NOEXPORTALL:
					case NOF:
					case NOFASTSRT:
					case NOFEPI:
					case NOFLAG:
					case NOFLAGMIG:
					case NOFLAGSTD:
					case NOFSRT:
					case NOGRAPHIC:
					case NOLENGTH:
					case NOLIB:
					case NOLINKAGE:
					case NOLIST:
					case NOMAP:
					case NOMD:
					case NOMDECK:
					case NONAME:
					case NONUM:
					case NONUMBER:
					case NOOBJ:
					case NOOBJECT:
					case NOOFF:
					case NOOFFSET:
					case NOOPSEQUENCE:
					case NOOPT:
					case NOOPTIMIZE:
					case NOOPTIONS:
					case NOP:
					case NOPROLOG:
					case NORENT:
					case NOS:
					case NOSEQ:
					case NOSOURCE:
					case NOSPIE:
					case NOSQL:
					case NOSQLC:
					case NOSQLCCSID:
					case NOSSR:
					case NOSSRANGE:
					case NOSTDTRUNC:
					case NOSEQUENCE:
					case NOTERM:
					case NOTERMINAL:
					case NOTEST:
					case NOTHREAD:
					case NOVBREF:
					case NOWD:
					case NOWORD:
					case NOX:
					case NOXREF:
					case NOZWB:
					case NS:
					case NSEQ:
					case NSYMBOL:
					case NUM:
					case NUMBER:
					case NUMPROC:
					case OBJ:
					case OBJECT:
					case OFF:
					case OFFSET:
					case OP:
					case OPMARGINS:
					case OPSEQUENCE:
					case OPT:
					case OPTFILE:
					case OPTIMIZE:
					case OPTIONS:
					case OUT:
					case OUTDD:
					case PGMN:
					case PGMNAME:
					case PROLOG:
					case QUOTE:
					case RENT:
					case RMODE:
					case SEQ:
					case SEQUENCE:
					case SIZE:
					case SOURCE:
					case SP:
					case SPACE:
					case SPIE:
					case SQL:
					case SQLC:
					case SQLCCSID:
					case SSR:
					case SSRANGE:
					case SYSEIB:
					case SZ:
					case TERM:
					case TERMINAL:
					case TEST:
					case THREAD:
					case TRUNC:
					case VBREF:
					case WD:
					case WORD:
					case XMLPARSE:
					case XP:
					case XREF:
					case YEARWINDOW:
					case YW:
					case ZWB:
					case C_CHAR:
					case D_CHAR:
					case F_CHAR:
					case Q_CHAR:
					case S_CHAR:
					case X_CHAR:
					case COMMACHAR:
						{
						setState(81);
						_errHandler.sync(this);
						_la = _input.LA(1);
						if (_la==COMMACHAR) {
							{
							setState(80);
							match(COMMACHAR);
							}
						}

						setState(83);
						compilerOption();
						}
						break;
					case XOPTS:
						{
						setState(84);
						compilerXOpts();
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(87); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,4,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
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

	@SuppressWarnings("CheckReturnValue")
	public static class CompilerXOptsContext extends ParserRuleContext {
		public TerminalNode XOPTS() { return getToken(Cobol85PreprocessorParser.XOPTS, 0); }
		public TerminalNode LPARENCHAR() { return getToken(Cobol85PreprocessorParser.LPARENCHAR, 0); }
		public List<CompilerOptionContext> compilerOption() {
			return getRuleContexts(CompilerOptionContext.class);
		}
		public CompilerOptionContext compilerOption(int i) {
			return getRuleContext(CompilerOptionContext.class,i);
		}
		public TerminalNode RPARENCHAR() { return getToken(Cobol85PreprocessorParser.RPARENCHAR, 0); }
		public List<TerminalNode> COMMACHAR() { return getTokens(Cobol85PreprocessorParser.COMMACHAR); }
		public TerminalNode COMMACHAR(int i) {
			return getToken(Cobol85PreprocessorParser.COMMACHAR, i);
		}
		public CompilerXOptsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_compilerXOpts; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterCompilerXOpts(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitCompilerXOpts(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitCompilerXOpts(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CompilerXOptsContext compilerXOpts() throws RecognitionException {
		CompilerXOptsContext _localctx = new CompilerXOptsContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_compilerXOpts);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(89);
			match(XOPTS);
			setState(90);
			match(LPARENCHAR);
			setState(91);
			compilerOption();
			setState(98);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & -639230256804891194L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -2669655688115L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -72060961292353545L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & 5750005924040276863L) != 0) || ((((_la - 257)) & ~0x3f) == 0 && ((1L << (_la - 257)) & 43540431L) != 0)) {
				{
				{
				setState(93);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMACHAR) {
					{
					setState(92);
					match(COMMACHAR);
					}
				}

				setState(95);
				compilerOption();
				}
				}
				setState(100);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(101);
			match(RPARENCHAR);
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

	@SuppressWarnings("CheckReturnValue")
	public static class CompilerOptionContext extends ParserRuleContext {
		public TerminalNode ADATA() { return getToken(Cobol85PreprocessorParser.ADATA, 0); }
		public TerminalNode ADV() { return getToken(Cobol85PreprocessorParser.ADV, 0); }
		public TerminalNode APOST() { return getToken(Cobol85PreprocessorParser.APOST, 0); }
		public TerminalNode LPARENCHAR() { return getToken(Cobol85PreprocessorParser.LPARENCHAR, 0); }
		public TerminalNode RPARENCHAR() { return getToken(Cobol85PreprocessorParser.RPARENCHAR, 0); }
		public TerminalNode ARITH() { return getToken(Cobol85PreprocessorParser.ARITH, 0); }
		public TerminalNode AR() { return getToken(Cobol85PreprocessorParser.AR, 0); }
		public TerminalNode EXTEND() { return getToken(Cobol85PreprocessorParser.EXTEND, 0); }
		public List<TerminalNode> E_CHAR() { return getTokens(Cobol85PreprocessorParser.E_CHAR); }
		public TerminalNode E_CHAR(int i) {
			return getToken(Cobol85PreprocessorParser.E_CHAR, i);
		}
		public TerminalNode COMPAT() { return getToken(Cobol85PreprocessorParser.COMPAT, 0); }
		public TerminalNode C_CHAR() { return getToken(Cobol85PreprocessorParser.C_CHAR, 0); }
		public TerminalNode AWO() { return getToken(Cobol85PreprocessorParser.AWO, 0); }
		public TerminalNode BLOCK0() { return getToken(Cobol85PreprocessorParser.BLOCK0, 0); }
		public List<LiteralContext> literal() {
			return getRuleContexts(LiteralContext.class);
		}
		public LiteralContext literal(int i) {
			return getRuleContext(LiteralContext.class,i);
		}
		public TerminalNode BUFSIZE() { return getToken(Cobol85PreprocessorParser.BUFSIZE, 0); }
		public TerminalNode BUF() { return getToken(Cobol85PreprocessorParser.BUF, 0); }
		public TerminalNode CBLCARD() { return getToken(Cobol85PreprocessorParser.CBLCARD, 0); }
		public TerminalNode CICS() { return getToken(Cobol85PreprocessorParser.CICS, 0); }
		public TerminalNode COBOL2() { return getToken(Cobol85PreprocessorParser.COBOL2, 0); }
		public TerminalNode COBOL3() { return getToken(Cobol85PreprocessorParser.COBOL3, 0); }
		public TerminalNode CODEPAGE() { return getToken(Cobol85PreprocessorParser.CODEPAGE, 0); }
		public TerminalNode CP() { return getToken(Cobol85PreprocessorParser.CP, 0); }
		public TerminalNode COMPILE() { return getToken(Cobol85PreprocessorParser.COMPILE, 0); }
		public TerminalNode CPP() { return getToken(Cobol85PreprocessorParser.CPP, 0); }
		public TerminalNode CPSM() { return getToken(Cobol85PreprocessorParser.CPSM, 0); }
		public TerminalNode CURRENCY() { return getToken(Cobol85PreprocessorParser.CURRENCY, 0); }
		public TerminalNode CURR() { return getToken(Cobol85PreprocessorParser.CURR, 0); }
		public TerminalNode DATA() { return getToken(Cobol85PreprocessorParser.DATA, 0); }
		public TerminalNode DATEPROC() { return getToken(Cobol85PreprocessorParser.DATEPROC, 0); }
		public TerminalNode DP() { return getToken(Cobol85PreprocessorParser.DP, 0); }
		public List<TerminalNode> COMMACHAR() { return getTokens(Cobol85PreprocessorParser.COMMACHAR); }
		public TerminalNode COMMACHAR(int i) {
			return getToken(Cobol85PreprocessorParser.COMMACHAR, i);
		}
		public TerminalNode FLAG() { return getToken(Cobol85PreprocessorParser.FLAG, 0); }
		public TerminalNode NOFLAG() { return getToken(Cobol85PreprocessorParser.NOFLAG, 0); }
		public TerminalNode TRIG() { return getToken(Cobol85PreprocessorParser.TRIG, 0); }
		public TerminalNode NOTRIG() { return getToken(Cobol85PreprocessorParser.NOTRIG, 0); }
		public TerminalNode DBCS() { return getToken(Cobol85PreprocessorParser.DBCS, 0); }
		public TerminalNode DECK() { return getToken(Cobol85PreprocessorParser.DECK, 0); }
		public TerminalNode D_CHAR() { return getToken(Cobol85PreprocessorParser.D_CHAR, 0); }
		public TerminalNode DEBUG() { return getToken(Cobol85PreprocessorParser.DEBUG, 0); }
		public TerminalNode DIAGTRUNC() { return getToken(Cobol85PreprocessorParser.DIAGTRUNC, 0); }
		public TerminalNode DTR() { return getToken(Cobol85PreprocessorParser.DTR, 0); }
		public TerminalNode DLL() { return getToken(Cobol85PreprocessorParser.DLL, 0); }
		public TerminalNode DUMP() { return getToken(Cobol85PreprocessorParser.DUMP, 0); }
		public TerminalNode DU() { return getToken(Cobol85PreprocessorParser.DU, 0); }
		public TerminalNode DYNAM() { return getToken(Cobol85PreprocessorParser.DYNAM, 0); }
		public TerminalNode DYN() { return getToken(Cobol85PreprocessorParser.DYN, 0); }
		public TerminalNode EDF() { return getToken(Cobol85PreprocessorParser.EDF, 0); }
		public TerminalNode EPILOG() { return getToken(Cobol85PreprocessorParser.EPILOG, 0); }
		public TerminalNode EXIT() { return getToken(Cobol85PreprocessorParser.EXIT, 0); }
		public TerminalNode EXPORTALL() { return getToken(Cobol85PreprocessorParser.EXPORTALL, 0); }
		public TerminalNode EXP() { return getToken(Cobol85PreprocessorParser.EXP, 0); }
		public TerminalNode FASTSRT() { return getToken(Cobol85PreprocessorParser.FASTSRT, 0); }
		public TerminalNode FSRT() { return getToken(Cobol85PreprocessorParser.FSRT, 0); }
		public TerminalNode FEPI() { return getToken(Cobol85PreprocessorParser.FEPI, 0); }
		public TerminalNode F_CHAR() { return getToken(Cobol85PreprocessorParser.F_CHAR, 0); }
		public List<TerminalNode> I_CHAR() { return getTokens(Cobol85PreprocessorParser.I_CHAR); }
		public TerminalNode I_CHAR(int i) {
			return getToken(Cobol85PreprocessorParser.I_CHAR, i);
		}
		public List<TerminalNode> S_CHAR() { return getTokens(Cobol85PreprocessorParser.S_CHAR); }
		public TerminalNode S_CHAR(int i) {
			return getToken(Cobol85PreprocessorParser.S_CHAR, i);
		}
		public List<TerminalNode> U_CHAR() { return getTokens(Cobol85PreprocessorParser.U_CHAR); }
		public TerminalNode U_CHAR(int i) {
			return getToken(Cobol85PreprocessorParser.U_CHAR, i);
		}
		public List<TerminalNode> W_CHAR() { return getTokens(Cobol85PreprocessorParser.W_CHAR); }
		public TerminalNode W_CHAR(int i) {
			return getToken(Cobol85PreprocessorParser.W_CHAR, i);
		}
		public TerminalNode FLAGSTD() { return getToken(Cobol85PreprocessorParser.FLAGSTD, 0); }
		public TerminalNode M_CHAR() { return getToken(Cobol85PreprocessorParser.M_CHAR, 0); }
		public TerminalNode H_CHAR() { return getToken(Cobol85PreprocessorParser.H_CHAR, 0); }
		public TerminalNode DD() { return getToken(Cobol85PreprocessorParser.DD, 0); }
		public TerminalNode N_CHAR() { return getToken(Cobol85PreprocessorParser.N_CHAR, 0); }
		public TerminalNode NN() { return getToken(Cobol85PreprocessorParser.NN, 0); }
		public TerminalNode SS() { return getToken(Cobol85PreprocessorParser.SS, 0); }
		public TerminalNode GDS() { return getToken(Cobol85PreprocessorParser.GDS, 0); }
		public TerminalNode GRAPHIC() { return getToken(Cobol85PreprocessorParser.GRAPHIC, 0); }
		public TerminalNode INTDATE() { return getToken(Cobol85PreprocessorParser.INTDATE, 0); }
		public TerminalNode ANSI() { return getToken(Cobol85PreprocessorParser.ANSI, 0); }
		public TerminalNode LILIAN() { return getToken(Cobol85PreprocessorParser.LILIAN, 0); }
		public TerminalNode LANGUAGE() { return getToken(Cobol85PreprocessorParser.LANGUAGE, 0); }
		public TerminalNode LANG() { return getToken(Cobol85PreprocessorParser.LANG, 0); }
		public TerminalNode ENGLISH() { return getToken(Cobol85PreprocessorParser.ENGLISH, 0); }
		public TerminalNode CS() { return getToken(Cobol85PreprocessorParser.CS, 0); }
		public TerminalNode EN() { return getToken(Cobol85PreprocessorParser.EN, 0); }
		public TerminalNode JA() { return getToken(Cobol85PreprocessorParser.JA, 0); }
		public TerminalNode JP() { return getToken(Cobol85PreprocessorParser.JP, 0); }
		public TerminalNode KA() { return getToken(Cobol85PreprocessorParser.KA, 0); }
		public TerminalNode UE() { return getToken(Cobol85PreprocessorParser.UE, 0); }
		public TerminalNode LEASM() { return getToken(Cobol85PreprocessorParser.LEASM, 0); }
		public TerminalNode LENGTH() { return getToken(Cobol85PreprocessorParser.LENGTH, 0); }
		public TerminalNode LIB() { return getToken(Cobol85PreprocessorParser.LIB, 0); }
		public TerminalNode LIN() { return getToken(Cobol85PreprocessorParser.LIN, 0); }
		public TerminalNode LINECOUNT() { return getToken(Cobol85PreprocessorParser.LINECOUNT, 0); }
		public TerminalNode LC() { return getToken(Cobol85PreprocessorParser.LC, 0); }
		public TerminalNode LINKAGE() { return getToken(Cobol85PreprocessorParser.LINKAGE, 0); }
		public TerminalNode LIST() { return getToken(Cobol85PreprocessorParser.LIST, 0); }
		public TerminalNode MAP() { return getToken(Cobol85PreprocessorParser.MAP, 0); }
		public TerminalNode MARGINS() { return getToken(Cobol85PreprocessorParser.MARGINS, 0); }
		public TerminalNode MDECK() { return getToken(Cobol85PreprocessorParser.MDECK, 0); }
		public TerminalNode MD() { return getToken(Cobol85PreprocessorParser.MD, 0); }
		public TerminalNode NOC() { return getToken(Cobol85PreprocessorParser.NOC, 0); }
		public TerminalNode NOCOMPILE() { return getToken(Cobol85PreprocessorParser.NOCOMPILE, 0); }
		public TerminalNode NAME() { return getToken(Cobol85PreprocessorParser.NAME, 0); }
		public TerminalNode ALIAS() { return getToken(Cobol85PreprocessorParser.ALIAS, 0); }
		public TerminalNode NOALIAS() { return getToken(Cobol85PreprocessorParser.NOALIAS, 0); }
		public TerminalNode NATLANG() { return getToken(Cobol85PreprocessorParser.NATLANG, 0); }
		public TerminalNode NOADATA() { return getToken(Cobol85PreprocessorParser.NOADATA, 0); }
		public TerminalNode NOADV() { return getToken(Cobol85PreprocessorParser.NOADV, 0); }
		public TerminalNode NOAWO() { return getToken(Cobol85PreprocessorParser.NOAWO, 0); }
		public TerminalNode NOBLOCK0() { return getToken(Cobol85PreprocessorParser.NOBLOCK0, 0); }
		public TerminalNode NOCBLCARD() { return getToken(Cobol85PreprocessorParser.NOCBLCARD, 0); }
		public TerminalNode NOCICS() { return getToken(Cobol85PreprocessorParser.NOCICS, 0); }
		public TerminalNode NOCMPR2() { return getToken(Cobol85PreprocessorParser.NOCMPR2, 0); }
		public TerminalNode NOCPSM() { return getToken(Cobol85PreprocessorParser.NOCPSM, 0); }
		public TerminalNode NOCURRENCY() { return getToken(Cobol85PreprocessorParser.NOCURRENCY, 0); }
		public TerminalNode NOCURR() { return getToken(Cobol85PreprocessorParser.NOCURR, 0); }
		public TerminalNode NODATEPROC() { return getToken(Cobol85PreprocessorParser.NODATEPROC, 0); }
		public TerminalNode NODP() { return getToken(Cobol85PreprocessorParser.NODP, 0); }
		public TerminalNode NODBCS() { return getToken(Cobol85PreprocessorParser.NODBCS, 0); }
		public TerminalNode NODEBUG() { return getToken(Cobol85PreprocessorParser.NODEBUG, 0); }
		public TerminalNode NODECK() { return getToken(Cobol85PreprocessorParser.NODECK, 0); }
		public TerminalNode NOD() { return getToken(Cobol85PreprocessorParser.NOD, 0); }
		public TerminalNode NODLL() { return getToken(Cobol85PreprocessorParser.NODLL, 0); }
		public TerminalNode NODE() { return getToken(Cobol85PreprocessorParser.NODE, 0); }
		public TerminalNode NODUMP() { return getToken(Cobol85PreprocessorParser.NODUMP, 0); }
		public TerminalNode NODU() { return getToken(Cobol85PreprocessorParser.NODU, 0); }
		public TerminalNode NODIAGTRUNC() { return getToken(Cobol85PreprocessorParser.NODIAGTRUNC, 0); }
		public TerminalNode NODTR() { return getToken(Cobol85PreprocessorParser.NODTR, 0); }
		public TerminalNode NODYNAM() { return getToken(Cobol85PreprocessorParser.NODYNAM, 0); }
		public TerminalNode NODYN() { return getToken(Cobol85PreprocessorParser.NODYN, 0); }
		public TerminalNode NOEDF() { return getToken(Cobol85PreprocessorParser.NOEDF, 0); }
		public TerminalNode NOEPILOG() { return getToken(Cobol85PreprocessorParser.NOEPILOG, 0); }
		public TerminalNode NOEXIT() { return getToken(Cobol85PreprocessorParser.NOEXIT, 0); }
		public TerminalNode NOEXPORTALL() { return getToken(Cobol85PreprocessorParser.NOEXPORTALL, 0); }
		public TerminalNode NOEXP() { return getToken(Cobol85PreprocessorParser.NOEXP, 0); }
		public TerminalNode NOFASTSRT() { return getToken(Cobol85PreprocessorParser.NOFASTSRT, 0); }
		public TerminalNode NOFSRT() { return getToken(Cobol85PreprocessorParser.NOFSRT, 0); }
		public TerminalNode NOFEPI() { return getToken(Cobol85PreprocessorParser.NOFEPI, 0); }
		public TerminalNode NOF() { return getToken(Cobol85PreprocessorParser.NOF, 0); }
		public TerminalNode NOFLAGMIG() { return getToken(Cobol85PreprocessorParser.NOFLAGMIG, 0); }
		public TerminalNode NOFLAGSTD() { return getToken(Cobol85PreprocessorParser.NOFLAGSTD, 0); }
		public TerminalNode NOGRAPHIC() { return getToken(Cobol85PreprocessorParser.NOGRAPHIC, 0); }
		public TerminalNode NOLENGTH() { return getToken(Cobol85PreprocessorParser.NOLENGTH, 0); }
		public TerminalNode NOLIB() { return getToken(Cobol85PreprocessorParser.NOLIB, 0); }
		public TerminalNode NOLINKAGE() { return getToken(Cobol85PreprocessorParser.NOLINKAGE, 0); }
		public TerminalNode NOLIST() { return getToken(Cobol85PreprocessorParser.NOLIST, 0); }
		public TerminalNode NOMAP() { return getToken(Cobol85PreprocessorParser.NOMAP, 0); }
		public TerminalNode NOMDECK() { return getToken(Cobol85PreprocessorParser.NOMDECK, 0); }
		public TerminalNode NOMD() { return getToken(Cobol85PreprocessorParser.NOMD, 0); }
		public TerminalNode NONAME() { return getToken(Cobol85PreprocessorParser.NONAME, 0); }
		public TerminalNode NONUMBER() { return getToken(Cobol85PreprocessorParser.NONUMBER, 0); }
		public TerminalNode NONUM() { return getToken(Cobol85PreprocessorParser.NONUM, 0); }
		public TerminalNode NOOBJECT() { return getToken(Cobol85PreprocessorParser.NOOBJECT, 0); }
		public TerminalNode NOOBJ() { return getToken(Cobol85PreprocessorParser.NOOBJ, 0); }
		public TerminalNode NOOFFSET() { return getToken(Cobol85PreprocessorParser.NOOFFSET, 0); }
		public TerminalNode NOOFF() { return getToken(Cobol85PreprocessorParser.NOOFF, 0); }
		public TerminalNode NOOPSEQUENCE() { return getToken(Cobol85PreprocessorParser.NOOPSEQUENCE, 0); }
		public TerminalNode NOOPTIMIZE() { return getToken(Cobol85PreprocessorParser.NOOPTIMIZE, 0); }
		public TerminalNode NOOPT() { return getToken(Cobol85PreprocessorParser.NOOPT, 0); }
		public TerminalNode NOOPTIONS() { return getToken(Cobol85PreprocessorParser.NOOPTIONS, 0); }
		public TerminalNode NOP() { return getToken(Cobol85PreprocessorParser.NOP, 0); }
		public TerminalNode NOPROLOG() { return getToken(Cobol85PreprocessorParser.NOPROLOG, 0); }
		public TerminalNode NORENT() { return getToken(Cobol85PreprocessorParser.NORENT, 0); }
		public TerminalNode NOSEQUENCE() { return getToken(Cobol85PreprocessorParser.NOSEQUENCE, 0); }
		public TerminalNode NOSEQ() { return getToken(Cobol85PreprocessorParser.NOSEQ, 0); }
		public TerminalNode NOSOURCE() { return getToken(Cobol85PreprocessorParser.NOSOURCE, 0); }
		public TerminalNode NOS() { return getToken(Cobol85PreprocessorParser.NOS, 0); }
		public TerminalNode NOSPIE() { return getToken(Cobol85PreprocessorParser.NOSPIE, 0); }
		public TerminalNode NOSQL() { return getToken(Cobol85PreprocessorParser.NOSQL, 0); }
		public TerminalNode NOSQLCCSID() { return getToken(Cobol85PreprocessorParser.NOSQLCCSID, 0); }
		public TerminalNode NOSQLC() { return getToken(Cobol85PreprocessorParser.NOSQLC, 0); }
		public TerminalNode NOSSRANGE() { return getToken(Cobol85PreprocessorParser.NOSSRANGE, 0); }
		public TerminalNode NOSSR() { return getToken(Cobol85PreprocessorParser.NOSSR, 0); }
		public TerminalNode NOSTDTRUNC() { return getToken(Cobol85PreprocessorParser.NOSTDTRUNC, 0); }
		public TerminalNode NOTERMINAL() { return getToken(Cobol85PreprocessorParser.NOTERMINAL, 0); }
		public TerminalNode NOTERM() { return getToken(Cobol85PreprocessorParser.NOTERM, 0); }
		public TerminalNode NOTEST() { return getToken(Cobol85PreprocessorParser.NOTEST, 0); }
		public TerminalNode NOTHREAD() { return getToken(Cobol85PreprocessorParser.NOTHREAD, 0); }
		public TerminalNode NOVBREF() { return getToken(Cobol85PreprocessorParser.NOVBREF, 0); }
		public TerminalNode NOWORD() { return getToken(Cobol85PreprocessorParser.NOWORD, 0); }
		public TerminalNode NOWD() { return getToken(Cobol85PreprocessorParser.NOWD, 0); }
		public TerminalNode NSEQ() { return getToken(Cobol85PreprocessorParser.NSEQ, 0); }
		public TerminalNode NSYMBOL() { return getToken(Cobol85PreprocessorParser.NSYMBOL, 0); }
		public TerminalNode NS() { return getToken(Cobol85PreprocessorParser.NS, 0); }
		public TerminalNode NATIONAL() { return getToken(Cobol85PreprocessorParser.NATIONAL, 0); }
		public TerminalNode NAT() { return getToken(Cobol85PreprocessorParser.NAT, 0); }
		public TerminalNode NOXREF() { return getToken(Cobol85PreprocessorParser.NOXREF, 0); }
		public TerminalNode NOX() { return getToken(Cobol85PreprocessorParser.NOX, 0); }
		public TerminalNode NOZWB() { return getToken(Cobol85PreprocessorParser.NOZWB, 0); }
		public TerminalNode NUMBER() { return getToken(Cobol85PreprocessorParser.NUMBER, 0); }
		public TerminalNode NUM() { return getToken(Cobol85PreprocessorParser.NUM, 0); }
		public TerminalNode NUMPROC() { return getToken(Cobol85PreprocessorParser.NUMPROC, 0); }
		public TerminalNode MIG() { return getToken(Cobol85PreprocessorParser.MIG, 0); }
		public TerminalNode NOPFD() { return getToken(Cobol85PreprocessorParser.NOPFD, 0); }
		public TerminalNode PFD() { return getToken(Cobol85PreprocessorParser.PFD, 0); }
		public TerminalNode OBJECT() { return getToken(Cobol85PreprocessorParser.OBJECT, 0); }
		public TerminalNode OBJ() { return getToken(Cobol85PreprocessorParser.OBJ, 0); }
		public TerminalNode OFFSET() { return getToken(Cobol85PreprocessorParser.OFFSET, 0); }
		public TerminalNode OFF() { return getToken(Cobol85PreprocessorParser.OFF, 0); }
		public TerminalNode OPMARGINS() { return getToken(Cobol85PreprocessorParser.OPMARGINS, 0); }
		public TerminalNode OPSEQUENCE() { return getToken(Cobol85PreprocessorParser.OPSEQUENCE, 0); }
		public TerminalNode OPTIMIZE() { return getToken(Cobol85PreprocessorParser.OPTIMIZE, 0); }
		public TerminalNode OPT() { return getToken(Cobol85PreprocessorParser.OPT, 0); }
		public TerminalNode FULL() { return getToken(Cobol85PreprocessorParser.FULL, 0); }
		public TerminalNode STD() { return getToken(Cobol85PreprocessorParser.STD, 0); }
		public TerminalNode OPTFILE() { return getToken(Cobol85PreprocessorParser.OPTFILE, 0); }
		public TerminalNode OPTIONS() { return getToken(Cobol85PreprocessorParser.OPTIONS, 0); }
		public TerminalNode OP() { return getToken(Cobol85PreprocessorParser.OP, 0); }
		public CobolWordContext cobolWord() {
			return getRuleContext(CobolWordContext.class,0);
		}
		public TerminalNode OUTDD() { return getToken(Cobol85PreprocessorParser.OUTDD, 0); }
		public TerminalNode OUT() { return getToken(Cobol85PreprocessorParser.OUT, 0); }
		public TerminalNode PGMNAME() { return getToken(Cobol85PreprocessorParser.PGMNAME, 0); }
		public TerminalNode PGMN() { return getToken(Cobol85PreprocessorParser.PGMN, 0); }
		public TerminalNode CO() { return getToken(Cobol85PreprocessorParser.CO, 0); }
		public TerminalNode LM() { return getToken(Cobol85PreprocessorParser.LM, 0); }
		public TerminalNode LONGMIXED() { return getToken(Cobol85PreprocessorParser.LONGMIXED, 0); }
		public TerminalNode LONGUPPER() { return getToken(Cobol85PreprocessorParser.LONGUPPER, 0); }
		public TerminalNode LU() { return getToken(Cobol85PreprocessorParser.LU, 0); }
		public TerminalNode MIXED() { return getToken(Cobol85PreprocessorParser.MIXED, 0); }
		public TerminalNode UPPER() { return getToken(Cobol85PreprocessorParser.UPPER, 0); }
		public TerminalNode PROLOG() { return getToken(Cobol85PreprocessorParser.PROLOG, 0); }
		public TerminalNode QUOTE() { return getToken(Cobol85PreprocessorParser.QUOTE, 0); }
		public TerminalNode Q_CHAR() { return getToken(Cobol85PreprocessorParser.Q_CHAR, 0); }
		public TerminalNode RENT() { return getToken(Cobol85PreprocessorParser.RENT, 0); }
		public TerminalNode RMODE() { return getToken(Cobol85PreprocessorParser.RMODE, 0); }
		public TerminalNode ANY() { return getToken(Cobol85PreprocessorParser.ANY, 0); }
		public TerminalNode AUTO() { return getToken(Cobol85PreprocessorParser.AUTO, 0); }
		public TerminalNode SEQUENCE() { return getToken(Cobol85PreprocessorParser.SEQUENCE, 0); }
		public TerminalNode SEQ() { return getToken(Cobol85PreprocessorParser.SEQ, 0); }
		public TerminalNode SIZE() { return getToken(Cobol85PreprocessorParser.SIZE, 0); }
		public TerminalNode SZ() { return getToken(Cobol85PreprocessorParser.SZ, 0); }
		public TerminalNode MAX() { return getToken(Cobol85PreprocessorParser.MAX, 0); }
		public TerminalNode SOURCE() { return getToken(Cobol85PreprocessorParser.SOURCE, 0); }
		public TerminalNode SP() { return getToken(Cobol85PreprocessorParser.SP, 0); }
		public TerminalNode SPACE() { return getToken(Cobol85PreprocessorParser.SPACE, 0); }
		public TerminalNode SPIE() { return getToken(Cobol85PreprocessorParser.SPIE, 0); }
		public TerminalNode SQL() { return getToken(Cobol85PreprocessorParser.SQL, 0); }
		public TerminalNode SQLCCSID() { return getToken(Cobol85PreprocessorParser.SQLCCSID, 0); }
		public TerminalNode SQLC() { return getToken(Cobol85PreprocessorParser.SQLC, 0); }
		public TerminalNode SSRANGE() { return getToken(Cobol85PreprocessorParser.SSRANGE, 0); }
		public TerminalNode SSR() { return getToken(Cobol85PreprocessorParser.SSR, 0); }
		public TerminalNode SYSEIB() { return getToken(Cobol85PreprocessorParser.SYSEIB, 0); }
		public TerminalNode TERMINAL() { return getToken(Cobol85PreprocessorParser.TERMINAL, 0); }
		public TerminalNode TERM() { return getToken(Cobol85PreprocessorParser.TERM, 0); }
		public TerminalNode TEST() { return getToken(Cobol85PreprocessorParser.TEST, 0); }
		public TerminalNode HOOK() { return getToken(Cobol85PreprocessorParser.HOOK, 0); }
		public TerminalNode NOHOOK() { return getToken(Cobol85PreprocessorParser.NOHOOK, 0); }
		public TerminalNode SEP() { return getToken(Cobol85PreprocessorParser.SEP, 0); }
		public TerminalNode SEPARATE() { return getToken(Cobol85PreprocessorParser.SEPARATE, 0); }
		public TerminalNode NOSEP() { return getToken(Cobol85PreprocessorParser.NOSEP, 0); }
		public TerminalNode NOSEPARATE() { return getToken(Cobol85PreprocessorParser.NOSEPARATE, 0); }
		public TerminalNode EJPD() { return getToken(Cobol85PreprocessorParser.EJPD, 0); }
		public TerminalNode NOEJPD() { return getToken(Cobol85PreprocessorParser.NOEJPD, 0); }
		public TerminalNode THREAD() { return getToken(Cobol85PreprocessorParser.THREAD, 0); }
		public TerminalNode TRUNC() { return getToken(Cobol85PreprocessorParser.TRUNC, 0); }
		public TerminalNode BIN() { return getToken(Cobol85PreprocessorParser.BIN, 0); }
		public TerminalNode VBREF() { return getToken(Cobol85PreprocessorParser.VBREF, 0); }
		public TerminalNode WORD() { return getToken(Cobol85PreprocessorParser.WORD, 0); }
		public TerminalNode WD() { return getToken(Cobol85PreprocessorParser.WD, 0); }
		public TerminalNode XMLPARSE() { return getToken(Cobol85PreprocessorParser.XMLPARSE, 0); }
		public TerminalNode XP() { return getToken(Cobol85PreprocessorParser.XP, 0); }
		public TerminalNode XMLSS() { return getToken(Cobol85PreprocessorParser.XMLSS, 0); }
		public TerminalNode X_CHAR() { return getToken(Cobol85PreprocessorParser.X_CHAR, 0); }
		public TerminalNode XREF() { return getToken(Cobol85PreprocessorParser.XREF, 0); }
		public TerminalNode SHORT() { return getToken(Cobol85PreprocessorParser.SHORT, 0); }
		public TerminalNode YEARWINDOW() { return getToken(Cobol85PreprocessorParser.YEARWINDOW, 0); }
		public TerminalNode YW() { return getToken(Cobol85PreprocessorParser.YW, 0); }
		public TerminalNode ZWB() { return getToken(Cobol85PreprocessorParser.ZWB, 0); }
		public CompilerOptionContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_compilerOption; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterCompilerOption(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitCompilerOption(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitCompilerOption(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CompilerOptionContext compilerOption() throws RecognitionException {
		CompilerOptionContext _localctx = new CompilerOptionContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_compilerOption);
		int _la;
		try {
			setState(445);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,32,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(103);
				match(ADATA);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(104);
				match(ADV);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(105);
				match(APOST);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(106);
				_la = _input.LA(1);
				if ( !(_la==AR || _la==ARITH) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(107);
				match(LPARENCHAR);
				setState(108);
				_la = _input.LA(1);
				if ( !(_la==COMPAT || _la==EXTEND || _la==C_CHAR || _la==E_CHAR) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(109);
				match(RPARENCHAR);
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(110);
				match(AWO);
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(111);
				match(BLOCK0);
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(112);
				_la = _input.LA(1);
				if ( !(_la==BUF || _la==BUFSIZE) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(113);
				match(LPARENCHAR);
				setState(114);
				literal();
				setState(115);
				match(RPARENCHAR);
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(117);
				match(CBLCARD);
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				setState(118);
				match(CICS);
				setState(123);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
				case 1:
					{
					setState(119);
					match(LPARENCHAR);
					setState(120);
					literal();
					setState(121);
					match(RPARENCHAR);
					}
					break;
				}
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 10);
				{
				setState(125);
				match(COBOL2);
				}
				break;
			case 11:
				enterOuterAlt(_localctx, 11);
				{
				setState(126);
				match(COBOL3);
				}
				break;
			case 12:
				enterOuterAlt(_localctx, 12);
				{
				setState(127);
				_la = _input.LA(1);
				if ( !(_la==CODEPAGE || _la==CP) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(128);
				match(LPARENCHAR);
				setState(129);
				literal();
				setState(130);
				match(RPARENCHAR);
				}
				break;
			case 13:
				enterOuterAlt(_localctx, 13);
				{
				setState(132);
				_la = _input.LA(1);
				if ( !(_la==COMPILE || _la==C_CHAR) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 14:
				enterOuterAlt(_localctx, 14);
				{
				setState(133);
				match(CPP);
				}
				break;
			case 15:
				enterOuterAlt(_localctx, 15);
				{
				setState(134);
				match(CPSM);
				}
				break;
			case 16:
				enterOuterAlt(_localctx, 16);
				{
				setState(135);
				_la = _input.LA(1);
				if ( !(_la==CURR || _la==CURRENCY) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(136);
				match(LPARENCHAR);
				setState(137);
				literal();
				setState(138);
				match(RPARENCHAR);
				}
				break;
			case 17:
				enterOuterAlt(_localctx, 17);
				{
				setState(140);
				match(DATA);
				setState(141);
				match(LPARENCHAR);
				setState(142);
				literal();
				setState(143);
				match(RPARENCHAR);
				}
				break;
			case 18:
				enterOuterAlt(_localctx, 18);
				{
				setState(145);
				_la = _input.LA(1);
				if ( !(_la==DATEPROC || _la==DP) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(157);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
				case 1:
					{
					setState(146);
					match(LPARENCHAR);
					setState(148);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==FLAG || _la==NOFLAG) {
						{
						setState(147);
						_la = _input.LA(1);
						if ( !(_la==FLAG || _la==NOFLAG) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						}
					}

					setState(151);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==COMMACHAR) {
						{
						setState(150);
						match(COMMACHAR);
						}
					}

					setState(154);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==NOTRIG || _la==TRIG) {
						{
						setState(153);
						_la = _input.LA(1);
						if ( !(_la==NOTRIG || _la==TRIG) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						}
					}

					setState(156);
					match(RPARENCHAR);
					}
					break;
				}
				}
				break;
			case 19:
				enterOuterAlt(_localctx, 19);
				{
				setState(159);
				match(DBCS);
				}
				break;
			case 20:
				enterOuterAlt(_localctx, 20);
				{
				setState(160);
				_la = _input.LA(1);
				if ( !(_la==DECK || _la==D_CHAR) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 21:
				enterOuterAlt(_localctx, 21);
				{
				setState(161);
				match(DEBUG);
				}
				break;
			case 22:
				enterOuterAlt(_localctx, 22);
				{
				setState(162);
				_la = _input.LA(1);
				if ( !(_la==DIAGTRUNC || _la==DTR) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 23:
				enterOuterAlt(_localctx, 23);
				{
				setState(163);
				match(DLL);
				}
				break;
			case 24:
				enterOuterAlt(_localctx, 24);
				{
				setState(164);
				_la = _input.LA(1);
				if ( !(_la==DU || _la==DUMP) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 25:
				enterOuterAlt(_localctx, 25);
				{
				setState(165);
				_la = _input.LA(1);
				if ( !(_la==DYN || _la==DYNAM) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 26:
				enterOuterAlt(_localctx, 26);
				{
				setState(166);
				match(EDF);
				}
				break;
			case 27:
				enterOuterAlt(_localctx, 27);
				{
				setState(167);
				match(EPILOG);
				}
				break;
			case 28:
				enterOuterAlt(_localctx, 28);
				{
				setState(168);
				match(EXIT);
				}
				break;
			case 29:
				enterOuterAlt(_localctx, 29);
				{
				setState(169);
				_la = _input.LA(1);
				if ( !(_la==EXP || _la==EXPORTALL) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 30:
				enterOuterAlt(_localctx, 30);
				{
				setState(170);
				_la = _input.LA(1);
				if ( !(_la==FASTSRT || _la==FSRT) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 31:
				enterOuterAlt(_localctx, 31);
				{
				setState(171);
				match(FEPI);
				}
				break;
			case 32:
				enterOuterAlt(_localctx, 32);
				{
				setState(172);
				_la = _input.LA(1);
				if ( !(_la==FLAG || _la==F_CHAR) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(173);
				match(LPARENCHAR);
				setState(174);
				_la = _input.LA(1);
				if ( !(((((_la - 270)) & ~0x3f) == 0 && ((1L << (_la - 270)) & 905L) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(177);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMACHAR) {
					{
					setState(175);
					match(COMMACHAR);
					setState(176);
					_la = _input.LA(1);
					if ( !(((((_la - 270)) & ~0x3f) == 0 && ((1L << (_la - 270)) & 905L) != 0)) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					}
				}

				setState(179);
				match(RPARENCHAR);
				}
				break;
			case 33:
				enterOuterAlt(_localctx, 33);
				{
				setState(180);
				match(FLAGSTD);
				setState(181);
				match(LPARENCHAR);
				setState(182);
				_la = _input.LA(1);
				if ( !(((((_la - 272)) & ~0x3f) == 0 && ((1L << (_la - 272)) & 7L) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(185);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMACHAR) {
					{
					setState(183);
					match(COMMACHAR);
					setState(184);
					_la = _input.LA(1);
					if ( !(_la==DD || _la==NN || ((((_la - 241)) & ~0x3f) == 0 && ((1L << (_la - 241)) & 86167781377L) != 0)) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					}
				}

				setState(187);
				match(RPARENCHAR);
				}
				break;
			case 34:
				enterOuterAlt(_localctx, 34);
				{
				setState(188);
				match(GDS);
				}
				break;
			case 35:
				enterOuterAlt(_localctx, 35);
				{
				setState(189);
				match(GRAPHIC);
				}
				break;
			case 36:
				enterOuterAlt(_localctx, 36);
				{
				setState(190);
				match(INTDATE);
				setState(191);
				match(LPARENCHAR);
				setState(192);
				_la = _input.LA(1);
				if ( !(_la==ANSI || _la==LILIAN) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(193);
				match(RPARENCHAR);
				}
				break;
			case 37:
				enterOuterAlt(_localctx, 37);
				{
				setState(194);
				_la = _input.LA(1);
				if ( !(_la==LANG || _la==LANGUAGE) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(195);
				match(LPARENCHAR);
				setState(196);
				_la = _input.LA(1);
				if ( !(((((_la - 29)) & ~0x3f) == 0 && ((1L << (_la - 29)) & 30786331869185L) != 0) || _la==UE) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(197);
				match(RPARENCHAR);
				}
				break;
			case 38:
				enterOuterAlt(_localctx, 38);
				{
				setState(198);
				match(LEASM);
				}
				break;
			case 39:
				enterOuterAlt(_localctx, 39);
				{
				setState(199);
				match(LENGTH);
				}
				break;
			case 40:
				enterOuterAlt(_localctx, 40);
				{
				setState(200);
				match(LIB);
				}
				break;
			case 41:
				enterOuterAlt(_localctx, 41);
				{
				setState(201);
				match(LIN);
				}
				break;
			case 42:
				enterOuterAlt(_localctx, 42);
				{
				setState(202);
				_la = _input.LA(1);
				if ( !(_la==LC || _la==LINECOUNT) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(203);
				match(LPARENCHAR);
				setState(204);
				literal();
				setState(205);
				match(RPARENCHAR);
				}
				break;
			case 43:
				enterOuterAlt(_localctx, 43);
				{
				setState(207);
				match(LINKAGE);
				}
				break;
			case 44:
				enterOuterAlt(_localctx, 44);
				{
				setState(208);
				match(LIST);
				}
				break;
			case 45:
				enterOuterAlt(_localctx, 45);
				{
				setState(209);
				match(MAP);
				}
				break;
			case 46:
				enterOuterAlt(_localctx, 46);
				{
				setState(210);
				match(MARGINS);
				setState(211);
				match(LPARENCHAR);
				setState(212);
				literal();
				setState(213);
				match(COMMACHAR);
				setState(214);
				literal();
				setState(217);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMACHAR) {
					{
					setState(215);
					match(COMMACHAR);
					setState(216);
					literal();
					}
				}

				setState(219);
				match(RPARENCHAR);
				}
				break;
			case 47:
				enterOuterAlt(_localctx, 47);
				{
				setState(221);
				_la = _input.LA(1);
				if ( !(_la==MD || _la==MDECK) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(225);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
				case 1:
					{
					setState(222);
					match(LPARENCHAR);
					setState(223);
					_la = _input.LA(1);
					if ( !(_la==COMPILE || _la==NOC || _la==NOCOMPILE || _la==C_CHAR) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(224);
					match(RPARENCHAR);
					}
					break;
				}
				}
				break;
			case 48:
				enterOuterAlt(_localctx, 48);
				{
				setState(227);
				match(NAME);
				setState(231);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
				case 1:
					{
					setState(228);
					match(LPARENCHAR);
					setState(229);
					_la = _input.LA(1);
					if ( !(_la==ALIAS || _la==NOALIAS) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(230);
					match(RPARENCHAR);
					}
					break;
				}
				}
				break;
			case 49:
				enterOuterAlt(_localctx, 49);
				{
				setState(233);
				match(NATLANG);
				setState(234);
				match(LPARENCHAR);
				setState(235);
				_la = _input.LA(1);
				if ( !(((((_la - 29)) & ~0x3f) == 0 && ((1L << (_la - 29)) & 17592188141569L) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(236);
				match(RPARENCHAR);
				}
				break;
			case 50:
				enterOuterAlt(_localctx, 50);
				{
				setState(237);
				match(NOADATA);
				}
				break;
			case 51:
				enterOuterAlt(_localctx, 51);
				{
				setState(238);
				match(NOADV);
				}
				break;
			case 52:
				enterOuterAlt(_localctx, 52);
				{
				setState(239);
				match(NOAWO);
				}
				break;
			case 53:
				enterOuterAlt(_localctx, 53);
				{
				setState(240);
				match(NOBLOCK0);
				}
				break;
			case 54:
				enterOuterAlt(_localctx, 54);
				{
				setState(241);
				match(NOCBLCARD);
				}
				break;
			case 55:
				enterOuterAlt(_localctx, 55);
				{
				setState(242);
				match(NOCICS);
				}
				break;
			case 56:
				enterOuterAlt(_localctx, 56);
				{
				setState(243);
				match(NOCMPR2);
				}
				break;
			case 57:
				enterOuterAlt(_localctx, 57);
				{
				setState(244);
				_la = _input.LA(1);
				if ( !(_la==NOC || _la==NOCOMPILE) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(248);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,17,_ctx) ) {
				case 1:
					{
					setState(245);
					match(LPARENCHAR);
					setState(246);
					_la = _input.LA(1);
					if ( !(((((_la - 270)) & ~0x3f) == 0 && ((1L << (_la - 270)) & 641L) != 0)) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(247);
					match(RPARENCHAR);
					}
					break;
				}
				}
				break;
			case 58:
				enterOuterAlt(_localctx, 58);
				{
				setState(250);
				match(NOCPSM);
				}
				break;
			case 59:
				enterOuterAlt(_localctx, 59);
				{
				setState(251);
				_la = _input.LA(1);
				if ( !(_la==NOCURR || _la==NOCURRENCY) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 60:
				enterOuterAlt(_localctx, 60);
				{
				setState(252);
				_la = _input.LA(1);
				if ( !(_la==NODATEPROC || _la==NODP) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 61:
				enterOuterAlt(_localctx, 61);
				{
				setState(253);
				match(NODBCS);
				}
				break;
			case 62:
				enterOuterAlt(_localctx, 62);
				{
				setState(254);
				match(NODEBUG);
				}
				break;
			case 63:
				enterOuterAlt(_localctx, 63);
				{
				setState(255);
				_la = _input.LA(1);
				if ( !(_la==NOD || _la==NODECK) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 64:
				enterOuterAlt(_localctx, 64);
				{
				setState(256);
				match(NODLL);
				}
				break;
			case 65:
				enterOuterAlt(_localctx, 65);
				{
				setState(257);
				match(NODE);
				}
				break;
			case 66:
				enterOuterAlt(_localctx, 66);
				{
				setState(258);
				_la = _input.LA(1);
				if ( !(_la==NODU || _la==NODUMP) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 67:
				enterOuterAlt(_localctx, 67);
				{
				setState(259);
				_la = _input.LA(1);
				if ( !(_la==NODIAGTRUNC || _la==NODTR) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 68:
				enterOuterAlt(_localctx, 68);
				{
				setState(260);
				_la = _input.LA(1);
				if ( !(_la==NODYN || _la==NODYNAM) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 69:
				enterOuterAlt(_localctx, 69);
				{
				setState(261);
				match(NOEDF);
				}
				break;
			case 70:
				enterOuterAlt(_localctx, 70);
				{
				setState(262);
				match(NOEPILOG);
				}
				break;
			case 71:
				enterOuterAlt(_localctx, 71);
				{
				setState(263);
				match(NOEXIT);
				}
				break;
			case 72:
				enterOuterAlt(_localctx, 72);
				{
				setState(264);
				_la = _input.LA(1);
				if ( !(_la==NOEXP || _la==NOEXPORTALL) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 73:
				enterOuterAlt(_localctx, 73);
				{
				setState(265);
				_la = _input.LA(1);
				if ( !(_la==NOFASTSRT || _la==NOFSRT) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 74:
				enterOuterAlt(_localctx, 74);
				{
				setState(266);
				match(NOFEPI);
				}
				break;
			case 75:
				enterOuterAlt(_localctx, 75);
				{
				setState(267);
				_la = _input.LA(1);
				if ( !(_la==NOF || _la==NOFLAG) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 76:
				enterOuterAlt(_localctx, 76);
				{
				setState(268);
				match(NOFLAGMIG);
				}
				break;
			case 77:
				enterOuterAlt(_localctx, 77);
				{
				setState(269);
				match(NOFLAGSTD);
				}
				break;
			case 78:
				enterOuterAlt(_localctx, 78);
				{
				setState(270);
				match(NOGRAPHIC);
				}
				break;
			case 79:
				enterOuterAlt(_localctx, 79);
				{
				setState(271);
				match(NOLENGTH);
				}
				break;
			case 80:
				enterOuterAlt(_localctx, 80);
				{
				setState(272);
				match(NOLIB);
				}
				break;
			case 81:
				enterOuterAlt(_localctx, 81);
				{
				setState(273);
				match(NOLINKAGE);
				}
				break;
			case 82:
				enterOuterAlt(_localctx, 82);
				{
				setState(274);
				match(NOLIST);
				}
				break;
			case 83:
				enterOuterAlt(_localctx, 83);
				{
				setState(275);
				match(NOMAP);
				}
				break;
			case 84:
				enterOuterAlt(_localctx, 84);
				{
				setState(276);
				_la = _input.LA(1);
				if ( !(_la==NOMD || _la==NOMDECK) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 85:
				enterOuterAlt(_localctx, 85);
				{
				setState(277);
				match(NONAME);
				}
				break;
			case 86:
				enterOuterAlt(_localctx, 86);
				{
				setState(278);
				_la = _input.LA(1);
				if ( !(_la==NONUM || _la==NONUMBER) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 87:
				enterOuterAlt(_localctx, 87);
				{
				setState(279);
				_la = _input.LA(1);
				if ( !(_la==NOOBJ || _la==NOOBJECT) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 88:
				enterOuterAlt(_localctx, 88);
				{
				setState(280);
				_la = _input.LA(1);
				if ( !(_la==NOOFF || _la==NOOFFSET) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 89:
				enterOuterAlt(_localctx, 89);
				{
				setState(281);
				match(NOOPSEQUENCE);
				}
				break;
			case 90:
				enterOuterAlt(_localctx, 90);
				{
				setState(282);
				_la = _input.LA(1);
				if ( !(_la==NOOPT || _la==NOOPTIMIZE) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 91:
				enterOuterAlt(_localctx, 91);
				{
				setState(283);
				match(NOOPTIONS);
				}
				break;
			case 92:
				enterOuterAlt(_localctx, 92);
				{
				setState(284);
				match(NOP);
				}
				break;
			case 93:
				enterOuterAlt(_localctx, 93);
				{
				setState(285);
				match(NOPROLOG);
				}
				break;
			case 94:
				enterOuterAlt(_localctx, 94);
				{
				setState(286);
				match(NORENT);
				}
				break;
			case 95:
				enterOuterAlt(_localctx, 95);
				{
				setState(287);
				_la = _input.LA(1);
				if ( !(_la==NOSEQ || _la==NOSEQUENCE) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 96:
				enterOuterAlt(_localctx, 96);
				{
				setState(288);
				_la = _input.LA(1);
				if ( !(_la==NOS || _la==NOSOURCE) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 97:
				enterOuterAlt(_localctx, 97);
				{
				setState(289);
				match(NOSPIE);
				}
				break;
			case 98:
				enterOuterAlt(_localctx, 98);
				{
				setState(290);
				match(NOSQL);
				}
				break;
			case 99:
				enterOuterAlt(_localctx, 99);
				{
				setState(291);
				_la = _input.LA(1);
				if ( !(_la==NOSQLC || _la==NOSQLCCSID) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 100:
				enterOuterAlt(_localctx, 100);
				{
				setState(292);
				_la = _input.LA(1);
				if ( !(_la==NOSSR || _la==NOSSRANGE) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 101:
				enterOuterAlt(_localctx, 101);
				{
				setState(293);
				match(NOSTDTRUNC);
				}
				break;
			case 102:
				enterOuterAlt(_localctx, 102);
				{
				setState(294);
				_la = _input.LA(1);
				if ( !(_la==NOTERM || _la==NOTERMINAL) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 103:
				enterOuterAlt(_localctx, 103);
				{
				setState(295);
				match(NOTEST);
				}
				break;
			case 104:
				enterOuterAlt(_localctx, 104);
				{
				setState(296);
				match(NOTHREAD);
				}
				break;
			case 105:
				enterOuterAlt(_localctx, 105);
				{
				setState(297);
				match(NOVBREF);
				}
				break;
			case 106:
				enterOuterAlt(_localctx, 106);
				{
				setState(298);
				_la = _input.LA(1);
				if ( !(_la==NOWD || _la==NOWORD) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 107:
				enterOuterAlt(_localctx, 107);
				{
				setState(299);
				match(NSEQ);
				}
				break;
			case 108:
				enterOuterAlt(_localctx, 108);
				{
				setState(300);
				_la = _input.LA(1);
				if ( !(_la==NS || _la==NSYMBOL) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(301);
				match(LPARENCHAR);
				setState(302);
				_la = _input.LA(1);
				if ( !(_la==DBCS || _la==NAT || _la==NATIONAL) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(303);
				match(RPARENCHAR);
				}
				break;
			case 109:
				enterOuterAlt(_localctx, 109);
				{
				setState(304);
				match(NOVBREF);
				}
				break;
			case 110:
				enterOuterAlt(_localctx, 110);
				{
				setState(305);
				_la = _input.LA(1);
				if ( !(_la==NOX || _la==NOXREF) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 111:
				enterOuterAlt(_localctx, 111);
				{
				setState(306);
				match(NOZWB);
				}
				break;
			case 112:
				enterOuterAlt(_localctx, 112);
				{
				setState(307);
				_la = _input.LA(1);
				if ( !(_la==NUM || _la==NUMBER) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 113:
				enterOuterAlt(_localctx, 113);
				{
				setState(308);
				match(NUMPROC);
				setState(309);
				match(LPARENCHAR);
				setState(310);
				_la = _input.LA(1);
				if ( !(_la==MIG || _la==NOPFD || _la==PFD) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(311);
				match(RPARENCHAR);
				}
				break;
			case 114:
				enterOuterAlt(_localctx, 114);
				{
				setState(312);
				_la = _input.LA(1);
				if ( !(_la==OBJ || _la==OBJECT) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 115:
				enterOuterAlt(_localctx, 115);
				{
				setState(313);
				_la = _input.LA(1);
				if ( !(_la==OFF || _la==OFFSET) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 116:
				enterOuterAlt(_localctx, 116);
				{
				setState(314);
				match(OPMARGINS);
				setState(315);
				match(LPARENCHAR);
				setState(316);
				literal();
				setState(317);
				match(COMMACHAR);
				setState(318);
				literal();
				setState(321);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==COMMACHAR) {
					{
					setState(319);
					match(COMMACHAR);
					setState(320);
					literal();
					}
				}

				setState(323);
				match(RPARENCHAR);
				}
				break;
			case 117:
				enterOuterAlt(_localctx, 117);
				{
				setState(325);
				match(OPSEQUENCE);
				setState(326);
				match(LPARENCHAR);
				setState(327);
				literal();
				setState(328);
				match(COMMACHAR);
				setState(329);
				literal();
				setState(330);
				match(RPARENCHAR);
				}
				break;
			case 118:
				enterOuterAlt(_localctx, 118);
				{
				setState(332);
				_la = _input.LA(1);
				if ( !(_la==OPT || _la==OPTIMIZE) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(336);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,19,_ctx) ) {
				case 1:
					{
					setState(333);
					match(LPARENCHAR);
					setState(334);
					_la = _input.LA(1);
					if ( !(_la==FULL || _la==STD) ) {
					_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(335);
					match(RPARENCHAR);
					}
					break;
				}
				}
				break;
			case 119:
				enterOuterAlt(_localctx, 119);
				{
				setState(338);
				match(OPTFILE);
				}
				break;
			case 120:
				enterOuterAlt(_localctx, 120);
				{
				setState(339);
				match(OPTIONS);
				}
				break;
			case 121:
				enterOuterAlt(_localctx, 121);
				{
				setState(340);
				match(OP);
				}
				break;
			case 122:
				enterOuterAlt(_localctx, 122);
				{
				setState(341);
				_la = _input.LA(1);
				if ( !(_la==OUT || _la==OUTDD) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(342);
				match(LPARENCHAR);
				setState(343);
				cobolWord();
				setState(344);
				match(RPARENCHAR);
				}
				break;
			case 123:
				enterOuterAlt(_localctx, 123);
				{
				setState(346);
				_la = _input.LA(1);
				if ( !(_la==PGMN || _la==PGMNAME) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(347);
				match(LPARENCHAR);
				setState(348);
				_la = _input.LA(1);
				if ( !(_la==CO || _la==COMPAT || ((((_la - 85)) & ~0x3f) == 0 && ((1L << (_la - 85)) & 2071L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 4456449L) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(349);
				match(RPARENCHAR);
				}
				break;
			case 124:
				enterOuterAlt(_localctx, 124);
				{
				setState(350);
				match(PROLOG);
				}
				break;
			case 125:
				enterOuterAlt(_localctx, 125);
				{
				setState(351);
				_la = _input.LA(1);
				if ( !(_la==QUOTE || _la==Q_CHAR) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 126:
				enterOuterAlt(_localctx, 126);
				{
				setState(352);
				match(RENT);
				}
				break;
			case 127:
				enterOuterAlt(_localctx, 127);
				{
				setState(353);
				match(RMODE);
				setState(354);
				match(LPARENCHAR);
				setState(358);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case ANY:
					{
					setState(355);
					match(ANY);
					}
					break;
				case AUTO:
					{
					setState(356);
					match(AUTO);
					}
					break;
				case NONNUMERICLITERAL:
				case NUMERICLITERAL:
					{
					setState(357);
					literal();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(360);
				match(RPARENCHAR);
				}
				break;
			case 128:
				enterOuterAlt(_localctx, 128);
				{
				setState(361);
				_la = _input.LA(1);
				if ( !(_la==SEQ || _la==SEQUENCE) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(368);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,21,_ctx) ) {
				case 1:
					{
					setState(362);
					match(LPARENCHAR);
					setState(363);
					literal();
					setState(364);
					match(COMMACHAR);
					setState(365);
					literal();
					setState(366);
					match(RPARENCHAR);
					}
					break;
				}
				}
				break;
			case 129:
				enterOuterAlt(_localctx, 129);
				{
				setState(370);
				_la = _input.LA(1);
				if ( !(_la==SIZE || _la==SZ) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(371);
				match(LPARENCHAR);
				setState(374);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case MAX:
					{
					setState(372);
					match(MAX);
					}
					break;
				case NONNUMERICLITERAL:
				case NUMERICLITERAL:
					{
					setState(373);
					literal();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(376);
				match(RPARENCHAR);
				}
				break;
			case 130:
				enterOuterAlt(_localctx, 130);
				{
				setState(377);
				_la = _input.LA(1);
				if ( !(_la==SOURCE || _la==S_CHAR) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 131:
				enterOuterAlt(_localctx, 131);
				{
				setState(378);
				match(SP);
				}
				break;
			case 132:
				enterOuterAlt(_localctx, 132);
				{
				setState(379);
				match(SPACE);
				setState(380);
				match(LPARENCHAR);
				setState(381);
				literal();
				setState(382);
				match(RPARENCHAR);
				}
				break;
			case 133:
				enterOuterAlt(_localctx, 133);
				{
				setState(384);
				match(SPIE);
				}
				break;
			case 134:
				enterOuterAlt(_localctx, 134);
				{
				setState(385);
				match(SQL);
				setState(390);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,23,_ctx) ) {
				case 1:
					{
					setState(386);
					match(LPARENCHAR);
					setState(387);
					literal();
					setState(388);
					match(RPARENCHAR);
					}
					break;
				}
				}
				break;
			case 135:
				enterOuterAlt(_localctx, 135);
				{
				setState(392);
				_la = _input.LA(1);
				if ( !(_la==SQLC || _la==SQLCCSID) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 136:
				enterOuterAlt(_localctx, 136);
				{
				setState(393);
				_la = _input.LA(1);
				if ( !(_la==SSR || _la==SSRANGE) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 137:
				enterOuterAlt(_localctx, 137);
				{
				setState(394);
				match(SYSEIB);
				}
				break;
			case 138:
				enterOuterAlt(_localctx, 138);
				{
				setState(395);
				_la = _input.LA(1);
				if ( !(_la==TERM || _la==TERMINAL) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			case 139:
				enterOuterAlt(_localctx, 139);
				{
				setState(396);
				match(TEST);
				setState(414);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,29,_ctx) ) {
				case 1:
					{
					setState(397);
					match(LPARENCHAR);
					setState(399);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==HOOK || _la==NOHOOK) {
						{
						setState(398);
						_la = _input.LA(1);
						if ( !(_la==HOOK || _la==NOHOOK) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						}
					}

					setState(402);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,25,_ctx) ) {
					case 1:
						{
						setState(401);
						match(COMMACHAR);
						}
						break;
					}
					setState(405);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (((((_la - 168)) & ~0x3f) == 0 && ((1L << (_la - 168)) & 216172782113783811L) != 0)) {
						{
						setState(404);
						_la = _input.LA(1);
						if ( !(((((_la - 168)) & ~0x3f) == 0 && ((1L << (_la - 168)) & 216172782113783811L) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						}
					}

					setState(408);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==COMMACHAR) {
						{
						setState(407);
						match(COMMACHAR);
						}
					}

					setState(411);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==EJPD || _la==NOEJPD) {
						{
						setState(410);
						_la = _input.LA(1);
						if ( !(_la==EJPD || _la==NOEJPD) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						}
					}

					setState(413);
					match(RPARENCHAR);
					}
					break;
				}
				}
				break;
			case 140:
				enterOuterAlt(_localctx, 140);
				{
				setState(416);
				match(THREAD);
				}
				break;
			case 141:
				enterOuterAlt(_localctx, 141);
				{
				setState(417);
				match(TRUNC);
				setState(418);
				match(LPARENCHAR);
				setState(419);
				_la = _input.LA(1);
				if ( !(_la==BIN || _la==OPT || _la==STD) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(420);
				match(RPARENCHAR);
				}
				break;
			case 142:
				enterOuterAlt(_localctx, 142);
				{
				setState(421);
				match(VBREF);
				}
				break;
			case 143:
				enterOuterAlt(_localctx, 143);
				{
				setState(422);
				_la = _input.LA(1);
				if ( !(_la==WD || _la==WORD) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(423);
				match(LPARENCHAR);
				setState(424);
				cobolWord();
				setState(425);
				match(RPARENCHAR);
				}
				break;
			case 144:
				enterOuterAlt(_localctx, 144);
				{
				setState(427);
				_la = _input.LA(1);
				if ( !(_la==XMLPARSE || _la==XP) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(428);
				match(LPARENCHAR);
				setState(429);
				_la = _input.LA(1);
				if ( !(_la==COMPAT || ((((_la - 261)) & ~0x3f) == 0 && ((1L << (_la - 261)) & 524417L) != 0)) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(430);
				match(RPARENCHAR);
				}
				break;
			case 145:
				enterOuterAlt(_localctx, 145);
				{
				setState(431);
				_la = _input.LA(1);
				if ( !(_la==XREF || _la==X_CHAR) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(437);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,31,_ctx) ) {
				case 1:
					{
					setState(432);
					match(LPARENCHAR);
					setState(434);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==FULL || _la==SHORT) {
						{
						setState(433);
						_la = _input.LA(1);
						if ( !(_la==FULL || _la==SHORT) ) {
						_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						}
					}

					setState(436);
					match(RPARENCHAR);
					}
					break;
				}
				}
				break;
			case 146:
				enterOuterAlt(_localctx, 146);
				{
				setState(439);
				_la = _input.LA(1);
				if ( !(_la==YEARWINDOW || _la==YW) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(440);
				match(LPARENCHAR);
				setState(441);
				literal();
				setState(442);
				match(RPARENCHAR);
				}
				break;
			case 147:
				enterOuterAlt(_localctx, 147);
				{
				setState(444);
				match(ZWB);
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

	@SuppressWarnings("CheckReturnValue")
	public static class ExecCicsStatementContext extends ParserRuleContext {
		public TerminalNode EXEC() { return getToken(Cobol85PreprocessorParser.EXEC, 0); }
		public TerminalNode CICS() { return getToken(Cobol85PreprocessorParser.CICS, 0); }
		public CharDataContext charData() {
			return getRuleContext(CharDataContext.class,0);
		}
		public TerminalNode END_EXEC() { return getToken(Cobol85PreprocessorParser.END_EXEC, 0); }
		public TerminalNode DOT() { return getToken(Cobol85PreprocessorParser.DOT, 0); }
		public ExecCicsStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_execCicsStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterExecCicsStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitExecCicsStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitExecCicsStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExecCicsStatementContext execCicsStatement() throws RecognitionException {
		ExecCicsStatementContext _localctx = new ExecCicsStatementContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_execCicsStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(447);
			match(EXEC);
			setState(448);
			match(CICS);
			setState(449);
			charData();
			setState(450);
			match(END_EXEC);
			setState(452);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,33,_ctx) ) {
			case 1:
				{
				setState(451);
				match(DOT);
				}
				break;
			}
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

	@SuppressWarnings("CheckReturnValue")
	public static class ExecSqlStatementContext extends ParserRuleContext {
		public TerminalNode EXEC() { return getToken(Cobol85PreprocessorParser.EXEC, 0); }
		public TerminalNode SQL() { return getToken(Cobol85PreprocessorParser.SQL, 0); }
		public CharDataSqlContext charDataSql() {
			return getRuleContext(CharDataSqlContext.class,0);
		}
		public TerminalNode END_EXEC() { return getToken(Cobol85PreprocessorParser.END_EXEC, 0); }
		public TerminalNode DOT() { return getToken(Cobol85PreprocessorParser.DOT, 0); }
		public ExecSqlStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_execSqlStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterExecSqlStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitExecSqlStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitExecSqlStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExecSqlStatementContext execSqlStatement() throws RecognitionException {
		ExecSqlStatementContext _localctx = new ExecSqlStatementContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_execSqlStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(454);
			match(EXEC);
			setState(455);
			match(SQL);
			setState(456);
			charDataSql();
			setState(457);
			match(END_EXEC);
			setState(459);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,34,_ctx) ) {
			case 1:
				{
				setState(458);
				match(DOT);
				}
				break;
			}
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

	@SuppressWarnings("CheckReturnValue")
	public static class ExecSqlImsStatementContext extends ParserRuleContext {
		public TerminalNode EXEC() { return getToken(Cobol85PreprocessorParser.EXEC, 0); }
		public TerminalNode SQLIMS() { return getToken(Cobol85PreprocessorParser.SQLIMS, 0); }
		public CharDataContext charData() {
			return getRuleContext(CharDataContext.class,0);
		}
		public TerminalNode END_EXEC() { return getToken(Cobol85PreprocessorParser.END_EXEC, 0); }
		public TerminalNode DOT() { return getToken(Cobol85PreprocessorParser.DOT, 0); }
		public ExecSqlImsStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_execSqlImsStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterExecSqlImsStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitExecSqlImsStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitExecSqlImsStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExecSqlImsStatementContext execSqlImsStatement() throws RecognitionException {
		ExecSqlImsStatementContext _localctx = new ExecSqlImsStatementContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_execSqlImsStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(461);
			match(EXEC);
			setState(462);
			match(SQLIMS);
			setState(463);
			charData();
			setState(464);
			match(END_EXEC);
			setState(466);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,35,_ctx) ) {
			case 1:
				{
				setState(465);
				match(DOT);
				}
				break;
			}
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

	@SuppressWarnings("CheckReturnValue")
	public static class CopyStatementContext extends ParserRuleContext {
		public TerminalNode COPY() { return getToken(Cobol85PreprocessorParser.COPY, 0); }
		public CopySourceContext copySource() {
			return getRuleContext(CopySourceContext.class,0);
		}
		public TerminalNode DOT() { return getToken(Cobol85PreprocessorParser.DOT, 0); }
		public List<TerminalNode> NEWLINE() { return getTokens(Cobol85PreprocessorParser.NEWLINE); }
		public TerminalNode NEWLINE(int i) {
			return getToken(Cobol85PreprocessorParser.NEWLINE, i);
		}
		public List<DirectoryPhraseContext> directoryPhrase() {
			return getRuleContexts(DirectoryPhraseContext.class);
		}
		public DirectoryPhraseContext directoryPhrase(int i) {
			return getRuleContext(DirectoryPhraseContext.class,i);
		}
		public List<FamilyPhraseContext> familyPhrase() {
			return getRuleContexts(FamilyPhraseContext.class);
		}
		public FamilyPhraseContext familyPhrase(int i) {
			return getRuleContext(FamilyPhraseContext.class,i);
		}
		public List<ReplacingPhraseContext> replacingPhrase() {
			return getRuleContexts(ReplacingPhraseContext.class);
		}
		public ReplacingPhraseContext replacingPhrase(int i) {
			return getRuleContext(ReplacingPhraseContext.class,i);
		}
		public List<TerminalNode> SUPPRESS() { return getTokens(Cobol85PreprocessorParser.SUPPRESS); }
		public TerminalNode SUPPRESS(int i) {
			return getToken(Cobol85PreprocessorParser.SUPPRESS, i);
		}
		public CopyStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_copyStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterCopyStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitCopyStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitCopyStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CopyStatementContext copyStatement() throws RecognitionException {
		CopyStatementContext _localctx = new CopyStatementContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_copyStatement);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(468);
			match(COPY);
			setState(469);
			copySource();
			setState(484);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,38,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(473);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==NEWLINE) {
						{
						{
						setState(470);
						match(NEWLINE);
						}
						}
						setState(475);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					setState(480);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case IN:
					case OF:
						{
						setState(476);
						directoryPhrase();
						}
						break;
					case ON:
						{
						setState(477);
						familyPhrase();
						}
						break;
					case REPLACING:
						{
						setState(478);
						replacingPhrase();
						}
						break;
					case SUPPRESS:
						{
						setState(479);
						match(SUPPRESS);
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					}
					} 
				}
				setState(486);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,38,_ctx);
			}
			setState(490);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==NEWLINE) {
				{
				{
				setState(487);
				match(NEWLINE);
				}
				}
				setState(492);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(493);
			match(DOT);
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

	@SuppressWarnings("CheckReturnValue")
	public static class CopySourceContext extends ParserRuleContext {
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public CobolWordContext cobolWord() {
			return getRuleContext(CobolWordContext.class,0);
		}
		public FilenameContext filename() {
			return getRuleContext(FilenameContext.class,0);
		}
		public CopyLibraryContext copyLibrary() {
			return getRuleContext(CopyLibraryContext.class,0);
		}
		public TerminalNode OF() { return getToken(Cobol85PreprocessorParser.OF, 0); }
		public TerminalNode IN() { return getToken(Cobol85PreprocessorParser.IN, 0); }
		public CopySourceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_copySource; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterCopySource(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitCopySource(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitCopySource(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CopySourceContext copySource() throws RecognitionException {
		CopySourceContext _localctx = new CopySourceContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_copySource);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(498);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NONNUMERICLITERAL:
			case NUMERICLITERAL:
				{
				setState(495);
				literal();
				}
				break;
			case ADATA:
			case ADV:
			case ALIAS:
			case ANSI:
			case ANY:
			case APOST:
			case AR:
			case ARITH:
			case AUTO:
			case AWO:
			case BIN:
			case BLOCK0:
			case BUF:
			case BUFSIZE:
			case BY:
			case CBL:
			case CBLCARD:
			case CO:
			case COBOL2:
			case COBOL3:
			case CODEPAGE:
			case COMPAT:
			case COMPILE:
			case CP:
			case CPP:
			case CPSM:
			case CS:
			case CURR:
			case CURRENCY:
			case DATA:
			case DATEPROC:
			case DBCS:
			case DD:
			case DEBUG:
			case DECK:
			case DIAGTRUNC:
			case DLI:
			case DLL:
			case DP:
			case DTR:
			case DU:
			case DUMP:
			case DYN:
			case DYNAM:
			case EDF:
			case EJECT:
			case EJPD:
			case EN:
			case ENGLISH:
			case EPILOG:
			case EXCI:
			case EXIT:
			case EXP:
			case EXPORTALL:
			case EXTEND:
			case FASTSRT:
			case FLAG:
			case FLAGSTD:
			case FSRT:
			case FULL:
			case GDS:
			case GRAPHIC:
			case HOOK:
			case IN:
			case INTDATE:
			case JA:
			case JP:
			case KA:
			case LANG:
			case LANGUAGE:
			case LC:
			case LENGTH:
			case LIB:
			case LILIAN:
			case LIN:
			case LINECOUNT:
			case LINKAGE:
			case LIST:
			case LM:
			case LONGMIXED:
			case LONGUPPER:
			case LU:
			case MAP:
			case MARGINS:
			case MAX:
			case MD:
			case MDECK:
			case MIG:
			case MIXED:
			case NAME:
			case NAT:
			case NATIONAL:
			case NATLANG:
			case NN:
			case NO:
			case NOADATA:
			case NOADV:
			case NOALIAS:
			case NOAWO:
			case NOBLOCK0:
			case NOC:
			case NOCBLCARD:
			case NOCICS:
			case NOCMPR2:
			case NOCOMPILE:
			case NOCPSM:
			case NOCURR:
			case NOCURRENCY:
			case NOD:
			case NODATEPROC:
			case NODBCS:
			case NODE:
			case NODEBUG:
			case NODECK:
			case NODIAGTRUNC:
			case NODLL:
			case NODU:
			case NODUMP:
			case NODP:
			case NODTR:
			case NODYN:
			case NODYNAM:
			case NOEDF:
			case NOEJPD:
			case NOEPILOG:
			case NOEXIT:
			case NOEXP:
			case NOEXPORTALL:
			case NOF:
			case NOFASTSRT:
			case NOFEPI:
			case NOFLAG:
			case NOFLAGMIG:
			case NOFLAGSTD:
			case NOFSRT:
			case NOGRAPHIC:
			case NOHOOK:
			case NOLENGTH:
			case NOLIB:
			case NOLINKAGE:
			case NOLIST:
			case NOMAP:
			case NOMD:
			case NOMDECK:
			case NONAME:
			case NONUM:
			case NONUMBER:
			case NOOBJ:
			case NOOBJECT:
			case NOOFF:
			case NOOFFSET:
			case NOOPSEQUENCE:
			case NOOPT:
			case NOOPTIMIZE:
			case NOOPTIONS:
			case NOP:
			case NOPFD:
			case NOPROLOG:
			case NORENT:
			case NOS:
			case NOSEP:
			case NOSEPARATE:
			case NOSEQ:
			case NOSOURCE:
			case NOSPIE:
			case NOSQL:
			case NOSQLC:
			case NOSQLCCSID:
			case NOSSR:
			case NOSSRANGE:
			case NOSTDTRUNC:
			case NOSEQUENCE:
			case NOTERM:
			case NOTERMINAL:
			case NOTEST:
			case NOTHREAD:
			case NOTRIG:
			case NOVBREF:
			case NOWORD:
			case NOX:
			case NOXREF:
			case NOZWB:
			case NS:
			case NSEQ:
			case NSYMBOL:
			case NUM:
			case NUMBER:
			case NUMPROC:
			case OBJ:
			case OBJECT:
			case OF:
			case OFF:
			case OFFSET:
			case ON:
			case OP:
			case OPMARGINS:
			case OPSEQUENCE:
			case OPT:
			case OPTFILE:
			case OPTIMIZE:
			case OPTIONS:
			case OUT:
			case OUTDD:
			case PFD:
			case PPTDBG:
			case PGMN:
			case PGMNAME:
			case PROCESS:
			case PROLOG:
			case QUOTE:
			case RENT:
			case REPLACING:
			case RMODE:
			case SEP:
			case SEPARATE:
			case SEQ:
			case SEQUENCE:
			case SHORT:
			case SIZE:
			case SOURCE:
			case SP:
			case SPACE:
			case SPIE:
			case SQL:
			case SQLC:
			case SQLCCSID:
			case SS:
			case SSR:
			case SSRANGE:
			case STD:
			case SYSEIB:
			case SZ:
			case TERM:
			case TERMINAL:
			case TEST:
			case THREAD:
			case TITLE:
			case TRIG:
			case TRUNC:
			case UE:
			case UPPER:
			case VBREF:
			case WD:
			case XMLPARSE:
			case XMLSS:
			case XOPTS:
			case XREF:
			case YEARWINDOW:
			case YW:
			case ZWB:
			case C_CHAR:
			case D_CHAR:
			case E_CHAR:
			case F_CHAR:
			case H_CHAR:
			case I_CHAR:
			case M_CHAR:
			case N_CHAR:
			case Q_CHAR:
			case S_CHAR:
			case U_CHAR:
			case W_CHAR:
			case X_CHAR:
			case COMMACHAR:
			case IDENTIFIER:
				{
				setState(496);
				cobolWord();
				}
				break;
			case FILENAME:
				{
				setState(497);
				filename();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(502);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,41,_ctx) ) {
			case 1:
				{
				setState(500);
				_la = _input.LA(1);
				if ( !(_la==IN || _la==OF) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(501);
				copyLibrary();
				}
				break;
			}
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

	@SuppressWarnings("CheckReturnValue")
	public static class CopyLibraryContext extends ParserRuleContext {
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public CobolWordContext cobolWord() {
			return getRuleContext(CobolWordContext.class,0);
		}
		public CopyLibraryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_copyLibrary; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterCopyLibrary(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitCopyLibrary(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitCopyLibrary(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CopyLibraryContext copyLibrary() throws RecognitionException {
		CopyLibraryContext _localctx = new CopyLibraryContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_copyLibrary);
		try {
			setState(506);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NONNUMERICLITERAL:
			case NUMERICLITERAL:
				enterOuterAlt(_localctx, 1);
				{
				setState(504);
				literal();
				}
				break;
			case ADATA:
			case ADV:
			case ALIAS:
			case ANSI:
			case ANY:
			case APOST:
			case AR:
			case ARITH:
			case AUTO:
			case AWO:
			case BIN:
			case BLOCK0:
			case BUF:
			case BUFSIZE:
			case BY:
			case CBL:
			case CBLCARD:
			case CO:
			case COBOL2:
			case COBOL3:
			case CODEPAGE:
			case COMPAT:
			case COMPILE:
			case CP:
			case CPP:
			case CPSM:
			case CS:
			case CURR:
			case CURRENCY:
			case DATA:
			case DATEPROC:
			case DBCS:
			case DD:
			case DEBUG:
			case DECK:
			case DIAGTRUNC:
			case DLI:
			case DLL:
			case DP:
			case DTR:
			case DU:
			case DUMP:
			case DYN:
			case DYNAM:
			case EDF:
			case EJECT:
			case EJPD:
			case EN:
			case ENGLISH:
			case EPILOG:
			case EXCI:
			case EXIT:
			case EXP:
			case EXPORTALL:
			case EXTEND:
			case FASTSRT:
			case FLAG:
			case FLAGSTD:
			case FSRT:
			case FULL:
			case GDS:
			case GRAPHIC:
			case HOOK:
			case IN:
			case INTDATE:
			case JA:
			case JP:
			case KA:
			case LANG:
			case LANGUAGE:
			case LC:
			case LENGTH:
			case LIB:
			case LILIAN:
			case LIN:
			case LINECOUNT:
			case LINKAGE:
			case LIST:
			case LM:
			case LONGMIXED:
			case LONGUPPER:
			case LU:
			case MAP:
			case MARGINS:
			case MAX:
			case MD:
			case MDECK:
			case MIG:
			case MIXED:
			case NAME:
			case NAT:
			case NATIONAL:
			case NATLANG:
			case NN:
			case NO:
			case NOADATA:
			case NOADV:
			case NOALIAS:
			case NOAWO:
			case NOBLOCK0:
			case NOC:
			case NOCBLCARD:
			case NOCICS:
			case NOCMPR2:
			case NOCOMPILE:
			case NOCPSM:
			case NOCURR:
			case NOCURRENCY:
			case NOD:
			case NODATEPROC:
			case NODBCS:
			case NODE:
			case NODEBUG:
			case NODECK:
			case NODIAGTRUNC:
			case NODLL:
			case NODU:
			case NODUMP:
			case NODP:
			case NODTR:
			case NODYN:
			case NODYNAM:
			case NOEDF:
			case NOEJPD:
			case NOEPILOG:
			case NOEXIT:
			case NOEXP:
			case NOEXPORTALL:
			case NOF:
			case NOFASTSRT:
			case NOFEPI:
			case NOFLAG:
			case NOFLAGMIG:
			case NOFLAGSTD:
			case NOFSRT:
			case NOGRAPHIC:
			case NOHOOK:
			case NOLENGTH:
			case NOLIB:
			case NOLINKAGE:
			case NOLIST:
			case NOMAP:
			case NOMD:
			case NOMDECK:
			case NONAME:
			case NONUM:
			case NONUMBER:
			case NOOBJ:
			case NOOBJECT:
			case NOOFF:
			case NOOFFSET:
			case NOOPSEQUENCE:
			case NOOPT:
			case NOOPTIMIZE:
			case NOOPTIONS:
			case NOP:
			case NOPFD:
			case NOPROLOG:
			case NORENT:
			case NOS:
			case NOSEP:
			case NOSEPARATE:
			case NOSEQ:
			case NOSOURCE:
			case NOSPIE:
			case NOSQL:
			case NOSQLC:
			case NOSQLCCSID:
			case NOSSR:
			case NOSSRANGE:
			case NOSTDTRUNC:
			case NOSEQUENCE:
			case NOTERM:
			case NOTERMINAL:
			case NOTEST:
			case NOTHREAD:
			case NOTRIG:
			case NOVBREF:
			case NOWORD:
			case NOX:
			case NOXREF:
			case NOZWB:
			case NS:
			case NSEQ:
			case NSYMBOL:
			case NUM:
			case NUMBER:
			case NUMPROC:
			case OBJ:
			case OBJECT:
			case OF:
			case OFF:
			case OFFSET:
			case ON:
			case OP:
			case OPMARGINS:
			case OPSEQUENCE:
			case OPT:
			case OPTFILE:
			case OPTIMIZE:
			case OPTIONS:
			case OUT:
			case OUTDD:
			case PFD:
			case PPTDBG:
			case PGMN:
			case PGMNAME:
			case PROCESS:
			case PROLOG:
			case QUOTE:
			case RENT:
			case REPLACING:
			case RMODE:
			case SEP:
			case SEPARATE:
			case SEQ:
			case SEQUENCE:
			case SHORT:
			case SIZE:
			case SOURCE:
			case SP:
			case SPACE:
			case SPIE:
			case SQL:
			case SQLC:
			case SQLCCSID:
			case SS:
			case SSR:
			case SSRANGE:
			case STD:
			case SYSEIB:
			case SZ:
			case TERM:
			case TERMINAL:
			case TEST:
			case THREAD:
			case TITLE:
			case TRIG:
			case TRUNC:
			case UE:
			case UPPER:
			case VBREF:
			case WD:
			case XMLPARSE:
			case XMLSS:
			case XOPTS:
			case XREF:
			case YEARWINDOW:
			case YW:
			case ZWB:
			case C_CHAR:
			case D_CHAR:
			case E_CHAR:
			case F_CHAR:
			case H_CHAR:
			case I_CHAR:
			case M_CHAR:
			case N_CHAR:
			case Q_CHAR:
			case S_CHAR:
			case U_CHAR:
			case W_CHAR:
			case X_CHAR:
			case COMMACHAR:
			case IDENTIFIER:
				enterOuterAlt(_localctx, 2);
				{
				setState(505);
				cobolWord();
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

	@SuppressWarnings("CheckReturnValue")
	public static class ReplacingPhraseContext extends ParserRuleContext {
		public TerminalNode REPLACING() { return getToken(Cobol85PreprocessorParser.REPLACING, 0); }
		public List<ReplaceClauseContext> replaceClause() {
			return getRuleContexts(ReplaceClauseContext.class);
		}
		public ReplaceClauseContext replaceClause(int i) {
			return getRuleContext(ReplaceClauseContext.class,i);
		}
		public List<TerminalNode> NEWLINE() { return getTokens(Cobol85PreprocessorParser.NEWLINE); }
		public TerminalNode NEWLINE(int i) {
			return getToken(Cobol85PreprocessorParser.NEWLINE, i);
		}
		public ReplacingPhraseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_replacingPhrase; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterReplacingPhrase(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitReplacingPhrase(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitReplacingPhrase(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ReplacingPhraseContext replacingPhrase() throws RecognitionException {
		ReplacingPhraseContext _localctx = new ReplacingPhraseContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_replacingPhrase);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(508);
			match(REPLACING);
			setState(512);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==NEWLINE) {
				{
				{
				setState(509);
				match(NEWLINE);
				}
				}
				setState(514);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(515);
			replaceClause();
			setState(524);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,45,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(517); 
					_errHandler.sync(this);
					_la = _input.LA(1);
					do {
						{
						{
						setState(516);
						match(NEWLINE);
						}
						}
						setState(519); 
						_errHandler.sync(this);
						_la = _input.LA(1);
					} while ( _la==NEWLINE );
					setState(521);
					replaceClause();
					}
					} 
				}
				setState(526);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,45,_ctx);
			}
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

	@SuppressWarnings("CheckReturnValue")
	public static class ReplaceAreaContext extends ParserRuleContext {
		public ReplaceByStatementContext replaceByStatement() {
			return getRuleContext(ReplaceByStatementContext.class,0);
		}
		public List<CopyStatementContext> copyStatement() {
			return getRuleContexts(CopyStatementContext.class);
		}
		public CopyStatementContext copyStatement(int i) {
			return getRuleContext(CopyStatementContext.class,i);
		}
		public List<CharDataContext> charData() {
			return getRuleContexts(CharDataContext.class);
		}
		public CharDataContext charData(int i) {
			return getRuleContext(CharDataContext.class,i);
		}
		public ReplaceOffStatementContext replaceOffStatement() {
			return getRuleContext(ReplaceOffStatementContext.class,0);
		}
		public ReplaceAreaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_replaceArea; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterReplaceArea(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitReplaceArea(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitReplaceArea(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ReplaceAreaContext replaceArea() throws RecognitionException {
		ReplaceAreaContext _localctx = new ReplaceAreaContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_replaceArea);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(527);
			replaceByStatement();
			setState(532);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,47,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					setState(530);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case COPY:
						{
						setState(528);
						copyStatement();
						}
						break;
					case ADATA:
					case ADV:
					case ALIAS:
					case ANSI:
					case ANY:
					case APOST:
					case AR:
					case ARITH:
					case AUTO:
					case AWO:
					case BIN:
					case BLOCK0:
					case BUF:
					case BUFSIZE:
					case BY:
					case CBL:
					case CBLCARD:
					case CO:
					case COBOL2:
					case COBOL3:
					case CODEPAGE:
					case COMPAT:
					case COMPILE:
					case CP:
					case CPP:
					case CPSM:
					case CS:
					case CURR:
					case CURRENCY:
					case DATA:
					case DATEPROC:
					case DBCS:
					case DD:
					case DEBUG:
					case DECK:
					case DIAGTRUNC:
					case DLI:
					case DLL:
					case DP:
					case DTR:
					case DU:
					case DUMP:
					case DYN:
					case DYNAM:
					case EDF:
					case EJECT:
					case EJPD:
					case EN:
					case ENGLISH:
					case EPILOG:
					case EXCI:
					case EXIT:
					case EXP:
					case EXPORTALL:
					case EXTEND:
					case FASTSRT:
					case FLAG:
					case FLAGSTD:
					case FSRT:
					case FULL:
					case GDS:
					case GRAPHIC:
					case HOOK:
					case IN:
					case INTDATE:
					case JA:
					case JP:
					case KA:
					case LANG:
					case LANGUAGE:
					case LC:
					case LENGTH:
					case LIB:
					case LILIAN:
					case LIN:
					case LINECOUNT:
					case LINKAGE:
					case LIST:
					case LM:
					case LONGMIXED:
					case LONGUPPER:
					case LPARENCHAR:
					case LU:
					case MAP:
					case MARGINS:
					case MAX:
					case MD:
					case MDECK:
					case MIG:
					case MIXED:
					case NAME:
					case NAT:
					case NATIONAL:
					case NATLANG:
					case NN:
					case NO:
					case NOADATA:
					case NOADV:
					case NOALIAS:
					case NOAWO:
					case NOBLOCK0:
					case NOC:
					case NOCBLCARD:
					case NOCICS:
					case NOCMPR2:
					case NOCOMPILE:
					case NOCPSM:
					case NOCURR:
					case NOCURRENCY:
					case NOD:
					case NODATEPROC:
					case NODBCS:
					case NODE:
					case NODEBUG:
					case NODECK:
					case NODIAGTRUNC:
					case NODLL:
					case NODU:
					case NODUMP:
					case NODP:
					case NODTR:
					case NODYN:
					case NODYNAM:
					case NOEDF:
					case NOEJPD:
					case NOEPILOG:
					case NOEXIT:
					case NOEXP:
					case NOEXPORTALL:
					case NOF:
					case NOFASTSRT:
					case NOFEPI:
					case NOFLAG:
					case NOFLAGMIG:
					case NOFLAGSTD:
					case NOFSRT:
					case NOGRAPHIC:
					case NOHOOK:
					case NOLENGTH:
					case NOLIB:
					case NOLINKAGE:
					case NOLIST:
					case NOMAP:
					case NOMD:
					case NOMDECK:
					case NONAME:
					case NONUM:
					case NONUMBER:
					case NOOBJ:
					case NOOBJECT:
					case NOOFF:
					case NOOFFSET:
					case NOOPSEQUENCE:
					case NOOPT:
					case NOOPTIMIZE:
					case NOOPTIONS:
					case NOP:
					case NOPFD:
					case NOPROLOG:
					case NORENT:
					case NOS:
					case NOSEP:
					case NOSEPARATE:
					case NOSEQ:
					case NOSOURCE:
					case NOSPIE:
					case NOSQL:
					case NOSQLC:
					case NOSQLCCSID:
					case NOSSR:
					case NOSSRANGE:
					case NOSTDTRUNC:
					case NOSEQUENCE:
					case NOTERM:
					case NOTERMINAL:
					case NOTEST:
					case NOTHREAD:
					case NOTRIG:
					case NOVBREF:
					case NOWORD:
					case NOX:
					case NOXREF:
					case NOZWB:
					case NS:
					case NSEQ:
					case NSYMBOL:
					case NUM:
					case NUMBER:
					case NUMPROC:
					case OBJ:
					case OBJECT:
					case OF:
					case OFF:
					case OFFSET:
					case ON:
					case OP:
					case OPMARGINS:
					case OPSEQUENCE:
					case OPT:
					case OPTFILE:
					case OPTIMIZE:
					case OPTIONS:
					case OUT:
					case OUTDD:
					case PFD:
					case PPTDBG:
					case PGMN:
					case PGMNAME:
					case PROCESS:
					case PROLOG:
					case QUOTE:
					case RENT:
					case REPLACING:
					case RMODE:
					case RPARENCHAR:
					case SEP:
					case SEPARATE:
					case SEQ:
					case SEQUENCE:
					case SHORT:
					case SIZE:
					case SOURCE:
					case SP:
					case SPACE:
					case SPIE:
					case SQL:
					case SQLC:
					case SQLCCSID:
					case SS:
					case SSR:
					case SSRANGE:
					case STD:
					case SYSEIB:
					case SZ:
					case TERM:
					case TERMINAL:
					case TEST:
					case THREAD:
					case TITLE:
					case TRIG:
					case TRUNC:
					case UE:
					case UPPER:
					case VBREF:
					case WD:
					case XMLPARSE:
					case XMLSS:
					case XOPTS:
					case XREF:
					case YEARWINDOW:
					case YW:
					case ZWB:
					case C_CHAR:
					case D_CHAR:
					case E_CHAR:
					case F_CHAR:
					case H_CHAR:
					case I_CHAR:
					case M_CHAR:
					case N_CHAR:
					case Q_CHAR:
					case S_CHAR:
					case U_CHAR:
					case W_CHAR:
					case X_CHAR:
					case COMMACHAR:
					case DOT:
					case NONNUMERICLITERAL:
					case NUMERICLITERAL:
					case IDENTIFIER:
					case FILENAME:
					case NEWLINE:
					case TEXT:
						{
						setState(529);
						charData();
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					} 
				}
				setState(534);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,47,_ctx);
			}
			setState(536);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,48,_ctx) ) {
			case 1:
				{
				setState(535);
				replaceOffStatement();
				}
				break;
			}
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

	@SuppressWarnings("CheckReturnValue")
	public static class ReplaceByStatementContext extends ParserRuleContext {
		public TerminalNode REPLACE() { return getToken(Cobol85PreprocessorParser.REPLACE, 0); }
		public TerminalNode DOT() { return getToken(Cobol85PreprocessorParser.DOT, 0); }
		public List<ReplaceClauseContext> replaceClause() {
			return getRuleContexts(ReplaceClauseContext.class);
		}
		public ReplaceClauseContext replaceClause(int i) {
			return getRuleContext(ReplaceClauseContext.class,i);
		}
		public List<TerminalNode> NEWLINE() { return getTokens(Cobol85PreprocessorParser.NEWLINE); }
		public TerminalNode NEWLINE(int i) {
			return getToken(Cobol85PreprocessorParser.NEWLINE, i);
		}
		public ReplaceByStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_replaceByStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterReplaceByStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitReplaceByStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitReplaceByStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ReplaceByStatementContext replaceByStatement() throws RecognitionException {
		ReplaceByStatementContext _localctx = new ReplaceByStatementContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_replaceByStatement);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(538);
			match(REPLACE);
			setState(546); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(542);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==NEWLINE) {
						{
						{
						setState(539);
						match(NEWLINE);
						}
						}
						setState(544);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					setState(545);
					replaceClause();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(548); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,50,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
			setState(550);
			match(DOT);
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

	@SuppressWarnings("CheckReturnValue")
	public static class ReplaceOffStatementContext extends ParserRuleContext {
		public TerminalNode REPLACE() { return getToken(Cobol85PreprocessorParser.REPLACE, 0); }
		public TerminalNode OFF() { return getToken(Cobol85PreprocessorParser.OFF, 0); }
		public TerminalNode DOT() { return getToken(Cobol85PreprocessorParser.DOT, 0); }
		public ReplaceOffStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_replaceOffStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterReplaceOffStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitReplaceOffStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitReplaceOffStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ReplaceOffStatementContext replaceOffStatement() throws RecognitionException {
		ReplaceOffStatementContext _localctx = new ReplaceOffStatementContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_replaceOffStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(552);
			match(REPLACE);
			setState(553);
			match(OFF);
			setState(554);
			match(DOT);
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

	@SuppressWarnings("CheckReturnValue")
	public static class ReplaceClauseContext extends ParserRuleContext {
		public ReplaceableContext replaceable() {
			return getRuleContext(ReplaceableContext.class,0);
		}
		public TerminalNode BY() { return getToken(Cobol85PreprocessorParser.BY, 0); }
		public ReplacementContext replacement() {
			return getRuleContext(ReplacementContext.class,0);
		}
		public List<TerminalNode> NEWLINE() { return getTokens(Cobol85PreprocessorParser.NEWLINE); }
		public TerminalNode NEWLINE(int i) {
			return getToken(Cobol85PreprocessorParser.NEWLINE, i);
		}
		public DirectoryPhraseContext directoryPhrase() {
			return getRuleContext(DirectoryPhraseContext.class,0);
		}
		public FamilyPhraseContext familyPhrase() {
			return getRuleContext(FamilyPhraseContext.class,0);
		}
		public ReplaceClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_replaceClause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterReplaceClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitReplaceClause(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitReplaceClause(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ReplaceClauseContext replaceClause() throws RecognitionException {
		ReplaceClauseContext _localctx = new ReplaceClauseContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_replaceClause);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(556);
			replaceable();
			setState(560);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==NEWLINE) {
				{
				{
				setState(557);
				match(NEWLINE);
				}
				}
				setState(562);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(563);
			match(BY);
			setState(567);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==NEWLINE) {
				{
				{
				setState(564);
				match(NEWLINE);
				}
				}
				setState(569);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(570);
			replacement();
			setState(578);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,54,_ctx) ) {
			case 1:
				{
				setState(574);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==NEWLINE) {
					{
					{
					setState(571);
					match(NEWLINE);
					}
					}
					setState(576);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(577);
				directoryPhrase();
				}
				break;
			}
			setState(587);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,56,_ctx) ) {
			case 1:
				{
				setState(583);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==NEWLINE) {
					{
					{
					setState(580);
					match(NEWLINE);
					}
					}
					setState(585);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(586);
				familyPhrase();
				}
				break;
			}
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

	@SuppressWarnings("CheckReturnValue")
	public static class DirectoryPhraseContext extends ParserRuleContext {
		public TerminalNode OF() { return getToken(Cobol85PreprocessorParser.OF, 0); }
		public TerminalNode IN() { return getToken(Cobol85PreprocessorParser.IN, 0); }
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public CobolWordContext cobolWord() {
			return getRuleContext(CobolWordContext.class,0);
		}
		public List<TerminalNode> NEWLINE() { return getTokens(Cobol85PreprocessorParser.NEWLINE); }
		public TerminalNode NEWLINE(int i) {
			return getToken(Cobol85PreprocessorParser.NEWLINE, i);
		}
		public DirectoryPhraseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_directoryPhrase; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterDirectoryPhrase(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitDirectoryPhrase(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitDirectoryPhrase(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DirectoryPhraseContext directoryPhrase() throws RecognitionException {
		DirectoryPhraseContext _localctx = new DirectoryPhraseContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_directoryPhrase);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(589);
			_la = _input.LA(1);
			if ( !(_la==IN || _la==OF) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(593);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==NEWLINE) {
				{
				{
				setState(590);
				match(NEWLINE);
				}
				}
				setState(595);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(598);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NONNUMERICLITERAL:
			case NUMERICLITERAL:
				{
				setState(596);
				literal();
				}
				break;
			case ADATA:
			case ADV:
			case ALIAS:
			case ANSI:
			case ANY:
			case APOST:
			case AR:
			case ARITH:
			case AUTO:
			case AWO:
			case BIN:
			case BLOCK0:
			case BUF:
			case BUFSIZE:
			case BY:
			case CBL:
			case CBLCARD:
			case CO:
			case COBOL2:
			case COBOL3:
			case CODEPAGE:
			case COMPAT:
			case COMPILE:
			case CP:
			case CPP:
			case CPSM:
			case CS:
			case CURR:
			case CURRENCY:
			case DATA:
			case DATEPROC:
			case DBCS:
			case DD:
			case DEBUG:
			case DECK:
			case DIAGTRUNC:
			case DLI:
			case DLL:
			case DP:
			case DTR:
			case DU:
			case DUMP:
			case DYN:
			case DYNAM:
			case EDF:
			case EJECT:
			case EJPD:
			case EN:
			case ENGLISH:
			case EPILOG:
			case EXCI:
			case EXIT:
			case EXP:
			case EXPORTALL:
			case EXTEND:
			case FASTSRT:
			case FLAG:
			case FLAGSTD:
			case FSRT:
			case FULL:
			case GDS:
			case GRAPHIC:
			case HOOK:
			case IN:
			case INTDATE:
			case JA:
			case JP:
			case KA:
			case LANG:
			case LANGUAGE:
			case LC:
			case LENGTH:
			case LIB:
			case LILIAN:
			case LIN:
			case LINECOUNT:
			case LINKAGE:
			case LIST:
			case LM:
			case LONGMIXED:
			case LONGUPPER:
			case LU:
			case MAP:
			case MARGINS:
			case MAX:
			case MD:
			case MDECK:
			case MIG:
			case MIXED:
			case NAME:
			case NAT:
			case NATIONAL:
			case NATLANG:
			case NN:
			case NO:
			case NOADATA:
			case NOADV:
			case NOALIAS:
			case NOAWO:
			case NOBLOCK0:
			case NOC:
			case NOCBLCARD:
			case NOCICS:
			case NOCMPR2:
			case NOCOMPILE:
			case NOCPSM:
			case NOCURR:
			case NOCURRENCY:
			case NOD:
			case NODATEPROC:
			case NODBCS:
			case NODE:
			case NODEBUG:
			case NODECK:
			case NODIAGTRUNC:
			case NODLL:
			case NODU:
			case NODUMP:
			case NODP:
			case NODTR:
			case NODYN:
			case NODYNAM:
			case NOEDF:
			case NOEJPD:
			case NOEPILOG:
			case NOEXIT:
			case NOEXP:
			case NOEXPORTALL:
			case NOF:
			case NOFASTSRT:
			case NOFEPI:
			case NOFLAG:
			case NOFLAGMIG:
			case NOFLAGSTD:
			case NOFSRT:
			case NOGRAPHIC:
			case NOHOOK:
			case NOLENGTH:
			case NOLIB:
			case NOLINKAGE:
			case NOLIST:
			case NOMAP:
			case NOMD:
			case NOMDECK:
			case NONAME:
			case NONUM:
			case NONUMBER:
			case NOOBJ:
			case NOOBJECT:
			case NOOFF:
			case NOOFFSET:
			case NOOPSEQUENCE:
			case NOOPT:
			case NOOPTIMIZE:
			case NOOPTIONS:
			case NOP:
			case NOPFD:
			case NOPROLOG:
			case NORENT:
			case NOS:
			case NOSEP:
			case NOSEPARATE:
			case NOSEQ:
			case NOSOURCE:
			case NOSPIE:
			case NOSQL:
			case NOSQLC:
			case NOSQLCCSID:
			case NOSSR:
			case NOSSRANGE:
			case NOSTDTRUNC:
			case NOSEQUENCE:
			case NOTERM:
			case NOTERMINAL:
			case NOTEST:
			case NOTHREAD:
			case NOTRIG:
			case NOVBREF:
			case NOWORD:
			case NOX:
			case NOXREF:
			case NOZWB:
			case NS:
			case NSEQ:
			case NSYMBOL:
			case NUM:
			case NUMBER:
			case NUMPROC:
			case OBJ:
			case OBJECT:
			case OF:
			case OFF:
			case OFFSET:
			case ON:
			case OP:
			case OPMARGINS:
			case OPSEQUENCE:
			case OPT:
			case OPTFILE:
			case OPTIMIZE:
			case OPTIONS:
			case OUT:
			case OUTDD:
			case PFD:
			case PPTDBG:
			case PGMN:
			case PGMNAME:
			case PROCESS:
			case PROLOG:
			case QUOTE:
			case RENT:
			case REPLACING:
			case RMODE:
			case SEP:
			case SEPARATE:
			case SEQ:
			case SEQUENCE:
			case SHORT:
			case SIZE:
			case SOURCE:
			case SP:
			case SPACE:
			case SPIE:
			case SQL:
			case SQLC:
			case SQLCCSID:
			case SS:
			case SSR:
			case SSRANGE:
			case STD:
			case SYSEIB:
			case SZ:
			case TERM:
			case TERMINAL:
			case TEST:
			case THREAD:
			case TITLE:
			case TRIG:
			case TRUNC:
			case UE:
			case UPPER:
			case VBREF:
			case WD:
			case XMLPARSE:
			case XMLSS:
			case XOPTS:
			case XREF:
			case YEARWINDOW:
			case YW:
			case ZWB:
			case C_CHAR:
			case D_CHAR:
			case E_CHAR:
			case F_CHAR:
			case H_CHAR:
			case I_CHAR:
			case M_CHAR:
			case N_CHAR:
			case Q_CHAR:
			case S_CHAR:
			case U_CHAR:
			case W_CHAR:
			case X_CHAR:
			case COMMACHAR:
			case IDENTIFIER:
				{
				setState(597);
				cobolWord();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
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

	@SuppressWarnings("CheckReturnValue")
	public static class FamilyPhraseContext extends ParserRuleContext {
		public TerminalNode ON() { return getToken(Cobol85PreprocessorParser.ON, 0); }
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public CobolWordContext cobolWord() {
			return getRuleContext(CobolWordContext.class,0);
		}
		public List<TerminalNode> NEWLINE() { return getTokens(Cobol85PreprocessorParser.NEWLINE); }
		public TerminalNode NEWLINE(int i) {
			return getToken(Cobol85PreprocessorParser.NEWLINE, i);
		}
		public FamilyPhraseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_familyPhrase; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterFamilyPhrase(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitFamilyPhrase(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitFamilyPhrase(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FamilyPhraseContext familyPhrase() throws RecognitionException {
		FamilyPhraseContext _localctx = new FamilyPhraseContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_familyPhrase);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(600);
			match(ON);
			setState(604);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==NEWLINE) {
				{
				{
				setState(601);
				match(NEWLINE);
				}
				}
				setState(606);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(609);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NONNUMERICLITERAL:
			case NUMERICLITERAL:
				{
				setState(607);
				literal();
				}
				break;
			case ADATA:
			case ADV:
			case ALIAS:
			case ANSI:
			case ANY:
			case APOST:
			case AR:
			case ARITH:
			case AUTO:
			case AWO:
			case BIN:
			case BLOCK0:
			case BUF:
			case BUFSIZE:
			case BY:
			case CBL:
			case CBLCARD:
			case CO:
			case COBOL2:
			case COBOL3:
			case CODEPAGE:
			case COMPAT:
			case COMPILE:
			case CP:
			case CPP:
			case CPSM:
			case CS:
			case CURR:
			case CURRENCY:
			case DATA:
			case DATEPROC:
			case DBCS:
			case DD:
			case DEBUG:
			case DECK:
			case DIAGTRUNC:
			case DLI:
			case DLL:
			case DP:
			case DTR:
			case DU:
			case DUMP:
			case DYN:
			case DYNAM:
			case EDF:
			case EJECT:
			case EJPD:
			case EN:
			case ENGLISH:
			case EPILOG:
			case EXCI:
			case EXIT:
			case EXP:
			case EXPORTALL:
			case EXTEND:
			case FASTSRT:
			case FLAG:
			case FLAGSTD:
			case FSRT:
			case FULL:
			case GDS:
			case GRAPHIC:
			case HOOK:
			case IN:
			case INTDATE:
			case JA:
			case JP:
			case KA:
			case LANG:
			case LANGUAGE:
			case LC:
			case LENGTH:
			case LIB:
			case LILIAN:
			case LIN:
			case LINECOUNT:
			case LINKAGE:
			case LIST:
			case LM:
			case LONGMIXED:
			case LONGUPPER:
			case LU:
			case MAP:
			case MARGINS:
			case MAX:
			case MD:
			case MDECK:
			case MIG:
			case MIXED:
			case NAME:
			case NAT:
			case NATIONAL:
			case NATLANG:
			case NN:
			case NO:
			case NOADATA:
			case NOADV:
			case NOALIAS:
			case NOAWO:
			case NOBLOCK0:
			case NOC:
			case NOCBLCARD:
			case NOCICS:
			case NOCMPR2:
			case NOCOMPILE:
			case NOCPSM:
			case NOCURR:
			case NOCURRENCY:
			case NOD:
			case NODATEPROC:
			case NODBCS:
			case NODE:
			case NODEBUG:
			case NODECK:
			case NODIAGTRUNC:
			case NODLL:
			case NODU:
			case NODUMP:
			case NODP:
			case NODTR:
			case NODYN:
			case NODYNAM:
			case NOEDF:
			case NOEJPD:
			case NOEPILOG:
			case NOEXIT:
			case NOEXP:
			case NOEXPORTALL:
			case NOF:
			case NOFASTSRT:
			case NOFEPI:
			case NOFLAG:
			case NOFLAGMIG:
			case NOFLAGSTD:
			case NOFSRT:
			case NOGRAPHIC:
			case NOHOOK:
			case NOLENGTH:
			case NOLIB:
			case NOLINKAGE:
			case NOLIST:
			case NOMAP:
			case NOMD:
			case NOMDECK:
			case NONAME:
			case NONUM:
			case NONUMBER:
			case NOOBJ:
			case NOOBJECT:
			case NOOFF:
			case NOOFFSET:
			case NOOPSEQUENCE:
			case NOOPT:
			case NOOPTIMIZE:
			case NOOPTIONS:
			case NOP:
			case NOPFD:
			case NOPROLOG:
			case NORENT:
			case NOS:
			case NOSEP:
			case NOSEPARATE:
			case NOSEQ:
			case NOSOURCE:
			case NOSPIE:
			case NOSQL:
			case NOSQLC:
			case NOSQLCCSID:
			case NOSSR:
			case NOSSRANGE:
			case NOSTDTRUNC:
			case NOSEQUENCE:
			case NOTERM:
			case NOTERMINAL:
			case NOTEST:
			case NOTHREAD:
			case NOTRIG:
			case NOVBREF:
			case NOWORD:
			case NOX:
			case NOXREF:
			case NOZWB:
			case NS:
			case NSEQ:
			case NSYMBOL:
			case NUM:
			case NUMBER:
			case NUMPROC:
			case OBJ:
			case OBJECT:
			case OF:
			case OFF:
			case OFFSET:
			case ON:
			case OP:
			case OPMARGINS:
			case OPSEQUENCE:
			case OPT:
			case OPTFILE:
			case OPTIMIZE:
			case OPTIONS:
			case OUT:
			case OUTDD:
			case PFD:
			case PPTDBG:
			case PGMN:
			case PGMNAME:
			case PROCESS:
			case PROLOG:
			case QUOTE:
			case RENT:
			case REPLACING:
			case RMODE:
			case SEP:
			case SEPARATE:
			case SEQ:
			case SEQUENCE:
			case SHORT:
			case SIZE:
			case SOURCE:
			case SP:
			case SPACE:
			case SPIE:
			case SQL:
			case SQLC:
			case SQLCCSID:
			case SS:
			case SSR:
			case SSRANGE:
			case STD:
			case SYSEIB:
			case SZ:
			case TERM:
			case TERMINAL:
			case TEST:
			case THREAD:
			case TITLE:
			case TRIG:
			case TRUNC:
			case UE:
			case UPPER:
			case VBREF:
			case WD:
			case XMLPARSE:
			case XMLSS:
			case XOPTS:
			case XREF:
			case YEARWINDOW:
			case YW:
			case ZWB:
			case C_CHAR:
			case D_CHAR:
			case E_CHAR:
			case F_CHAR:
			case H_CHAR:
			case I_CHAR:
			case M_CHAR:
			case N_CHAR:
			case Q_CHAR:
			case S_CHAR:
			case U_CHAR:
			case W_CHAR:
			case X_CHAR:
			case COMMACHAR:
			case IDENTIFIER:
				{
				setState(608);
				cobolWord();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
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

	@SuppressWarnings("CheckReturnValue")
	public static class ReplaceableContext extends ParserRuleContext {
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public CobolWordContext cobolWord() {
			return getRuleContext(CobolWordContext.class,0);
		}
		public PseudoTextContext pseudoText() {
			return getRuleContext(PseudoTextContext.class,0);
		}
		public CharDataLineContext charDataLine() {
			return getRuleContext(CharDataLineContext.class,0);
		}
		public ReplaceableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_replaceable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterReplaceable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitReplaceable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitReplaceable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ReplaceableContext replaceable() throws RecognitionException {
		ReplaceableContext _localctx = new ReplaceableContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_replaceable);
		try {
			setState(615);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,61,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(611);
				literal();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(612);
				cobolWord();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(613);
				pseudoText();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(614);
				charDataLine();
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

	@SuppressWarnings("CheckReturnValue")
	public static class ReplacementContext extends ParserRuleContext {
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public CobolWordContext cobolWord() {
			return getRuleContext(CobolWordContext.class,0);
		}
		public PseudoTextContext pseudoText() {
			return getRuleContext(PseudoTextContext.class,0);
		}
		public CharDataLineContext charDataLine() {
			return getRuleContext(CharDataLineContext.class,0);
		}
		public ReplacementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_replacement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterReplacement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitReplacement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitReplacement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ReplacementContext replacement() throws RecognitionException {
		ReplacementContext _localctx = new ReplacementContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_replacement);
		try {
			setState(621);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,62,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(617);
				literal();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(618);
				cobolWord();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(619);
				pseudoText();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(620);
				charDataLine();
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

	@SuppressWarnings("CheckReturnValue")
	public static class EjectStatementContext extends ParserRuleContext {
		public TerminalNode EJECT() { return getToken(Cobol85PreprocessorParser.EJECT, 0); }
		public TerminalNode DOT() { return getToken(Cobol85PreprocessorParser.DOT, 0); }
		public EjectStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ejectStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterEjectStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitEjectStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitEjectStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EjectStatementContext ejectStatement() throws RecognitionException {
		EjectStatementContext _localctx = new EjectStatementContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_ejectStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(623);
			match(EJECT);
			setState(625);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,63,_ctx) ) {
			case 1:
				{
				setState(624);
				match(DOT);
				}
				break;
			}
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

	@SuppressWarnings("CheckReturnValue")
	public static class SkipStatementContext extends ParserRuleContext {
		public TerminalNode SKIP1() { return getToken(Cobol85PreprocessorParser.SKIP1, 0); }
		public TerminalNode SKIP2() { return getToken(Cobol85PreprocessorParser.SKIP2, 0); }
		public TerminalNode SKIP3() { return getToken(Cobol85PreprocessorParser.SKIP3, 0); }
		public TerminalNode DOT() { return getToken(Cobol85PreprocessorParser.DOT, 0); }
		public SkipStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_skipStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterSkipStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitSkipStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitSkipStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SkipStatementContext skipStatement() throws RecognitionException {
		SkipStatementContext _localctx = new SkipStatementContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_skipStatement);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(627);
			_la = _input.LA(1);
			if ( !(((((_la - 238)) & ~0x3f) == 0 && ((1L << (_la - 238)) & 7L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(629);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,64,_ctx) ) {
			case 1:
				{
				setState(628);
				match(DOT);
				}
				break;
			}
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

	@SuppressWarnings("CheckReturnValue")
	public static class TitleStatementContext extends ParserRuleContext {
		public TerminalNode TITLE() { return getToken(Cobol85PreprocessorParser.TITLE, 0); }
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public TerminalNode DOT() { return getToken(Cobol85PreprocessorParser.DOT, 0); }
		public TitleStatementContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_titleStatement; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterTitleStatement(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitTitleStatement(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitTitleStatement(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TitleStatementContext titleStatement() throws RecognitionException {
		TitleStatementContext _localctx = new TitleStatementContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_titleStatement);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(631);
			match(TITLE);
			setState(632);
			literal();
			setState(634);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,65,_ctx) ) {
			case 1:
				{
				setState(633);
				match(DOT);
				}
				break;
			}
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

	@SuppressWarnings("CheckReturnValue")
	public static class PseudoTextContext extends ParserRuleContext {
		public List<TerminalNode> DOUBLEEQUALCHAR() { return getTokens(Cobol85PreprocessorParser.DOUBLEEQUALCHAR); }
		public TerminalNode DOUBLEEQUALCHAR(int i) {
			return getToken(Cobol85PreprocessorParser.DOUBLEEQUALCHAR, i);
		}
		public CharDataContext charData() {
			return getRuleContext(CharDataContext.class,0);
		}
		public PseudoTextContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pseudoText; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterPseudoText(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitPseudoText(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitPseudoText(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PseudoTextContext pseudoText() throws RecognitionException {
		PseudoTextContext _localctx = new PseudoTextContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_pseudoText);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(636);
			match(DOUBLEEQUALCHAR);
			setState(638);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & -2346375405893844994L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -8193L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -288230376151711745L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -9534965104508929L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 85597355895L) != 0)) {
				{
				setState(637);
				charData();
				}
			}

			setState(640);
			match(DOUBLEEQUALCHAR);
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

	@SuppressWarnings("CheckReturnValue")
	public static class CharDataContext extends ParserRuleContext {
		public List<CharDataLineContext> charDataLine() {
			return getRuleContexts(CharDataLineContext.class);
		}
		public CharDataLineContext charDataLine(int i) {
			return getRuleContext(CharDataLineContext.class,i);
		}
		public List<TerminalNode> NEWLINE() { return getTokens(Cobol85PreprocessorParser.NEWLINE); }
		public TerminalNode NEWLINE(int i) {
			return getToken(Cobol85PreprocessorParser.NEWLINE, i);
		}
		public CharDataContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_charData; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterCharData(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitCharData(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitCharData(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CharDataContext charData() throws RecognitionException {
		CharDataContext _localctx = new CharDataContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_charData);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(644); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					setState(644);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case ADATA:
					case ADV:
					case ALIAS:
					case ANSI:
					case ANY:
					case APOST:
					case AR:
					case ARITH:
					case AUTO:
					case AWO:
					case BIN:
					case BLOCK0:
					case BUF:
					case BUFSIZE:
					case BY:
					case CBL:
					case CBLCARD:
					case CO:
					case COBOL2:
					case COBOL3:
					case CODEPAGE:
					case COMPAT:
					case COMPILE:
					case CP:
					case CPP:
					case CPSM:
					case CS:
					case CURR:
					case CURRENCY:
					case DATA:
					case DATEPROC:
					case DBCS:
					case DD:
					case DEBUG:
					case DECK:
					case DIAGTRUNC:
					case DLI:
					case DLL:
					case DP:
					case DTR:
					case DU:
					case DUMP:
					case DYN:
					case DYNAM:
					case EDF:
					case EJECT:
					case EJPD:
					case EN:
					case ENGLISH:
					case EPILOG:
					case EXCI:
					case EXIT:
					case EXP:
					case EXPORTALL:
					case EXTEND:
					case FASTSRT:
					case FLAG:
					case FLAGSTD:
					case FSRT:
					case FULL:
					case GDS:
					case GRAPHIC:
					case HOOK:
					case IN:
					case INTDATE:
					case JA:
					case JP:
					case KA:
					case LANG:
					case LANGUAGE:
					case LC:
					case LENGTH:
					case LIB:
					case LILIAN:
					case LIN:
					case LINECOUNT:
					case LINKAGE:
					case LIST:
					case LM:
					case LONGMIXED:
					case LONGUPPER:
					case LPARENCHAR:
					case LU:
					case MAP:
					case MARGINS:
					case MAX:
					case MD:
					case MDECK:
					case MIG:
					case MIXED:
					case NAME:
					case NAT:
					case NATIONAL:
					case NATLANG:
					case NN:
					case NO:
					case NOADATA:
					case NOADV:
					case NOALIAS:
					case NOAWO:
					case NOBLOCK0:
					case NOC:
					case NOCBLCARD:
					case NOCICS:
					case NOCMPR2:
					case NOCOMPILE:
					case NOCPSM:
					case NOCURR:
					case NOCURRENCY:
					case NOD:
					case NODATEPROC:
					case NODBCS:
					case NODE:
					case NODEBUG:
					case NODECK:
					case NODIAGTRUNC:
					case NODLL:
					case NODU:
					case NODUMP:
					case NODP:
					case NODTR:
					case NODYN:
					case NODYNAM:
					case NOEDF:
					case NOEJPD:
					case NOEPILOG:
					case NOEXIT:
					case NOEXP:
					case NOEXPORTALL:
					case NOF:
					case NOFASTSRT:
					case NOFEPI:
					case NOFLAG:
					case NOFLAGMIG:
					case NOFLAGSTD:
					case NOFSRT:
					case NOGRAPHIC:
					case NOHOOK:
					case NOLENGTH:
					case NOLIB:
					case NOLINKAGE:
					case NOLIST:
					case NOMAP:
					case NOMD:
					case NOMDECK:
					case NONAME:
					case NONUM:
					case NONUMBER:
					case NOOBJ:
					case NOOBJECT:
					case NOOFF:
					case NOOFFSET:
					case NOOPSEQUENCE:
					case NOOPT:
					case NOOPTIMIZE:
					case NOOPTIONS:
					case NOP:
					case NOPFD:
					case NOPROLOG:
					case NORENT:
					case NOS:
					case NOSEP:
					case NOSEPARATE:
					case NOSEQ:
					case NOSOURCE:
					case NOSPIE:
					case NOSQL:
					case NOSQLC:
					case NOSQLCCSID:
					case NOSSR:
					case NOSSRANGE:
					case NOSTDTRUNC:
					case NOSEQUENCE:
					case NOTERM:
					case NOTERMINAL:
					case NOTEST:
					case NOTHREAD:
					case NOTRIG:
					case NOVBREF:
					case NOWORD:
					case NOX:
					case NOXREF:
					case NOZWB:
					case NS:
					case NSEQ:
					case NSYMBOL:
					case NUM:
					case NUMBER:
					case NUMPROC:
					case OBJ:
					case OBJECT:
					case OF:
					case OFF:
					case OFFSET:
					case ON:
					case OP:
					case OPMARGINS:
					case OPSEQUENCE:
					case OPT:
					case OPTFILE:
					case OPTIMIZE:
					case OPTIONS:
					case OUT:
					case OUTDD:
					case PFD:
					case PPTDBG:
					case PGMN:
					case PGMNAME:
					case PROCESS:
					case PROLOG:
					case QUOTE:
					case RENT:
					case REPLACING:
					case RMODE:
					case RPARENCHAR:
					case SEP:
					case SEPARATE:
					case SEQ:
					case SEQUENCE:
					case SHORT:
					case SIZE:
					case SOURCE:
					case SP:
					case SPACE:
					case SPIE:
					case SQL:
					case SQLC:
					case SQLCCSID:
					case SS:
					case SSR:
					case SSRANGE:
					case STD:
					case SYSEIB:
					case SZ:
					case TERM:
					case TERMINAL:
					case TEST:
					case THREAD:
					case TITLE:
					case TRIG:
					case TRUNC:
					case UE:
					case UPPER:
					case VBREF:
					case WD:
					case XMLPARSE:
					case XMLSS:
					case XOPTS:
					case XREF:
					case YEARWINDOW:
					case YW:
					case ZWB:
					case C_CHAR:
					case D_CHAR:
					case E_CHAR:
					case F_CHAR:
					case H_CHAR:
					case I_CHAR:
					case M_CHAR:
					case N_CHAR:
					case Q_CHAR:
					case S_CHAR:
					case U_CHAR:
					case W_CHAR:
					case X_CHAR:
					case COMMACHAR:
					case DOT:
					case NONNUMERICLITERAL:
					case NUMERICLITERAL:
					case IDENTIFIER:
					case FILENAME:
					case TEXT:
						{
						setState(642);
						charDataLine();
						}
						break;
					case NEWLINE:
						{
						setState(643);
						match(NEWLINE);
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(646); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,68,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
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

	@SuppressWarnings("CheckReturnValue")
	public static class CharDataSqlContext extends ParserRuleContext {
		public List<CharDataLineContext> charDataLine() {
			return getRuleContexts(CharDataLineContext.class);
		}
		public CharDataLineContext charDataLine(int i) {
			return getRuleContext(CharDataLineContext.class,i);
		}
		public List<TerminalNode> COPY() { return getTokens(Cobol85PreprocessorParser.COPY); }
		public TerminalNode COPY(int i) {
			return getToken(Cobol85PreprocessorParser.COPY, i);
		}
		public List<TerminalNode> REPLACE() { return getTokens(Cobol85PreprocessorParser.REPLACE); }
		public TerminalNode REPLACE(int i) {
			return getToken(Cobol85PreprocessorParser.REPLACE, i);
		}
		public List<TerminalNode> NEWLINE() { return getTokens(Cobol85PreprocessorParser.NEWLINE); }
		public TerminalNode NEWLINE(int i) {
			return getToken(Cobol85PreprocessorParser.NEWLINE, i);
		}
		public CharDataSqlContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_charDataSql; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterCharDataSql(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitCharDataSql(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitCharDataSql(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CharDataSqlContext charDataSql() throws RecognitionException {
		CharDataSqlContext _localctx = new CharDataSqlContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_charDataSql);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(652); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				setState(652);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case ADATA:
				case ADV:
				case ALIAS:
				case ANSI:
				case ANY:
				case APOST:
				case AR:
				case ARITH:
				case AUTO:
				case AWO:
				case BIN:
				case BLOCK0:
				case BUF:
				case BUFSIZE:
				case BY:
				case CBL:
				case CBLCARD:
				case CO:
				case COBOL2:
				case COBOL3:
				case CODEPAGE:
				case COMPAT:
				case COMPILE:
				case CP:
				case CPP:
				case CPSM:
				case CS:
				case CURR:
				case CURRENCY:
				case DATA:
				case DATEPROC:
				case DBCS:
				case DD:
				case DEBUG:
				case DECK:
				case DIAGTRUNC:
				case DLI:
				case DLL:
				case DP:
				case DTR:
				case DU:
				case DUMP:
				case DYN:
				case DYNAM:
				case EDF:
				case EJECT:
				case EJPD:
				case EN:
				case ENGLISH:
				case EPILOG:
				case EXCI:
				case EXIT:
				case EXP:
				case EXPORTALL:
				case EXTEND:
				case FASTSRT:
				case FLAG:
				case FLAGSTD:
				case FSRT:
				case FULL:
				case GDS:
				case GRAPHIC:
				case HOOK:
				case IN:
				case INTDATE:
				case JA:
				case JP:
				case KA:
				case LANG:
				case LANGUAGE:
				case LC:
				case LENGTH:
				case LIB:
				case LILIAN:
				case LIN:
				case LINECOUNT:
				case LINKAGE:
				case LIST:
				case LM:
				case LONGMIXED:
				case LONGUPPER:
				case LPARENCHAR:
				case LU:
				case MAP:
				case MARGINS:
				case MAX:
				case MD:
				case MDECK:
				case MIG:
				case MIXED:
				case NAME:
				case NAT:
				case NATIONAL:
				case NATLANG:
				case NN:
				case NO:
				case NOADATA:
				case NOADV:
				case NOALIAS:
				case NOAWO:
				case NOBLOCK0:
				case NOC:
				case NOCBLCARD:
				case NOCICS:
				case NOCMPR2:
				case NOCOMPILE:
				case NOCPSM:
				case NOCURR:
				case NOCURRENCY:
				case NOD:
				case NODATEPROC:
				case NODBCS:
				case NODE:
				case NODEBUG:
				case NODECK:
				case NODIAGTRUNC:
				case NODLL:
				case NODU:
				case NODUMP:
				case NODP:
				case NODTR:
				case NODYN:
				case NODYNAM:
				case NOEDF:
				case NOEJPD:
				case NOEPILOG:
				case NOEXIT:
				case NOEXP:
				case NOEXPORTALL:
				case NOF:
				case NOFASTSRT:
				case NOFEPI:
				case NOFLAG:
				case NOFLAGMIG:
				case NOFLAGSTD:
				case NOFSRT:
				case NOGRAPHIC:
				case NOHOOK:
				case NOLENGTH:
				case NOLIB:
				case NOLINKAGE:
				case NOLIST:
				case NOMAP:
				case NOMD:
				case NOMDECK:
				case NONAME:
				case NONUM:
				case NONUMBER:
				case NOOBJ:
				case NOOBJECT:
				case NOOFF:
				case NOOFFSET:
				case NOOPSEQUENCE:
				case NOOPT:
				case NOOPTIMIZE:
				case NOOPTIONS:
				case NOP:
				case NOPFD:
				case NOPROLOG:
				case NORENT:
				case NOS:
				case NOSEP:
				case NOSEPARATE:
				case NOSEQ:
				case NOSOURCE:
				case NOSPIE:
				case NOSQL:
				case NOSQLC:
				case NOSQLCCSID:
				case NOSSR:
				case NOSSRANGE:
				case NOSTDTRUNC:
				case NOSEQUENCE:
				case NOTERM:
				case NOTERMINAL:
				case NOTEST:
				case NOTHREAD:
				case NOTRIG:
				case NOVBREF:
				case NOWORD:
				case NOX:
				case NOXREF:
				case NOZWB:
				case NS:
				case NSEQ:
				case NSYMBOL:
				case NUM:
				case NUMBER:
				case NUMPROC:
				case OBJ:
				case OBJECT:
				case OF:
				case OFF:
				case OFFSET:
				case ON:
				case OP:
				case OPMARGINS:
				case OPSEQUENCE:
				case OPT:
				case OPTFILE:
				case OPTIMIZE:
				case OPTIONS:
				case OUT:
				case OUTDD:
				case PFD:
				case PPTDBG:
				case PGMN:
				case PGMNAME:
				case PROCESS:
				case PROLOG:
				case QUOTE:
				case RENT:
				case REPLACING:
				case RMODE:
				case RPARENCHAR:
				case SEP:
				case SEPARATE:
				case SEQ:
				case SEQUENCE:
				case SHORT:
				case SIZE:
				case SOURCE:
				case SP:
				case SPACE:
				case SPIE:
				case SQL:
				case SQLC:
				case SQLCCSID:
				case SS:
				case SSR:
				case SSRANGE:
				case STD:
				case SYSEIB:
				case SZ:
				case TERM:
				case TERMINAL:
				case TEST:
				case THREAD:
				case TITLE:
				case TRIG:
				case TRUNC:
				case UE:
				case UPPER:
				case VBREF:
				case WD:
				case XMLPARSE:
				case XMLSS:
				case XOPTS:
				case XREF:
				case YEARWINDOW:
				case YW:
				case ZWB:
				case C_CHAR:
				case D_CHAR:
				case E_CHAR:
				case F_CHAR:
				case H_CHAR:
				case I_CHAR:
				case M_CHAR:
				case N_CHAR:
				case Q_CHAR:
				case S_CHAR:
				case U_CHAR:
				case W_CHAR:
				case X_CHAR:
				case COMMACHAR:
				case DOT:
				case NONNUMERICLITERAL:
				case NUMERICLITERAL:
				case IDENTIFIER:
				case FILENAME:
				case TEXT:
					{
					setState(648);
					charDataLine();
					}
					break;
				case COPY:
					{
					setState(649);
					match(COPY);
					}
					break;
				case REPLACE:
					{
					setState(650);
					match(REPLACE);
					}
					break;
				case NEWLINE:
					{
					setState(651);
					match(NEWLINE);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(654); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & -2346375405860290562L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -8193L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -288230376151711745L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -9534964836073473L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 85597355895L) != 0) );
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

	@SuppressWarnings("CheckReturnValue")
	public static class CharDataLineContext extends ParserRuleContext {
		public List<CobolWordContext> cobolWord() {
			return getRuleContexts(CobolWordContext.class);
		}
		public CobolWordContext cobolWord(int i) {
			return getRuleContext(CobolWordContext.class,i);
		}
		public List<LiteralContext> literal() {
			return getRuleContexts(LiteralContext.class);
		}
		public LiteralContext literal(int i) {
			return getRuleContext(LiteralContext.class,i);
		}
		public List<FilenameContext> filename() {
			return getRuleContexts(FilenameContext.class);
		}
		public FilenameContext filename(int i) {
			return getRuleContext(FilenameContext.class,i);
		}
		public List<TerminalNode> TEXT() { return getTokens(Cobol85PreprocessorParser.TEXT); }
		public TerminalNode TEXT(int i) {
			return getToken(Cobol85PreprocessorParser.TEXT, i);
		}
		public List<TerminalNode> DOT() { return getTokens(Cobol85PreprocessorParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(Cobol85PreprocessorParser.DOT, i);
		}
		public List<TerminalNode> LPARENCHAR() { return getTokens(Cobol85PreprocessorParser.LPARENCHAR); }
		public TerminalNode LPARENCHAR(int i) {
			return getToken(Cobol85PreprocessorParser.LPARENCHAR, i);
		}
		public List<TerminalNode> RPARENCHAR() { return getTokens(Cobol85PreprocessorParser.RPARENCHAR); }
		public TerminalNode RPARENCHAR(int i) {
			return getToken(Cobol85PreprocessorParser.RPARENCHAR, i);
		}
		public CharDataLineContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_charDataLine; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterCharDataLine(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitCharDataLine(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitCharDataLine(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CharDataLineContext charDataLine() throws RecognitionException {
		CharDataLineContext _localctx = new CharDataLineContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_charDataLine);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(663); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					setState(663);
					_errHandler.sync(this);
					switch (_input.LA(1)) {
					case ADATA:
					case ADV:
					case ALIAS:
					case ANSI:
					case ANY:
					case APOST:
					case AR:
					case ARITH:
					case AUTO:
					case AWO:
					case BIN:
					case BLOCK0:
					case BUF:
					case BUFSIZE:
					case BY:
					case CBL:
					case CBLCARD:
					case CO:
					case COBOL2:
					case COBOL3:
					case CODEPAGE:
					case COMPAT:
					case COMPILE:
					case CP:
					case CPP:
					case CPSM:
					case CS:
					case CURR:
					case CURRENCY:
					case DATA:
					case DATEPROC:
					case DBCS:
					case DD:
					case DEBUG:
					case DECK:
					case DIAGTRUNC:
					case DLI:
					case DLL:
					case DP:
					case DTR:
					case DU:
					case DUMP:
					case DYN:
					case DYNAM:
					case EDF:
					case EJECT:
					case EJPD:
					case EN:
					case ENGLISH:
					case EPILOG:
					case EXCI:
					case EXIT:
					case EXP:
					case EXPORTALL:
					case EXTEND:
					case FASTSRT:
					case FLAG:
					case FLAGSTD:
					case FSRT:
					case FULL:
					case GDS:
					case GRAPHIC:
					case HOOK:
					case IN:
					case INTDATE:
					case JA:
					case JP:
					case KA:
					case LANG:
					case LANGUAGE:
					case LC:
					case LENGTH:
					case LIB:
					case LILIAN:
					case LIN:
					case LINECOUNT:
					case LINKAGE:
					case LIST:
					case LM:
					case LONGMIXED:
					case LONGUPPER:
					case LU:
					case MAP:
					case MARGINS:
					case MAX:
					case MD:
					case MDECK:
					case MIG:
					case MIXED:
					case NAME:
					case NAT:
					case NATIONAL:
					case NATLANG:
					case NN:
					case NO:
					case NOADATA:
					case NOADV:
					case NOALIAS:
					case NOAWO:
					case NOBLOCK0:
					case NOC:
					case NOCBLCARD:
					case NOCICS:
					case NOCMPR2:
					case NOCOMPILE:
					case NOCPSM:
					case NOCURR:
					case NOCURRENCY:
					case NOD:
					case NODATEPROC:
					case NODBCS:
					case NODE:
					case NODEBUG:
					case NODECK:
					case NODIAGTRUNC:
					case NODLL:
					case NODU:
					case NODUMP:
					case NODP:
					case NODTR:
					case NODYN:
					case NODYNAM:
					case NOEDF:
					case NOEJPD:
					case NOEPILOG:
					case NOEXIT:
					case NOEXP:
					case NOEXPORTALL:
					case NOF:
					case NOFASTSRT:
					case NOFEPI:
					case NOFLAG:
					case NOFLAGMIG:
					case NOFLAGSTD:
					case NOFSRT:
					case NOGRAPHIC:
					case NOHOOK:
					case NOLENGTH:
					case NOLIB:
					case NOLINKAGE:
					case NOLIST:
					case NOMAP:
					case NOMD:
					case NOMDECK:
					case NONAME:
					case NONUM:
					case NONUMBER:
					case NOOBJ:
					case NOOBJECT:
					case NOOFF:
					case NOOFFSET:
					case NOOPSEQUENCE:
					case NOOPT:
					case NOOPTIMIZE:
					case NOOPTIONS:
					case NOP:
					case NOPFD:
					case NOPROLOG:
					case NORENT:
					case NOS:
					case NOSEP:
					case NOSEPARATE:
					case NOSEQ:
					case NOSOURCE:
					case NOSPIE:
					case NOSQL:
					case NOSQLC:
					case NOSQLCCSID:
					case NOSSR:
					case NOSSRANGE:
					case NOSTDTRUNC:
					case NOSEQUENCE:
					case NOTERM:
					case NOTERMINAL:
					case NOTEST:
					case NOTHREAD:
					case NOTRIG:
					case NOVBREF:
					case NOWORD:
					case NOX:
					case NOXREF:
					case NOZWB:
					case NS:
					case NSEQ:
					case NSYMBOL:
					case NUM:
					case NUMBER:
					case NUMPROC:
					case OBJ:
					case OBJECT:
					case OF:
					case OFF:
					case OFFSET:
					case ON:
					case OP:
					case OPMARGINS:
					case OPSEQUENCE:
					case OPT:
					case OPTFILE:
					case OPTIMIZE:
					case OPTIONS:
					case OUT:
					case OUTDD:
					case PFD:
					case PPTDBG:
					case PGMN:
					case PGMNAME:
					case PROCESS:
					case PROLOG:
					case QUOTE:
					case RENT:
					case REPLACING:
					case RMODE:
					case SEP:
					case SEPARATE:
					case SEQ:
					case SEQUENCE:
					case SHORT:
					case SIZE:
					case SOURCE:
					case SP:
					case SPACE:
					case SPIE:
					case SQL:
					case SQLC:
					case SQLCCSID:
					case SS:
					case SSR:
					case SSRANGE:
					case STD:
					case SYSEIB:
					case SZ:
					case TERM:
					case TERMINAL:
					case TEST:
					case THREAD:
					case TITLE:
					case TRIG:
					case TRUNC:
					case UE:
					case UPPER:
					case VBREF:
					case WD:
					case XMLPARSE:
					case XMLSS:
					case XOPTS:
					case XREF:
					case YEARWINDOW:
					case YW:
					case ZWB:
					case C_CHAR:
					case D_CHAR:
					case E_CHAR:
					case F_CHAR:
					case H_CHAR:
					case I_CHAR:
					case M_CHAR:
					case N_CHAR:
					case Q_CHAR:
					case S_CHAR:
					case U_CHAR:
					case W_CHAR:
					case X_CHAR:
					case COMMACHAR:
					case IDENTIFIER:
						{
						setState(656);
						cobolWord();
						}
						break;
					case NONNUMERICLITERAL:
					case NUMERICLITERAL:
						{
						setState(657);
						literal();
						}
						break;
					case FILENAME:
						{
						setState(658);
						filename();
						}
						break;
					case TEXT:
						{
						setState(659);
						match(TEXT);
						}
						break;
					case DOT:
						{
						setState(660);
						match(DOT);
						}
						break;
					case LPARENCHAR:
						{
						setState(661);
						match(LPARENCHAR);
						}
						break;
					case RPARENCHAR:
						{
						setState(662);
						match(RPARENCHAR);
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(665); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,72,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
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

	@SuppressWarnings("CheckReturnValue")
	public static class CobolWordContext extends ParserRuleContext {
		public TerminalNode IDENTIFIER() { return getToken(Cobol85PreprocessorParser.IDENTIFIER, 0); }
		public CharDataKeywordContext charDataKeyword() {
			return getRuleContext(CharDataKeywordContext.class,0);
		}
		public CobolWordContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_cobolWord; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterCobolWord(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitCobolWord(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitCobolWord(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CobolWordContext cobolWord() throws RecognitionException {
		CobolWordContext _localctx = new CobolWordContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_cobolWord);
		try {
			setState(669);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case IDENTIFIER:
				enterOuterAlt(_localctx, 1);
				{
				setState(667);
				match(IDENTIFIER);
				}
				break;
			case ADATA:
			case ADV:
			case ALIAS:
			case ANSI:
			case ANY:
			case APOST:
			case AR:
			case ARITH:
			case AUTO:
			case AWO:
			case BIN:
			case BLOCK0:
			case BUF:
			case BUFSIZE:
			case BY:
			case CBL:
			case CBLCARD:
			case CO:
			case COBOL2:
			case COBOL3:
			case CODEPAGE:
			case COMPAT:
			case COMPILE:
			case CP:
			case CPP:
			case CPSM:
			case CS:
			case CURR:
			case CURRENCY:
			case DATA:
			case DATEPROC:
			case DBCS:
			case DD:
			case DEBUG:
			case DECK:
			case DIAGTRUNC:
			case DLI:
			case DLL:
			case DP:
			case DTR:
			case DU:
			case DUMP:
			case DYN:
			case DYNAM:
			case EDF:
			case EJECT:
			case EJPD:
			case EN:
			case ENGLISH:
			case EPILOG:
			case EXCI:
			case EXIT:
			case EXP:
			case EXPORTALL:
			case EXTEND:
			case FASTSRT:
			case FLAG:
			case FLAGSTD:
			case FSRT:
			case FULL:
			case GDS:
			case GRAPHIC:
			case HOOK:
			case IN:
			case INTDATE:
			case JA:
			case JP:
			case KA:
			case LANG:
			case LANGUAGE:
			case LC:
			case LENGTH:
			case LIB:
			case LILIAN:
			case LIN:
			case LINECOUNT:
			case LINKAGE:
			case LIST:
			case LM:
			case LONGMIXED:
			case LONGUPPER:
			case LU:
			case MAP:
			case MARGINS:
			case MAX:
			case MD:
			case MDECK:
			case MIG:
			case MIXED:
			case NAME:
			case NAT:
			case NATIONAL:
			case NATLANG:
			case NN:
			case NO:
			case NOADATA:
			case NOADV:
			case NOALIAS:
			case NOAWO:
			case NOBLOCK0:
			case NOC:
			case NOCBLCARD:
			case NOCICS:
			case NOCMPR2:
			case NOCOMPILE:
			case NOCPSM:
			case NOCURR:
			case NOCURRENCY:
			case NOD:
			case NODATEPROC:
			case NODBCS:
			case NODE:
			case NODEBUG:
			case NODECK:
			case NODIAGTRUNC:
			case NODLL:
			case NODU:
			case NODUMP:
			case NODP:
			case NODTR:
			case NODYN:
			case NODYNAM:
			case NOEDF:
			case NOEJPD:
			case NOEPILOG:
			case NOEXIT:
			case NOEXP:
			case NOEXPORTALL:
			case NOF:
			case NOFASTSRT:
			case NOFEPI:
			case NOFLAG:
			case NOFLAGMIG:
			case NOFLAGSTD:
			case NOFSRT:
			case NOGRAPHIC:
			case NOHOOK:
			case NOLENGTH:
			case NOLIB:
			case NOLINKAGE:
			case NOLIST:
			case NOMAP:
			case NOMD:
			case NOMDECK:
			case NONAME:
			case NONUM:
			case NONUMBER:
			case NOOBJ:
			case NOOBJECT:
			case NOOFF:
			case NOOFFSET:
			case NOOPSEQUENCE:
			case NOOPT:
			case NOOPTIMIZE:
			case NOOPTIONS:
			case NOP:
			case NOPFD:
			case NOPROLOG:
			case NORENT:
			case NOS:
			case NOSEP:
			case NOSEPARATE:
			case NOSEQ:
			case NOSOURCE:
			case NOSPIE:
			case NOSQL:
			case NOSQLC:
			case NOSQLCCSID:
			case NOSSR:
			case NOSSRANGE:
			case NOSTDTRUNC:
			case NOSEQUENCE:
			case NOTERM:
			case NOTERMINAL:
			case NOTEST:
			case NOTHREAD:
			case NOTRIG:
			case NOVBREF:
			case NOWORD:
			case NOX:
			case NOXREF:
			case NOZWB:
			case NS:
			case NSEQ:
			case NSYMBOL:
			case NUM:
			case NUMBER:
			case NUMPROC:
			case OBJ:
			case OBJECT:
			case OF:
			case OFF:
			case OFFSET:
			case ON:
			case OP:
			case OPMARGINS:
			case OPSEQUENCE:
			case OPT:
			case OPTFILE:
			case OPTIMIZE:
			case OPTIONS:
			case OUT:
			case OUTDD:
			case PFD:
			case PPTDBG:
			case PGMN:
			case PGMNAME:
			case PROCESS:
			case PROLOG:
			case QUOTE:
			case RENT:
			case REPLACING:
			case RMODE:
			case SEP:
			case SEPARATE:
			case SEQ:
			case SEQUENCE:
			case SHORT:
			case SIZE:
			case SOURCE:
			case SP:
			case SPACE:
			case SPIE:
			case SQL:
			case SQLC:
			case SQLCCSID:
			case SS:
			case SSR:
			case SSRANGE:
			case STD:
			case SYSEIB:
			case SZ:
			case TERM:
			case TERMINAL:
			case TEST:
			case THREAD:
			case TITLE:
			case TRIG:
			case TRUNC:
			case UE:
			case UPPER:
			case VBREF:
			case WD:
			case XMLPARSE:
			case XMLSS:
			case XOPTS:
			case XREF:
			case YEARWINDOW:
			case YW:
			case ZWB:
			case C_CHAR:
			case D_CHAR:
			case E_CHAR:
			case F_CHAR:
			case H_CHAR:
			case I_CHAR:
			case M_CHAR:
			case N_CHAR:
			case Q_CHAR:
			case S_CHAR:
			case U_CHAR:
			case W_CHAR:
			case X_CHAR:
			case COMMACHAR:
				enterOuterAlt(_localctx, 2);
				{
				setState(668);
				charDataKeyword();
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

	@SuppressWarnings("CheckReturnValue")
	public static class LiteralContext extends ParserRuleContext {
		public TerminalNode NONNUMERICLITERAL() { return getToken(Cobol85PreprocessorParser.NONNUMERICLITERAL, 0); }
		public TerminalNode NUMERICLITERAL() { return getToken(Cobol85PreprocessorParser.NUMERICLITERAL, 0); }
		public LiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_literal; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LiteralContext literal() throws RecognitionException {
		LiteralContext _localctx = new LiteralContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_literal);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(671);
			_la = _input.LA(1);
			if ( !(_la==NONNUMERICLITERAL || _la==NUMERICLITERAL) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
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

	@SuppressWarnings("CheckReturnValue")
	public static class FilenameContext extends ParserRuleContext {
		public TerminalNode FILENAME() { return getToken(Cobol85PreprocessorParser.FILENAME, 0); }
		public FilenameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_filename; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterFilename(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitFilename(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitFilename(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FilenameContext filename() throws RecognitionException {
		FilenameContext _localctx = new FilenameContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_filename);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(673);
			match(FILENAME);
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

	@SuppressWarnings("CheckReturnValue")
	public static class CharDataKeywordContext extends ParserRuleContext {
		public TerminalNode ADATA() { return getToken(Cobol85PreprocessorParser.ADATA, 0); }
		public TerminalNode ADV() { return getToken(Cobol85PreprocessorParser.ADV, 0); }
		public TerminalNode ALIAS() { return getToken(Cobol85PreprocessorParser.ALIAS, 0); }
		public TerminalNode ANSI() { return getToken(Cobol85PreprocessorParser.ANSI, 0); }
		public TerminalNode ANY() { return getToken(Cobol85PreprocessorParser.ANY, 0); }
		public TerminalNode APOST() { return getToken(Cobol85PreprocessorParser.APOST, 0); }
		public TerminalNode AR() { return getToken(Cobol85PreprocessorParser.AR, 0); }
		public TerminalNode ARITH() { return getToken(Cobol85PreprocessorParser.ARITH, 0); }
		public TerminalNode AUTO() { return getToken(Cobol85PreprocessorParser.AUTO, 0); }
		public TerminalNode AWO() { return getToken(Cobol85PreprocessorParser.AWO, 0); }
		public TerminalNode BIN() { return getToken(Cobol85PreprocessorParser.BIN, 0); }
		public TerminalNode BLOCK0() { return getToken(Cobol85PreprocessorParser.BLOCK0, 0); }
		public TerminalNode BUF() { return getToken(Cobol85PreprocessorParser.BUF, 0); }
		public TerminalNode BUFSIZE() { return getToken(Cobol85PreprocessorParser.BUFSIZE, 0); }
		public TerminalNode BY() { return getToken(Cobol85PreprocessorParser.BY, 0); }
		public TerminalNode CBL() { return getToken(Cobol85PreprocessorParser.CBL, 0); }
		public TerminalNode CBLCARD() { return getToken(Cobol85PreprocessorParser.CBLCARD, 0); }
		public TerminalNode CO() { return getToken(Cobol85PreprocessorParser.CO, 0); }
		public TerminalNode COBOL2() { return getToken(Cobol85PreprocessorParser.COBOL2, 0); }
		public TerminalNode COBOL3() { return getToken(Cobol85PreprocessorParser.COBOL3, 0); }
		public TerminalNode CODEPAGE() { return getToken(Cobol85PreprocessorParser.CODEPAGE, 0); }
		public TerminalNode COMMACHAR() { return getToken(Cobol85PreprocessorParser.COMMACHAR, 0); }
		public TerminalNode COMPAT() { return getToken(Cobol85PreprocessorParser.COMPAT, 0); }
		public TerminalNode COMPILE() { return getToken(Cobol85PreprocessorParser.COMPILE, 0); }
		public TerminalNode CP() { return getToken(Cobol85PreprocessorParser.CP, 0); }
		public TerminalNode CPP() { return getToken(Cobol85PreprocessorParser.CPP, 0); }
		public TerminalNode CPSM() { return getToken(Cobol85PreprocessorParser.CPSM, 0); }
		public TerminalNode CS() { return getToken(Cobol85PreprocessorParser.CS, 0); }
		public TerminalNode CURR() { return getToken(Cobol85PreprocessorParser.CURR, 0); }
		public TerminalNode CURRENCY() { return getToken(Cobol85PreprocessorParser.CURRENCY, 0); }
		public TerminalNode DATA() { return getToken(Cobol85PreprocessorParser.DATA, 0); }
		public TerminalNode DATEPROC() { return getToken(Cobol85PreprocessorParser.DATEPROC, 0); }
		public TerminalNode DBCS() { return getToken(Cobol85PreprocessorParser.DBCS, 0); }
		public TerminalNode DD() { return getToken(Cobol85PreprocessorParser.DD, 0); }
		public TerminalNode DEBUG() { return getToken(Cobol85PreprocessorParser.DEBUG, 0); }
		public TerminalNode DECK() { return getToken(Cobol85PreprocessorParser.DECK, 0); }
		public TerminalNode DIAGTRUNC() { return getToken(Cobol85PreprocessorParser.DIAGTRUNC, 0); }
		public TerminalNode DLI() { return getToken(Cobol85PreprocessorParser.DLI, 0); }
		public TerminalNode DLL() { return getToken(Cobol85PreprocessorParser.DLL, 0); }
		public TerminalNode DP() { return getToken(Cobol85PreprocessorParser.DP, 0); }
		public TerminalNode DTR() { return getToken(Cobol85PreprocessorParser.DTR, 0); }
		public TerminalNode DU() { return getToken(Cobol85PreprocessorParser.DU, 0); }
		public TerminalNode DUMP() { return getToken(Cobol85PreprocessorParser.DUMP, 0); }
		public TerminalNode DYN() { return getToken(Cobol85PreprocessorParser.DYN, 0); }
		public TerminalNode DYNAM() { return getToken(Cobol85PreprocessorParser.DYNAM, 0); }
		public TerminalNode EDF() { return getToken(Cobol85PreprocessorParser.EDF, 0); }
		public TerminalNode EJECT() { return getToken(Cobol85PreprocessorParser.EJECT, 0); }
		public TerminalNode EJPD() { return getToken(Cobol85PreprocessorParser.EJPD, 0); }
		public TerminalNode EN() { return getToken(Cobol85PreprocessorParser.EN, 0); }
		public TerminalNode ENGLISH() { return getToken(Cobol85PreprocessorParser.ENGLISH, 0); }
		public TerminalNode EPILOG() { return getToken(Cobol85PreprocessorParser.EPILOG, 0); }
		public TerminalNode EXCI() { return getToken(Cobol85PreprocessorParser.EXCI, 0); }
		public TerminalNode EXIT() { return getToken(Cobol85PreprocessorParser.EXIT, 0); }
		public TerminalNode EXP() { return getToken(Cobol85PreprocessorParser.EXP, 0); }
		public TerminalNode EXPORTALL() { return getToken(Cobol85PreprocessorParser.EXPORTALL, 0); }
		public TerminalNode EXTEND() { return getToken(Cobol85PreprocessorParser.EXTEND, 0); }
		public TerminalNode FASTSRT() { return getToken(Cobol85PreprocessorParser.FASTSRT, 0); }
		public TerminalNode FLAG() { return getToken(Cobol85PreprocessorParser.FLAG, 0); }
		public TerminalNode FLAGSTD() { return getToken(Cobol85PreprocessorParser.FLAGSTD, 0); }
		public TerminalNode FULL() { return getToken(Cobol85PreprocessorParser.FULL, 0); }
		public TerminalNode FSRT() { return getToken(Cobol85PreprocessorParser.FSRT, 0); }
		public TerminalNode GDS() { return getToken(Cobol85PreprocessorParser.GDS, 0); }
		public TerminalNode GRAPHIC() { return getToken(Cobol85PreprocessorParser.GRAPHIC, 0); }
		public TerminalNode HOOK() { return getToken(Cobol85PreprocessorParser.HOOK, 0); }
		public TerminalNode IN() { return getToken(Cobol85PreprocessorParser.IN, 0); }
		public TerminalNode INTDATE() { return getToken(Cobol85PreprocessorParser.INTDATE, 0); }
		public TerminalNode JA() { return getToken(Cobol85PreprocessorParser.JA, 0); }
		public TerminalNode JP() { return getToken(Cobol85PreprocessorParser.JP, 0); }
		public TerminalNode KA() { return getToken(Cobol85PreprocessorParser.KA, 0); }
		public TerminalNode LANG() { return getToken(Cobol85PreprocessorParser.LANG, 0); }
		public TerminalNode LANGUAGE() { return getToken(Cobol85PreprocessorParser.LANGUAGE, 0); }
		public TerminalNode LC() { return getToken(Cobol85PreprocessorParser.LC, 0); }
		public TerminalNode LENGTH() { return getToken(Cobol85PreprocessorParser.LENGTH, 0); }
		public TerminalNode LIB() { return getToken(Cobol85PreprocessorParser.LIB, 0); }
		public TerminalNode LILIAN() { return getToken(Cobol85PreprocessorParser.LILIAN, 0); }
		public TerminalNode LIN() { return getToken(Cobol85PreprocessorParser.LIN, 0); }
		public TerminalNode LINECOUNT() { return getToken(Cobol85PreprocessorParser.LINECOUNT, 0); }
		public TerminalNode LINKAGE() { return getToken(Cobol85PreprocessorParser.LINKAGE, 0); }
		public TerminalNode LIST() { return getToken(Cobol85PreprocessorParser.LIST, 0); }
		public TerminalNode LM() { return getToken(Cobol85PreprocessorParser.LM, 0); }
		public TerminalNode LONGMIXED() { return getToken(Cobol85PreprocessorParser.LONGMIXED, 0); }
		public TerminalNode LONGUPPER() { return getToken(Cobol85PreprocessorParser.LONGUPPER, 0); }
		public TerminalNode LU() { return getToken(Cobol85PreprocessorParser.LU, 0); }
		public TerminalNode MAP() { return getToken(Cobol85PreprocessorParser.MAP, 0); }
		public TerminalNode MARGINS() { return getToken(Cobol85PreprocessorParser.MARGINS, 0); }
		public TerminalNode MAX() { return getToken(Cobol85PreprocessorParser.MAX, 0); }
		public TerminalNode MD() { return getToken(Cobol85PreprocessorParser.MD, 0); }
		public TerminalNode MDECK() { return getToken(Cobol85PreprocessorParser.MDECK, 0); }
		public TerminalNode MIG() { return getToken(Cobol85PreprocessorParser.MIG, 0); }
		public TerminalNode MIXED() { return getToken(Cobol85PreprocessorParser.MIXED, 0); }
		public TerminalNode NAME() { return getToken(Cobol85PreprocessorParser.NAME, 0); }
		public TerminalNode NAT() { return getToken(Cobol85PreprocessorParser.NAT, 0); }
		public TerminalNode NATIONAL() { return getToken(Cobol85PreprocessorParser.NATIONAL, 0); }
		public TerminalNode NATLANG() { return getToken(Cobol85PreprocessorParser.NATLANG, 0); }
		public TerminalNode NN() { return getToken(Cobol85PreprocessorParser.NN, 0); }
		public TerminalNode NO() { return getToken(Cobol85PreprocessorParser.NO, 0); }
		public TerminalNode NOADATA() { return getToken(Cobol85PreprocessorParser.NOADATA, 0); }
		public TerminalNode NOADV() { return getToken(Cobol85PreprocessorParser.NOADV, 0); }
		public TerminalNode NOALIAS() { return getToken(Cobol85PreprocessorParser.NOALIAS, 0); }
		public TerminalNode NOAWO() { return getToken(Cobol85PreprocessorParser.NOAWO, 0); }
		public TerminalNode NOBLOCK0() { return getToken(Cobol85PreprocessorParser.NOBLOCK0, 0); }
		public TerminalNode NOC() { return getToken(Cobol85PreprocessorParser.NOC, 0); }
		public TerminalNode NOCBLCARD() { return getToken(Cobol85PreprocessorParser.NOCBLCARD, 0); }
		public TerminalNode NOCICS() { return getToken(Cobol85PreprocessorParser.NOCICS, 0); }
		public TerminalNode NOCMPR2() { return getToken(Cobol85PreprocessorParser.NOCMPR2, 0); }
		public TerminalNode NOCOMPILE() { return getToken(Cobol85PreprocessorParser.NOCOMPILE, 0); }
		public TerminalNode NOCPSM() { return getToken(Cobol85PreprocessorParser.NOCPSM, 0); }
		public TerminalNode NOCURR() { return getToken(Cobol85PreprocessorParser.NOCURR, 0); }
		public TerminalNode NOCURRENCY() { return getToken(Cobol85PreprocessorParser.NOCURRENCY, 0); }
		public TerminalNode NOD() { return getToken(Cobol85PreprocessorParser.NOD, 0); }
		public TerminalNode NODATEPROC() { return getToken(Cobol85PreprocessorParser.NODATEPROC, 0); }
		public TerminalNode NODBCS() { return getToken(Cobol85PreprocessorParser.NODBCS, 0); }
		public TerminalNode NODE() { return getToken(Cobol85PreprocessorParser.NODE, 0); }
		public TerminalNode NODEBUG() { return getToken(Cobol85PreprocessorParser.NODEBUG, 0); }
		public TerminalNode NODECK() { return getToken(Cobol85PreprocessorParser.NODECK, 0); }
		public TerminalNode NODIAGTRUNC() { return getToken(Cobol85PreprocessorParser.NODIAGTRUNC, 0); }
		public TerminalNode NODLL() { return getToken(Cobol85PreprocessorParser.NODLL, 0); }
		public TerminalNode NODU() { return getToken(Cobol85PreprocessorParser.NODU, 0); }
		public TerminalNode NODUMP() { return getToken(Cobol85PreprocessorParser.NODUMP, 0); }
		public TerminalNode NODP() { return getToken(Cobol85PreprocessorParser.NODP, 0); }
		public TerminalNode NODTR() { return getToken(Cobol85PreprocessorParser.NODTR, 0); }
		public TerminalNode NODYN() { return getToken(Cobol85PreprocessorParser.NODYN, 0); }
		public TerminalNode NODYNAM() { return getToken(Cobol85PreprocessorParser.NODYNAM, 0); }
		public TerminalNode NOEDF() { return getToken(Cobol85PreprocessorParser.NOEDF, 0); }
		public TerminalNode NOEJPD() { return getToken(Cobol85PreprocessorParser.NOEJPD, 0); }
		public TerminalNode NOEPILOG() { return getToken(Cobol85PreprocessorParser.NOEPILOG, 0); }
		public TerminalNode NOEXIT() { return getToken(Cobol85PreprocessorParser.NOEXIT, 0); }
		public TerminalNode NOEXP() { return getToken(Cobol85PreprocessorParser.NOEXP, 0); }
		public TerminalNode NOEXPORTALL() { return getToken(Cobol85PreprocessorParser.NOEXPORTALL, 0); }
		public TerminalNode NOF() { return getToken(Cobol85PreprocessorParser.NOF, 0); }
		public TerminalNode NOFASTSRT() { return getToken(Cobol85PreprocessorParser.NOFASTSRT, 0); }
		public TerminalNode NOFEPI() { return getToken(Cobol85PreprocessorParser.NOFEPI, 0); }
		public TerminalNode NOFLAG() { return getToken(Cobol85PreprocessorParser.NOFLAG, 0); }
		public TerminalNode NOFLAGMIG() { return getToken(Cobol85PreprocessorParser.NOFLAGMIG, 0); }
		public TerminalNode NOFLAGSTD() { return getToken(Cobol85PreprocessorParser.NOFLAGSTD, 0); }
		public TerminalNode NOFSRT() { return getToken(Cobol85PreprocessorParser.NOFSRT, 0); }
		public TerminalNode NOGRAPHIC() { return getToken(Cobol85PreprocessorParser.NOGRAPHIC, 0); }
		public TerminalNode NOHOOK() { return getToken(Cobol85PreprocessorParser.NOHOOK, 0); }
		public TerminalNode NOLENGTH() { return getToken(Cobol85PreprocessorParser.NOLENGTH, 0); }
		public TerminalNode NOLIB() { return getToken(Cobol85PreprocessorParser.NOLIB, 0); }
		public TerminalNode NOLINKAGE() { return getToken(Cobol85PreprocessorParser.NOLINKAGE, 0); }
		public TerminalNode NOLIST() { return getToken(Cobol85PreprocessorParser.NOLIST, 0); }
		public TerminalNode NOMAP() { return getToken(Cobol85PreprocessorParser.NOMAP, 0); }
		public TerminalNode NOMD() { return getToken(Cobol85PreprocessorParser.NOMD, 0); }
		public TerminalNode NOMDECK() { return getToken(Cobol85PreprocessorParser.NOMDECK, 0); }
		public TerminalNode NONAME() { return getToken(Cobol85PreprocessorParser.NONAME, 0); }
		public TerminalNode NONUM() { return getToken(Cobol85PreprocessorParser.NONUM, 0); }
		public TerminalNode NONUMBER() { return getToken(Cobol85PreprocessorParser.NONUMBER, 0); }
		public TerminalNode NOOBJ() { return getToken(Cobol85PreprocessorParser.NOOBJ, 0); }
		public TerminalNode NOOBJECT() { return getToken(Cobol85PreprocessorParser.NOOBJECT, 0); }
		public TerminalNode NOOFF() { return getToken(Cobol85PreprocessorParser.NOOFF, 0); }
		public TerminalNode NOOFFSET() { return getToken(Cobol85PreprocessorParser.NOOFFSET, 0); }
		public TerminalNode NOOPSEQUENCE() { return getToken(Cobol85PreprocessorParser.NOOPSEQUENCE, 0); }
		public TerminalNode NOOPT() { return getToken(Cobol85PreprocessorParser.NOOPT, 0); }
		public TerminalNode NOOPTIMIZE() { return getToken(Cobol85PreprocessorParser.NOOPTIMIZE, 0); }
		public TerminalNode NOOPTIONS() { return getToken(Cobol85PreprocessorParser.NOOPTIONS, 0); }
		public TerminalNode NOP() { return getToken(Cobol85PreprocessorParser.NOP, 0); }
		public TerminalNode NOPFD() { return getToken(Cobol85PreprocessorParser.NOPFD, 0); }
		public TerminalNode NOPROLOG() { return getToken(Cobol85PreprocessorParser.NOPROLOG, 0); }
		public TerminalNode NORENT() { return getToken(Cobol85PreprocessorParser.NORENT, 0); }
		public TerminalNode NOS() { return getToken(Cobol85PreprocessorParser.NOS, 0); }
		public TerminalNode NOSEP() { return getToken(Cobol85PreprocessorParser.NOSEP, 0); }
		public TerminalNode NOSEPARATE() { return getToken(Cobol85PreprocessorParser.NOSEPARATE, 0); }
		public TerminalNode NOSEQ() { return getToken(Cobol85PreprocessorParser.NOSEQ, 0); }
		public TerminalNode NOSEQUENCE() { return getToken(Cobol85PreprocessorParser.NOSEQUENCE, 0); }
		public TerminalNode NOSOURCE() { return getToken(Cobol85PreprocessorParser.NOSOURCE, 0); }
		public TerminalNode NOSPIE() { return getToken(Cobol85PreprocessorParser.NOSPIE, 0); }
		public TerminalNode NOSQL() { return getToken(Cobol85PreprocessorParser.NOSQL, 0); }
		public TerminalNode NOSQLC() { return getToken(Cobol85PreprocessorParser.NOSQLC, 0); }
		public TerminalNode NOSQLCCSID() { return getToken(Cobol85PreprocessorParser.NOSQLCCSID, 0); }
		public TerminalNode NOSSR() { return getToken(Cobol85PreprocessorParser.NOSSR, 0); }
		public TerminalNode NOSSRANGE() { return getToken(Cobol85PreprocessorParser.NOSSRANGE, 0); }
		public TerminalNode NOSTDTRUNC() { return getToken(Cobol85PreprocessorParser.NOSTDTRUNC, 0); }
		public TerminalNode NOTERM() { return getToken(Cobol85PreprocessorParser.NOTERM, 0); }
		public TerminalNode NOTERMINAL() { return getToken(Cobol85PreprocessorParser.NOTERMINAL, 0); }
		public TerminalNode NOTEST() { return getToken(Cobol85PreprocessorParser.NOTEST, 0); }
		public TerminalNode NOTHREAD() { return getToken(Cobol85PreprocessorParser.NOTHREAD, 0); }
		public TerminalNode NOTRIG() { return getToken(Cobol85PreprocessorParser.NOTRIG, 0); }
		public TerminalNode NOVBREF() { return getToken(Cobol85PreprocessorParser.NOVBREF, 0); }
		public TerminalNode NOWORD() { return getToken(Cobol85PreprocessorParser.NOWORD, 0); }
		public TerminalNode NOX() { return getToken(Cobol85PreprocessorParser.NOX, 0); }
		public TerminalNode NOXREF() { return getToken(Cobol85PreprocessorParser.NOXREF, 0); }
		public TerminalNode NOZWB() { return getToken(Cobol85PreprocessorParser.NOZWB, 0); }
		public TerminalNode NSEQ() { return getToken(Cobol85PreprocessorParser.NSEQ, 0); }
		public TerminalNode NSYMBOL() { return getToken(Cobol85PreprocessorParser.NSYMBOL, 0); }
		public TerminalNode NS() { return getToken(Cobol85PreprocessorParser.NS, 0); }
		public TerminalNode NUM() { return getToken(Cobol85PreprocessorParser.NUM, 0); }
		public TerminalNode NUMBER() { return getToken(Cobol85PreprocessorParser.NUMBER, 0); }
		public TerminalNode NUMPROC() { return getToken(Cobol85PreprocessorParser.NUMPROC, 0); }
		public TerminalNode OBJ() { return getToken(Cobol85PreprocessorParser.OBJ, 0); }
		public TerminalNode OBJECT() { return getToken(Cobol85PreprocessorParser.OBJECT, 0); }
		public TerminalNode ON() { return getToken(Cobol85PreprocessorParser.ON, 0); }
		public TerminalNode OF() { return getToken(Cobol85PreprocessorParser.OF, 0); }
		public TerminalNode OFF() { return getToken(Cobol85PreprocessorParser.OFF, 0); }
		public TerminalNode OFFSET() { return getToken(Cobol85PreprocessorParser.OFFSET, 0); }
		public TerminalNode OPMARGINS() { return getToken(Cobol85PreprocessorParser.OPMARGINS, 0); }
		public TerminalNode OPSEQUENCE() { return getToken(Cobol85PreprocessorParser.OPSEQUENCE, 0); }
		public TerminalNode OPTIMIZE() { return getToken(Cobol85PreprocessorParser.OPTIMIZE, 0); }
		public TerminalNode OP() { return getToken(Cobol85PreprocessorParser.OP, 0); }
		public TerminalNode OPT() { return getToken(Cobol85PreprocessorParser.OPT, 0); }
		public TerminalNode OPTFILE() { return getToken(Cobol85PreprocessorParser.OPTFILE, 0); }
		public TerminalNode OPTIONS() { return getToken(Cobol85PreprocessorParser.OPTIONS, 0); }
		public TerminalNode OUT() { return getToken(Cobol85PreprocessorParser.OUT, 0); }
		public TerminalNode OUTDD() { return getToken(Cobol85PreprocessorParser.OUTDD, 0); }
		public TerminalNode PFD() { return getToken(Cobol85PreprocessorParser.PFD, 0); }
		public TerminalNode PGMN() { return getToken(Cobol85PreprocessorParser.PGMN, 0); }
		public TerminalNode PGMNAME() { return getToken(Cobol85PreprocessorParser.PGMNAME, 0); }
		public TerminalNode PPTDBG() { return getToken(Cobol85PreprocessorParser.PPTDBG, 0); }
		public TerminalNode PROCESS() { return getToken(Cobol85PreprocessorParser.PROCESS, 0); }
		public TerminalNode PROLOG() { return getToken(Cobol85PreprocessorParser.PROLOG, 0); }
		public TerminalNode QUOTE() { return getToken(Cobol85PreprocessorParser.QUOTE, 0); }
		public TerminalNode RENT() { return getToken(Cobol85PreprocessorParser.RENT, 0); }
		public TerminalNode REPLACING() { return getToken(Cobol85PreprocessorParser.REPLACING, 0); }
		public TerminalNode RMODE() { return getToken(Cobol85PreprocessorParser.RMODE, 0); }
		public TerminalNode SEQ() { return getToken(Cobol85PreprocessorParser.SEQ, 0); }
		public TerminalNode SEQUENCE() { return getToken(Cobol85PreprocessorParser.SEQUENCE, 0); }
		public TerminalNode SEP() { return getToken(Cobol85PreprocessorParser.SEP, 0); }
		public TerminalNode SEPARATE() { return getToken(Cobol85PreprocessorParser.SEPARATE, 0); }
		public TerminalNode SHORT() { return getToken(Cobol85PreprocessorParser.SHORT, 0); }
		public TerminalNode SIZE() { return getToken(Cobol85PreprocessorParser.SIZE, 0); }
		public TerminalNode SOURCE() { return getToken(Cobol85PreprocessorParser.SOURCE, 0); }
		public TerminalNode SP() { return getToken(Cobol85PreprocessorParser.SP, 0); }
		public TerminalNode SPACE() { return getToken(Cobol85PreprocessorParser.SPACE, 0); }
		public TerminalNode SPIE() { return getToken(Cobol85PreprocessorParser.SPIE, 0); }
		public TerminalNode SQL() { return getToken(Cobol85PreprocessorParser.SQL, 0); }
		public TerminalNode SQLC() { return getToken(Cobol85PreprocessorParser.SQLC, 0); }
		public TerminalNode SQLCCSID() { return getToken(Cobol85PreprocessorParser.SQLCCSID, 0); }
		public TerminalNode SS() { return getToken(Cobol85PreprocessorParser.SS, 0); }
		public TerminalNode SSR() { return getToken(Cobol85PreprocessorParser.SSR, 0); }
		public TerminalNode SSRANGE() { return getToken(Cobol85PreprocessorParser.SSRANGE, 0); }
		public TerminalNode STD() { return getToken(Cobol85PreprocessorParser.STD, 0); }
		public TerminalNode SYSEIB() { return getToken(Cobol85PreprocessorParser.SYSEIB, 0); }
		public TerminalNode SZ() { return getToken(Cobol85PreprocessorParser.SZ, 0); }
		public TerminalNode TERM() { return getToken(Cobol85PreprocessorParser.TERM, 0); }
		public TerminalNode TERMINAL() { return getToken(Cobol85PreprocessorParser.TERMINAL, 0); }
		public TerminalNode TEST() { return getToken(Cobol85PreprocessorParser.TEST, 0); }
		public TerminalNode THREAD() { return getToken(Cobol85PreprocessorParser.THREAD, 0); }
		public TerminalNode TITLE() { return getToken(Cobol85PreprocessorParser.TITLE, 0); }
		public TerminalNode TRIG() { return getToken(Cobol85PreprocessorParser.TRIG, 0); }
		public TerminalNode TRUNC() { return getToken(Cobol85PreprocessorParser.TRUNC, 0); }
		public TerminalNode UE() { return getToken(Cobol85PreprocessorParser.UE, 0); }
		public TerminalNode UPPER() { return getToken(Cobol85PreprocessorParser.UPPER, 0); }
		public TerminalNode VBREF() { return getToken(Cobol85PreprocessorParser.VBREF, 0); }
		public TerminalNode WD() { return getToken(Cobol85PreprocessorParser.WD, 0); }
		public TerminalNode XMLPARSE() { return getToken(Cobol85PreprocessorParser.XMLPARSE, 0); }
		public TerminalNode XMLSS() { return getToken(Cobol85PreprocessorParser.XMLSS, 0); }
		public TerminalNode XOPTS() { return getToken(Cobol85PreprocessorParser.XOPTS, 0); }
		public TerminalNode XREF() { return getToken(Cobol85PreprocessorParser.XREF, 0); }
		public TerminalNode YEARWINDOW() { return getToken(Cobol85PreprocessorParser.YEARWINDOW, 0); }
		public TerminalNode YW() { return getToken(Cobol85PreprocessorParser.YW, 0); }
		public TerminalNode ZWB() { return getToken(Cobol85PreprocessorParser.ZWB, 0); }
		public TerminalNode C_CHAR() { return getToken(Cobol85PreprocessorParser.C_CHAR, 0); }
		public TerminalNode D_CHAR() { return getToken(Cobol85PreprocessorParser.D_CHAR, 0); }
		public TerminalNode E_CHAR() { return getToken(Cobol85PreprocessorParser.E_CHAR, 0); }
		public TerminalNode F_CHAR() { return getToken(Cobol85PreprocessorParser.F_CHAR, 0); }
		public TerminalNode H_CHAR() { return getToken(Cobol85PreprocessorParser.H_CHAR, 0); }
		public TerminalNode I_CHAR() { return getToken(Cobol85PreprocessorParser.I_CHAR, 0); }
		public TerminalNode M_CHAR() { return getToken(Cobol85PreprocessorParser.M_CHAR, 0); }
		public TerminalNode N_CHAR() { return getToken(Cobol85PreprocessorParser.N_CHAR, 0); }
		public TerminalNode Q_CHAR() { return getToken(Cobol85PreprocessorParser.Q_CHAR, 0); }
		public TerminalNode S_CHAR() { return getToken(Cobol85PreprocessorParser.S_CHAR, 0); }
		public TerminalNode U_CHAR() { return getToken(Cobol85PreprocessorParser.U_CHAR, 0); }
		public TerminalNode W_CHAR() { return getToken(Cobol85PreprocessorParser.W_CHAR, 0); }
		public TerminalNode X_CHAR() { return getToken(Cobol85PreprocessorParser.X_CHAR, 0); }
		public CharDataKeywordContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_charDataKeyword; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).enterCharDataKeyword(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof Cobol85PreprocessorListener ) ((Cobol85PreprocessorListener)listener).exitCharDataKeyword(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof Cobol85PreprocessorVisitor ) return ((Cobol85PreprocessorVisitor<? extends T>)visitor).visitCharDataKeyword(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CharDataKeywordContext charDataKeyword() throws RecognitionException {
		CharDataKeywordContext _localctx = new CharDataKeywordContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_charDataKeyword);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(675);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & -2346375405893844994L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & -16785409L) != 0) || ((((_la - 128)) & ~0x3f) == 0 && ((1L << (_la - 128)) & -288230376151711745L) != 0) || ((((_la - 192)) & ~0x3f) == 0 && ((1L << (_la - 192)) & -9534967251992577L) != 0) || ((((_la - 256)) & ~0x3f) == 0 && ((1L << (_la - 256)) & 100663159L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
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
		"\u0004\u0001\u0124\u02a6\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001"+
		"\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004"+
		"\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007"+
		"\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b"+
		"\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007"+
		"\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007"+
		"\u0012\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007"+
		"\u0015\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002\u0018\u0007"+
		"\u0018\u0002\u0019\u0007\u0019\u0002\u001a\u0007\u001a\u0002\u001b\u0007"+
		"\u001b\u0002\u001c\u0007\u001c\u0002\u001d\u0007\u001d\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0005\u0000I\b"+
		"\u0000\n\u0000\f\u0000L\t\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001"+
		"\u0001\u0003\u0001R\b\u0001\u0001\u0001\u0001\u0001\u0004\u0001V\b\u0001"+
		"\u000b\u0001\f\u0001W\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0003\u0002^\b\u0002\u0001\u0002\u0005\u0002a\b\u0002\n\u0002\f\u0002"+
		"d\t\u0002\u0001\u0002\u0001\u0002\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0003\u0003"+
		"|\b\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0003\u0003\u0095\b\u0003\u0001\u0003\u0003\u0003\u0098\b\u0003\u0001"+
		"\u0003\u0003\u0003\u009b\b\u0003\u0001\u0003\u0003\u0003\u009e\b\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0003\u0003\u00b2\b\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0003\u0003\u00ba\b\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0003\u0003\u00da\b\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0003\u0003\u00e2\b\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0003\u0003\u00e8\b\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0003\u0003"+
		"\u00f9\b\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0003\u0003\u0142\b\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0003\u0003\u0151\b\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0003\u0003\u0167\b\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0003\u0003"+
		"\u0171\b\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0003\u0003"+
		"\u0177\b\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0003\u0003\u0187\b\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0003\u0003\u0190\b\u0003\u0001\u0003\u0003\u0003\u0193\b\u0003\u0001"+
		"\u0003\u0003\u0003\u0196\b\u0003\u0001\u0003\u0003\u0003\u0199\b\u0003"+
		"\u0001\u0003\u0003\u0003\u019c\b\u0003\u0001\u0003\u0003\u0003\u019f\b"+
		"\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001"+
		"\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001"+
		"\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001"+
		"\u0003\u0003\u0003\u01b3\b\u0003\u0001\u0003\u0003\u0003\u01b6\b\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0003\u0003\u01be\b\u0003\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004"+
		"\u0001\u0004\u0003\u0004\u01c5\b\u0004\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0001\u0005\u0003\u0005\u01cc\b\u0005\u0001\u0006\u0001\u0006"+
		"\u0001\u0006\u0001\u0006\u0001\u0006\u0003\u0006\u01d3\b\u0006\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0005\u0007\u01d8\b\u0007\n\u0007\f\u0007\u01db"+
		"\t\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0003\u0007\u01e1"+
		"\b\u0007\u0005\u0007\u01e3\b\u0007\n\u0007\f\u0007\u01e6\t\u0007\u0001"+
		"\u0007\u0005\u0007\u01e9\b\u0007\n\u0007\f\u0007\u01ec\t\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\b\u0001\b\u0001\b\u0003\b\u01f3\b\b\u0001\b\u0001\b"+
		"\u0003\b\u01f7\b\b\u0001\t\u0001\t\u0003\t\u01fb\b\t\u0001\n\u0001\n\u0005"+
		"\n\u01ff\b\n\n\n\f\n\u0202\t\n\u0001\n\u0001\n\u0004\n\u0206\b\n\u000b"+
		"\n\f\n\u0207\u0001\n\u0005\n\u020b\b\n\n\n\f\n\u020e\t\n\u0001\u000b\u0001"+
		"\u000b\u0001\u000b\u0005\u000b\u0213\b\u000b\n\u000b\f\u000b\u0216\t\u000b"+
		"\u0001\u000b\u0003\u000b\u0219\b\u000b\u0001\f\u0001\f\u0005\f\u021d\b"+
		"\f\n\f\f\f\u0220\t\f\u0001\f\u0004\f\u0223\b\f\u000b\f\f\f\u0224\u0001"+
		"\f\u0001\f\u0001\r\u0001\r\u0001\r\u0001\r\u0001\u000e\u0001\u000e\u0005"+
		"\u000e\u022f\b\u000e\n\u000e\f\u000e\u0232\t\u000e\u0001\u000e\u0001\u000e"+
		"\u0005\u000e\u0236\b\u000e\n\u000e\f\u000e\u0239\t\u000e\u0001\u000e\u0001"+
		"\u000e\u0005\u000e\u023d\b\u000e\n\u000e\f\u000e\u0240\t\u000e\u0001\u000e"+
		"\u0003\u000e\u0243\b\u000e\u0001\u000e\u0005\u000e\u0246\b\u000e\n\u000e"+
		"\f\u000e\u0249\t\u000e\u0001\u000e\u0003\u000e\u024c\b\u000e\u0001\u000f"+
		"\u0001\u000f\u0005\u000f\u0250\b\u000f\n\u000f\f\u000f\u0253\t\u000f\u0001"+
		"\u000f\u0001\u000f\u0003\u000f\u0257\b\u000f\u0001\u0010\u0001\u0010\u0005"+
		"\u0010\u025b\b\u0010\n\u0010\f\u0010\u025e\t\u0010\u0001\u0010\u0001\u0010"+
		"\u0003\u0010\u0262\b\u0010\u0001\u0011\u0001\u0011\u0001\u0011\u0001\u0011"+
		"\u0003\u0011\u0268\b\u0011\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012"+
		"\u0003\u0012\u026e\b\u0012\u0001\u0013\u0001\u0013\u0003\u0013\u0272\b"+
		"\u0013\u0001\u0014\u0001\u0014\u0003\u0014\u0276\b\u0014\u0001\u0015\u0001"+
		"\u0015\u0001\u0015\u0003\u0015\u027b\b\u0015\u0001\u0016\u0001\u0016\u0003"+
		"\u0016\u027f\b\u0016\u0001\u0016\u0001\u0016\u0001\u0017\u0001\u0017\u0004"+
		"\u0017\u0285\b\u0017\u000b\u0017\f\u0017\u0286\u0001\u0018\u0001\u0018"+
		"\u0001\u0018\u0001\u0018\u0004\u0018\u028d\b\u0018\u000b\u0018\f\u0018"+
		"\u028e\u0001\u0019\u0001\u0019\u0001\u0019\u0001\u0019\u0001\u0019\u0001"+
		"\u0019\u0001\u0019\u0004\u0019\u0298\b\u0019\u000b\u0019\f\u0019\u0299"+
		"\u0001\u001a\u0001\u001a\u0003\u001a\u029e\b\u001a\u0001\u001b\u0001\u001b"+
		"\u0001\u001c\u0001\u001c\u0001\u001d\u0001\u001d\u0001\u001d\u0000\u0000"+
		"\u001e\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018"+
		"\u001a\u001c\u001e \"$&(*,.02468:\u0000S\u0002\u0000\u0010\u0010\u00d8"+
		"\u00d8\u0001\u0000\u0007\b\u0004\u0000\u0017\u0017;;\u010c\u010c\u010e"+
		"\u010e\u0001\u0000\r\u000e\u0002\u0000\u0016\u0016\u001a\u001a\u0002\u0000"+
		"\u0018\u0018\u010c\u010c\u0001\u0000\u001e\u001f\u0002\u0000!!))\u0002"+
		"\u0000>>\u008b\u008b\u0002\u0000\u00b8\u00b8\u00fd\u00fd\u0002\u0000%"+
		"%\u010d\u010d\u0002\u0000&&**\u0001\u0000+,\u0001\u0000-.\u0001\u0000"+
		"9:\u0002\u0000<<@@\u0002\u0000>>\u010f\u010f\u0003\u0000\u010e\u010e\u0111"+
		"\u0111\u0115\u0117\u0001\u0000\u0110\u0112\u0006\u0000##ee\u00f1\u00f1"+
		"\u010d\u010d\u0113\u0113\u0115\u0115\u0002\u0000\u0004\u0004PP\u0001\u0000"+
		"JK\u0004\u0000\u001d\u001d23GI\u00ff\u00ff\u0002\u0000LLRR\u0001\u0000"+
		"]^\u0004\u0000\u0018\u0018llpp\u010c\u010c\u0002\u0000\u0003\u0003ii\u0003"+
		"\u0000\u001d\u001d22II\u0002\u0000llpp\u0003\u0000\u010e\u010e\u0115\u0115"+
		"\u0117\u0117\u0001\u0000rs\u0002\u0000uu~~\u0002\u0000ttyy\u0001\u0000"+
		"|}\u0002\u0000zz\u007f\u007f\u0001\u0000\u0080\u0081\u0001\u0000\u0086"+
		"\u0087\u0002\u0000\u0089\u0089\u008e\u008e\u0002\u0000\u0088\u0088\u008b"+
		"\u008b\u0001\u0000\u0096\u0097\u0001\u0000\u0099\u009a\u0001\u0000\u009b"+
		"\u009c\u0001\u0000\u009d\u009e\u0001\u0000\u00a0\u00a1\u0002\u0000\u00aa"+
		"\u00aa\u00b3\u00b3\u0002\u0000\u00a7\u00a7\u00ab\u00ab\u0001\u0000\u00ae"+
		"\u00af\u0001\u0000\u00b0\u00b1\u0001\u0000\u00b4\u00b5\u0001\u0000\u00ba"+
		"\u00bb\u0002\u0000\u00bf\u00bf\u00c1\u00c1\u0002\u0000\"\"bc\u0001\u0000"+
		"\u00bc\u00bd\u0001\u0000\u00c2\u00c3\u0003\u0000__\u00a4\u00a4\u00d4\u00d4"+
		"\u0001\u0000\u00c5\u00c6\u0001\u0000\u00c8\u00c9\u0002\u0000\u00ce\u00ce"+
		"\u00d0\u00d0\u0002\u0000AA\u00f4\u00f4\u0001\u0000\u00d2\u00d3\u0001\u0000"+
		"\u00d6\u00d7\b\u0000\u0013\u0013\u0017\u0017UWYY``\u0100\u0100\u0112\u0112"+
		"\u0116\u0116\u0002\u0000\u00da\u00da\u0114\u0114\u0001\u0000\u00e2\u00e3"+
		"\u0002\u0000\u00e5\u00e5\u00f7\u00f7\u0002\u0000\u00e6\u00e6\u0115\u0115"+
		"\u0001\u0000\u00eb\u00ec\u0001\u0000\u00f2\u00f3\u0001\u0000\u00f8\u00f9"+
		"\u0002\u0000DD\u0090\u0090\u0002\u0000\u00a8\u00a9\u00e0\u00e1\u0002\u0000"+
		"11\u0083\u0083\u0003\u0000\u000b\u000b\u00ce\u00ce\u00f4\u00f4\u0001\u0000"+
		"\u0102\u0103\u0002\u0000\u0104\u0104\u0107\u0107\u0004\u0000\u0017\u0017"+
		"\u0105\u0105\u010c\u010c\u0118\u0118\u0002\u0000\u0108\u0108\u0118\u0118"+
		"\u0002\u0000AA\u00e4\u00e4\u0001\u0000\u0109\u010a\u0002\u0000EE\u00c7"+
		"\u00c7\u0001\u0000\u00ee\u00f0\u0001\u0000\u011d\u011e\u0010\u0000\u0001"+
		"\u0011\u0013\u0018\u001a3568<>LNWY\u00b9\u00bb\u00db\u00dd\u00de\u00e0"+
		"\u00ec\u00f1\u00f4\u00f6\u0102\u0104\u0106\u0108\u0118\u011a\u011a\u037b"+
		"\u0000J\u0001\u0000\u0000\u0000\u0002O\u0001\u0000\u0000\u0000\u0004Y"+
		"\u0001\u0000\u0000\u0000\u0006\u01bd\u0001\u0000\u0000\u0000\b\u01bf\u0001"+
		"\u0000\u0000\u0000\n\u01c6\u0001\u0000\u0000\u0000\f\u01cd\u0001\u0000"+
		"\u0000\u0000\u000e\u01d4\u0001\u0000\u0000\u0000\u0010\u01f2\u0001\u0000"+
		"\u0000\u0000\u0012\u01fa\u0001\u0000\u0000\u0000\u0014\u01fc\u0001\u0000"+
		"\u0000\u0000\u0016\u020f\u0001\u0000\u0000\u0000\u0018\u021a\u0001\u0000"+
		"\u0000\u0000\u001a\u0228\u0001\u0000\u0000\u0000\u001c\u022c\u0001\u0000"+
		"\u0000\u0000\u001e\u024d\u0001\u0000\u0000\u0000 \u0258\u0001\u0000\u0000"+
		"\u0000\"\u0267\u0001\u0000\u0000\u0000$\u026d\u0001\u0000\u0000\u0000"+
		"&\u026f\u0001\u0000\u0000\u0000(\u0273\u0001\u0000\u0000\u0000*\u0277"+
		"\u0001\u0000\u0000\u0000,\u027c\u0001\u0000\u0000\u0000.\u0284\u0001\u0000"+
		"\u0000\u00000\u028c\u0001\u0000\u0000\u00002\u0297\u0001\u0000\u0000\u0000"+
		"4\u029d\u0001\u0000\u0000\u00006\u029f\u0001\u0000\u0000\u00008\u02a1"+
		"\u0001\u0000\u0000\u0000:\u02a3\u0001\u0000\u0000\u0000<I\u0003\u0002"+
		"\u0001\u0000=I\u0003\u000e\u0007\u0000>I\u0003\b\u0004\u0000?I\u0003\n"+
		"\u0005\u0000@I\u0003\f\u0006\u0000AI\u0003\u001a\r\u0000BI\u0003\u0016"+
		"\u000b\u0000CI\u0003&\u0013\u0000DI\u0003(\u0014\u0000EI\u0003*\u0015"+
		"\u0000FI\u00032\u0019\u0000GI\u0005\u0121\u0000\u0000H<\u0001\u0000\u0000"+
		"\u0000H=\u0001\u0000\u0000\u0000H>\u0001\u0000\u0000\u0000H?\u0001\u0000"+
		"\u0000\u0000H@\u0001\u0000\u0000\u0000HA\u0001\u0000\u0000\u0000HB\u0001"+
		"\u0000\u0000\u0000HC\u0001\u0000\u0000\u0000HD\u0001\u0000\u0000\u0000"+
		"HE\u0001\u0000\u0000\u0000HF\u0001\u0000\u0000\u0000HG\u0001\u0000\u0000"+
		"\u0000IL\u0001\u0000\u0000\u0000JH\u0001\u0000\u0000\u0000JK\u0001\u0000"+
		"\u0000\u0000KM\u0001\u0000\u0000\u0000LJ\u0001\u0000\u0000\u0000MN\u0005"+
		"\u0000\u0000\u0001N\u0001\u0001\u0000\u0000\u0000OU\u0007\u0000\u0000"+
		"\u0000PR\u0005\u011a\u0000\u0000QP\u0001\u0000\u0000\u0000QR\u0001\u0000"+
		"\u0000\u0000RS\u0001\u0000\u0000\u0000SV\u0003\u0006\u0003\u0000TV\u0003"+
		"\u0004\u0002\u0000UQ\u0001\u0000\u0000\u0000UT\u0001\u0000\u0000\u0000"+
		"VW\u0001\u0000\u0000\u0000WU\u0001\u0000\u0000\u0000WX\u0001\u0000\u0000"+
		"\u0000X\u0003\u0001\u0000\u0000\u0000YZ\u0005\u0106\u0000\u0000Z[\u0005"+
		"X\u0000\u0000[b\u0003\u0006\u0003\u0000\\^\u0005\u011a\u0000\u0000]\\"+
		"\u0001\u0000\u0000\u0000]^\u0001\u0000\u0000\u0000^_\u0001\u0000\u0000"+
		"\u0000_a\u0003\u0006\u0003\u0000`]\u0001\u0000\u0000\u0000ad\u0001\u0000"+
		"\u0000\u0000b`\u0001\u0000\u0000\u0000bc\u0001\u0000\u0000\u0000ce\u0001"+
		"\u0000\u0000\u0000db\u0001\u0000\u0000\u0000ef\u0005\u00df\u0000\u0000"+
		"f\u0005\u0001\u0000\u0000\u0000g\u01be\u0005\u0001\u0000\u0000h\u01be"+
		"\u0005\u0002\u0000\u0000i\u01be\u0005\u0006\u0000\u0000jk\u0007\u0001"+
		"\u0000\u0000kl\u0005X\u0000\u0000lm\u0007\u0002\u0000\u0000m\u01be\u0005"+
		"\u00df\u0000\u0000n\u01be\u0005\n\u0000\u0000o\u01be\u0005\f\u0000\u0000"+
		"pq\u0007\u0003\u0000\u0000qr\u0005X\u0000\u0000rs\u00036\u001b\u0000s"+
		"t\u0005\u00df\u0000\u0000t\u01be\u0001\u0000\u0000\u0000u\u01be\u0005"+
		"\u0011\u0000\u0000v{\u0005\u0012\u0000\u0000wx\u0005X\u0000\u0000xy\u0003"+
		"6\u001b\u0000yz\u0005\u00df\u0000\u0000z|\u0001\u0000\u0000\u0000{w\u0001"+
		"\u0000\u0000\u0000{|\u0001\u0000\u0000\u0000|\u01be\u0001\u0000\u0000"+
		"\u0000}\u01be\u0005\u0014\u0000\u0000~\u01be\u0005\u0015\u0000\u0000\u007f"+
		"\u0080\u0007\u0004\u0000\u0000\u0080\u0081\u0005X\u0000\u0000\u0081\u0082"+
		"\u00036\u001b\u0000\u0082\u0083\u0005\u00df\u0000\u0000\u0083\u01be\u0001"+
		"\u0000\u0000\u0000\u0084\u01be\u0007\u0005\u0000\u0000\u0085\u01be\u0005"+
		"\u001b\u0000\u0000\u0086\u01be\u0005\u001c\u0000\u0000\u0087\u0088\u0007"+
		"\u0006\u0000\u0000\u0088\u0089\u0005X\u0000\u0000\u0089\u008a\u00036\u001b"+
		"\u0000\u008a\u008b\u0005\u00df\u0000\u0000\u008b\u01be\u0001\u0000\u0000"+
		"\u0000\u008c\u008d\u0005 \u0000\u0000\u008d\u008e\u0005X\u0000\u0000\u008e"+
		"\u008f\u00036\u001b\u0000\u008f\u0090\u0005\u00df\u0000\u0000\u0090\u01be"+
		"\u0001\u0000\u0000\u0000\u0091\u009d\u0007\u0007\u0000\u0000\u0092\u0094"+
		"\u0005X\u0000\u0000\u0093\u0095\u0007\b\u0000\u0000\u0094\u0093\u0001"+
		"\u0000\u0000\u0000\u0094\u0095\u0001\u0000\u0000\u0000\u0095\u0097\u0001"+
		"\u0000\u0000\u0000\u0096\u0098\u0005\u011a\u0000\u0000\u0097\u0096\u0001"+
		"\u0000\u0000\u0000\u0097\u0098\u0001\u0000\u0000\u0000\u0098\u009a\u0001"+
		"\u0000\u0000\u0000\u0099\u009b\u0007\t\u0000\u0000\u009a\u0099\u0001\u0000"+
		"\u0000\u0000\u009a\u009b\u0001\u0000\u0000\u0000\u009b\u009c\u0001\u0000"+
		"\u0000\u0000\u009c\u009e\u0005\u00df\u0000\u0000\u009d\u0092\u0001\u0000"+
		"\u0000\u0000\u009d\u009e\u0001\u0000\u0000\u0000\u009e\u01be\u0001\u0000"+
		"\u0000\u0000\u009f\u01be\u0005\"\u0000\u0000\u00a0\u01be\u0007\n\u0000"+
		"\u0000\u00a1\u01be\u0005$\u0000\u0000\u00a2\u01be\u0007\u000b\u0000\u0000"+
		"\u00a3\u01be\u0005(\u0000\u0000\u00a4\u01be\u0007\f\u0000\u0000\u00a5"+
		"\u01be\u0007\r\u0000\u0000\u00a6\u01be\u0005/\u0000\u0000\u00a7\u01be"+
		"\u00055\u0000\u0000\u00a8\u01be\u00058\u0000\u0000\u00a9\u01be\u0007\u000e"+
		"\u0000\u0000\u00aa\u01be\u0007\u000f\u0000\u0000\u00ab\u01be\u0005=\u0000"+
		"\u0000\u00ac\u00ad\u0007\u0010\u0000\u0000\u00ad\u00ae\u0005X\u0000\u0000"+
		"\u00ae\u00b1\u0007\u0011\u0000\u0000\u00af\u00b0\u0005\u011a\u0000\u0000"+
		"\u00b0\u00b2\u0007\u0011\u0000\u0000\u00b1\u00af\u0001\u0000\u0000\u0000"+
		"\u00b1\u00b2\u0001\u0000\u0000\u0000\u00b2\u00b3\u0001\u0000\u0000\u0000"+
		"\u00b3\u01be\u0005\u00df\u0000\u0000\u00b4\u00b5\u0005?\u0000\u0000\u00b5"+
		"\u00b6\u0005X\u0000\u0000\u00b6\u00b9\u0007\u0012\u0000\u0000\u00b7\u00b8"+
		"\u0005\u011a\u0000\u0000\u00b8\u00ba\u0007\u0013\u0000\u0000\u00b9\u00b7"+
		"\u0001\u0000\u0000\u0000\u00b9\u00ba\u0001\u0000\u0000\u0000\u00ba\u00bb"+
		"\u0001\u0000\u0000\u0000\u00bb\u01be\u0005\u00df\u0000\u0000\u00bc\u01be"+
		"\u0005B\u0000\u0000\u00bd\u01be\u0005C\u0000\u0000\u00be\u00bf\u0005F"+
		"\u0000\u0000\u00bf\u00c0\u0005X\u0000\u0000\u00c0\u00c1\u0007\u0014\u0000"+
		"\u0000\u00c1\u01be\u0005\u00df\u0000\u0000\u00c2\u00c3\u0007\u0015\u0000"+
		"\u0000\u00c3\u00c4\u0005X\u0000\u0000\u00c4\u00c5\u0007\u0016\u0000\u0000"+
		"\u00c5\u01be\u0005\u00df\u0000\u0000\u00c6\u01be\u0005M\u0000\u0000\u00c7"+
		"\u01be\u0005N\u0000\u0000\u00c8\u01be\u0005O\u0000\u0000\u00c9\u01be\u0005"+
		"Q\u0000\u0000\u00ca\u00cb\u0007\u0017\u0000\u0000\u00cb\u00cc\u0005X\u0000"+
		"\u0000\u00cc\u00cd\u00036\u001b\u0000\u00cd\u00ce\u0005\u00df\u0000\u0000"+
		"\u00ce\u01be\u0001\u0000\u0000\u0000\u00cf\u01be\u0005S\u0000\u0000\u00d0"+
		"\u01be\u0005T\u0000\u0000\u00d1\u01be\u0005Z\u0000\u0000\u00d2\u00d3\u0005"+
		"[\u0000\u0000\u00d3\u00d4\u0005X\u0000\u0000\u00d4\u00d5\u00036\u001b"+
		"\u0000\u00d5\u00d6\u0005\u011a\u0000\u0000\u00d6\u00d9\u00036\u001b\u0000"+
		"\u00d7\u00d8\u0005\u011a\u0000\u0000\u00d8\u00da\u00036\u001b\u0000\u00d9"+
		"\u00d7\u0001\u0000\u0000\u0000\u00d9\u00da\u0001\u0000\u0000\u0000\u00da"+
		"\u00db\u0001\u0000\u0000\u0000\u00db\u00dc\u0005\u00df\u0000\u0000\u00dc"+
		"\u01be\u0001\u0000\u0000\u0000\u00dd\u00e1\u0007\u0018\u0000\u0000\u00de"+
		"\u00df\u0005X\u0000\u0000\u00df\u00e0\u0007\u0019\u0000\u0000\u00e0\u00e2"+
		"\u0005\u00df\u0000\u0000\u00e1\u00de\u0001\u0000\u0000\u0000\u00e1\u00e2"+
		"\u0001\u0000\u0000\u0000\u00e2\u01be\u0001\u0000\u0000\u0000\u00e3\u00e7"+
		"\u0005a\u0000\u0000\u00e4\u00e5\u0005X\u0000\u0000\u00e5\u00e6\u0007\u001a"+
		"\u0000\u0000\u00e6\u00e8\u0005\u00df\u0000\u0000\u00e7\u00e4\u0001\u0000"+
		"\u0000\u0000\u00e7\u00e8\u0001\u0000\u0000\u0000\u00e8\u01be\u0001\u0000"+
		"\u0000\u0000\u00e9\u00ea\u0005d\u0000\u0000\u00ea\u00eb\u0005X\u0000\u0000"+
		"\u00eb\u00ec\u0007\u001b\u0000\u0000\u00ec\u01be\u0005\u00df\u0000\u0000"+
		"\u00ed\u01be\u0005g\u0000\u0000\u00ee\u01be\u0005h\u0000\u0000\u00ef\u01be"+
		"\u0005j\u0000\u0000\u00f0\u01be\u0005k\u0000\u0000\u00f1\u01be\u0005m"+
		"\u0000\u0000\u00f2\u01be\u0005n\u0000\u0000\u00f3\u01be\u0005o\u0000\u0000"+
		"\u00f4\u00f8\u0007\u001c\u0000\u0000\u00f5\u00f6\u0005X\u0000\u0000\u00f6"+
		"\u00f7\u0007\u001d\u0000\u0000\u00f7\u00f9\u0005\u00df\u0000\u0000\u00f8"+
		"\u00f5\u0001\u0000\u0000\u0000\u00f8\u00f9\u0001\u0000\u0000\u0000\u00f9"+
		"\u01be\u0001\u0000\u0000\u0000\u00fa\u01be\u0005q\u0000\u0000\u00fb\u01be"+
		"\u0007\u001e\u0000\u0000\u00fc\u01be\u0007\u001f\u0000\u0000\u00fd\u01be"+
		"\u0005v\u0000\u0000\u00fe\u01be\u0005x\u0000\u0000\u00ff\u01be\u0007 "+
		"\u0000\u0000\u0100\u01be\u0005{\u0000\u0000\u0101\u01be\u0005w\u0000\u0000"+
		"\u0102\u01be\u0007!\u0000\u0000\u0103\u01be\u0007\"\u0000\u0000\u0104"+
		"\u01be\u0007#\u0000\u0000\u0105\u01be\u0005\u0082\u0000\u0000\u0106\u01be"+
		"\u0005\u0084\u0000\u0000\u0107\u01be\u0005\u0085\u0000\u0000\u0108\u01be"+
		"\u0007$\u0000\u0000\u0109\u01be\u0007%\u0000\u0000\u010a\u01be\u0005\u008a"+
		"\u0000\u0000\u010b\u01be\u0007&\u0000\u0000\u010c\u01be\u0005\u008c\u0000"+
		"\u0000\u010d\u01be\u0005\u008d\u0000\u0000\u010e\u01be\u0005\u008f\u0000"+
		"\u0000\u010f\u01be\u0005\u0091\u0000\u0000\u0110\u01be\u0005\u0092\u0000"+
		"\u0000\u0111\u01be\u0005\u0093\u0000\u0000\u0112\u01be\u0005\u0094\u0000"+
		"\u0000\u0113\u01be\u0005\u0095\u0000\u0000\u0114\u01be\u0007\'\u0000\u0000"+
		"\u0115\u01be\u0005\u0098\u0000\u0000\u0116\u01be\u0007(\u0000\u0000\u0117"+
		"\u01be\u0007)\u0000\u0000\u0118\u01be\u0007*\u0000\u0000\u0119\u01be\u0005"+
		"\u009f\u0000\u0000\u011a\u01be\u0007+\u0000\u0000\u011b\u01be\u0005\u00a2"+
		"\u0000\u0000\u011c\u01be\u0005\u00a3\u0000\u0000\u011d\u01be\u0005\u00a5"+
		"\u0000\u0000\u011e\u01be\u0005\u00a6\u0000\u0000\u011f\u01be\u0007,\u0000"+
		"\u0000\u0120\u01be\u0007-\u0000\u0000\u0121\u01be\u0005\u00ac\u0000\u0000"+
		"\u0122\u01be\u0005\u00ad\u0000\u0000\u0123\u01be\u0007.\u0000\u0000\u0124"+
		"\u01be\u0007/\u0000\u0000\u0125\u01be\u0005\u00b2\u0000\u0000\u0126\u01be"+
		"\u00070\u0000\u0000\u0127\u01be\u0005\u00b6\u0000\u0000\u0128\u01be\u0005"+
		"\u00b7\u0000\u0000\u0129\u01be\u0005\u00b9\u0000\u0000\u012a\u01be\u0007"+
		"1\u0000\u0000\u012b\u01be\u0005\u00c0\u0000\u0000\u012c\u012d\u00072\u0000"+
		"\u0000\u012d\u012e\u0005X\u0000\u0000\u012e\u012f\u00073\u0000\u0000\u012f"+
		"\u01be\u0005\u00df\u0000\u0000\u0130\u01be\u0005\u00b9\u0000\u0000\u0131"+
		"\u01be\u00074\u0000\u0000\u0132\u01be\u0005\u00be\u0000\u0000\u0133\u01be"+
		"\u00075\u0000\u0000\u0134\u0135\u0005\u00c4\u0000\u0000\u0135\u0136\u0005"+
		"X\u0000\u0000\u0136\u0137\u00076\u0000\u0000\u0137\u01be\u0005\u00df\u0000"+
		"\u0000\u0138\u01be\u00077\u0000\u0000\u0139\u01be\u00078\u0000\u0000\u013a"+
		"\u013b\u0005\u00cc\u0000\u0000\u013b\u013c\u0005X\u0000\u0000\u013c\u013d"+
		"\u00036\u001b\u0000\u013d\u013e\u0005\u011a\u0000\u0000\u013e\u0141\u0003"+
		"6\u001b\u0000\u013f\u0140\u0005\u011a\u0000\u0000\u0140\u0142\u00036\u001b"+
		"\u0000\u0141\u013f\u0001\u0000\u0000\u0000\u0141\u0142\u0001\u0000\u0000"+
		"\u0000\u0142\u0143\u0001\u0000\u0000\u0000\u0143\u0144\u0005\u00df\u0000"+
		"\u0000\u0144\u01be\u0001\u0000\u0000\u0000\u0145\u0146\u0005\u00cd\u0000"+
		"\u0000\u0146\u0147\u0005X\u0000\u0000\u0147\u0148\u00036\u001b\u0000\u0148"+
		"\u0149\u0005\u011a\u0000\u0000\u0149\u014a\u00036\u001b\u0000\u014a\u014b"+
		"\u0005\u00df\u0000\u0000\u014b\u01be\u0001\u0000\u0000\u0000\u014c\u0150"+
		"\u00079\u0000\u0000\u014d\u014e\u0005X\u0000\u0000\u014e\u014f\u0007:"+
		"\u0000\u0000\u014f\u0151\u0005\u00df\u0000\u0000\u0150\u014d\u0001\u0000"+
		"\u0000\u0000\u0150\u0151\u0001\u0000\u0000\u0000\u0151\u01be\u0001\u0000"+
		"\u0000\u0000\u0152\u01be\u0005\u00cf\u0000\u0000\u0153\u01be\u0005\u00d1"+
		"\u0000\u0000\u0154\u01be\u0005\u00cb\u0000\u0000\u0155\u0156\u0007;\u0000"+
		"\u0000\u0156\u0157\u0005X\u0000\u0000\u0157\u0158\u00034\u001a\u0000\u0158"+
		"\u0159\u0005\u00df\u0000\u0000\u0159\u01be\u0001\u0000\u0000\u0000\u015a"+
		"\u015b\u0007<\u0000\u0000\u015b\u015c\u0005X\u0000\u0000\u015c\u015d\u0007"+
		"=\u0000\u0000\u015d\u01be\u0005\u00df\u0000\u0000\u015e\u01be\u0005\u00d9"+
		"\u0000\u0000\u015f\u01be\u0007>\u0000\u0000\u0160\u01be\u0005\u00db\u0000"+
		"\u0000\u0161\u0162\u0005\u00de\u0000\u0000\u0162\u0166\u0005X\u0000\u0000"+
		"\u0163\u0167\u0005\u0005\u0000\u0000\u0164\u0167\u0005\t\u0000\u0000\u0165"+
		"\u0167\u00036\u001b\u0000\u0166\u0163\u0001\u0000\u0000\u0000\u0166\u0164"+
		"\u0001\u0000\u0000\u0000\u0166\u0165\u0001\u0000\u0000\u0000\u0167\u0168"+
		"\u0001\u0000\u0000\u0000\u0168\u01be\u0005\u00df\u0000\u0000\u0169\u0170"+
		"\u0007?\u0000\u0000\u016a\u016b\u0005X\u0000\u0000\u016b\u016c\u00036"+
		"\u001b\u0000\u016c\u016d\u0005\u011a\u0000\u0000\u016d\u016e\u00036\u001b"+
		"\u0000\u016e\u016f\u0005\u00df\u0000\u0000\u016f\u0171\u0001\u0000\u0000"+
		"\u0000\u0170\u016a\u0001\u0000\u0000\u0000\u0170\u0171\u0001\u0000\u0000"+
		"\u0000\u0171\u01be\u0001\u0000\u0000\u0000\u0172\u0173\u0007@\u0000\u0000"+
		"\u0173\u0176\u0005X\u0000\u0000\u0174\u0177\u0005\\\u0000\u0000\u0175"+
		"\u0177\u00036\u001b\u0000\u0176\u0174\u0001\u0000\u0000\u0000\u0176\u0175"+
		"\u0001\u0000\u0000\u0000\u0177\u0178\u0001\u0000\u0000\u0000\u0178\u01be"+
		"\u0005\u00df\u0000\u0000\u0179\u01be\u0007A\u0000\u0000\u017a\u01be\u0005"+
		"\u00e7\u0000\u0000\u017b\u017c\u0005\u00e8\u0000\u0000\u017c\u017d\u0005"+
		"X\u0000\u0000\u017d\u017e\u00036\u001b\u0000\u017e\u017f\u0005\u00df\u0000"+
		"\u0000\u017f\u01be\u0001\u0000\u0000\u0000\u0180\u01be\u0005\u00e9\u0000"+
		"\u0000\u0181\u0186\u0005\u00ea\u0000\u0000\u0182\u0183\u0005X\u0000\u0000"+
		"\u0183\u0184\u00036\u001b\u0000\u0184\u0185\u0005\u00df\u0000\u0000\u0185"+
		"\u0187\u0001\u0000\u0000\u0000\u0186\u0182\u0001\u0000\u0000\u0000\u0186"+
		"\u0187\u0001\u0000\u0000\u0000\u0187\u01be\u0001\u0000\u0000\u0000\u0188"+
		"\u01be\u0007B\u0000\u0000\u0189\u01be\u0007C\u0000\u0000\u018a\u01be\u0005"+
		"\u00f6\u0000\u0000\u018b\u01be\u0007D\u0000\u0000\u018c\u019e\u0005\u00fa"+
		"\u0000\u0000\u018d\u018f\u0005X\u0000\u0000\u018e\u0190\u0007E\u0000\u0000"+
		"\u018f\u018e\u0001\u0000\u0000\u0000\u018f\u0190\u0001\u0000\u0000\u0000"+
		"\u0190\u0192\u0001\u0000\u0000\u0000\u0191\u0193\u0005\u011a\u0000\u0000"+
		"\u0192\u0191\u0001\u0000\u0000\u0000\u0192\u0193\u0001\u0000\u0000\u0000"+
		"\u0193\u0195\u0001\u0000\u0000\u0000\u0194\u0196\u0007F\u0000\u0000\u0195"+
		"\u0194\u0001\u0000\u0000\u0000\u0195\u0196\u0001\u0000\u0000\u0000\u0196"+
		"\u0198\u0001\u0000\u0000\u0000\u0197\u0199\u0005\u011a\u0000\u0000\u0198"+
		"\u0197\u0001\u0000\u0000\u0000\u0198\u0199\u0001\u0000\u0000\u0000\u0199"+
		"\u019b\u0001\u0000\u0000\u0000\u019a\u019c\u0007G\u0000\u0000\u019b\u019a"+
		"\u0001\u0000\u0000\u0000\u019b\u019c\u0001\u0000\u0000\u0000\u019c\u019d"+
		"\u0001\u0000\u0000\u0000\u019d\u019f\u0005\u00df\u0000\u0000\u019e\u018d"+
		"\u0001\u0000\u0000\u0000\u019e\u019f\u0001\u0000\u0000\u0000\u019f\u01be"+
		"\u0001\u0000\u0000\u0000\u01a0\u01be\u0005\u00fb\u0000\u0000\u01a1\u01a2"+
		"\u0005\u00fe\u0000\u0000\u01a2\u01a3\u0005X\u0000\u0000\u01a3\u01a4\u0007"+
		"H\u0000\u0000\u01a4\u01be\u0005\u00df\u0000\u0000\u01a5\u01be\u0005\u0101"+
		"\u0000\u0000\u01a6\u01a7\u0007I\u0000\u0000\u01a7\u01a8\u0005X\u0000\u0000"+
		"\u01a8\u01a9\u00034\u001a\u0000\u01a9\u01aa\u0005\u00df\u0000\u0000\u01aa"+
		"\u01be\u0001\u0000\u0000\u0000\u01ab\u01ac\u0007J\u0000\u0000\u01ac\u01ad"+
		"\u0005X\u0000\u0000\u01ad\u01ae\u0007K\u0000\u0000\u01ae\u01be\u0005\u00df"+
		"\u0000\u0000\u01af\u01b5\u0007L\u0000\u0000\u01b0\u01b2\u0005X\u0000\u0000"+
		"\u01b1\u01b3\u0007M\u0000\u0000\u01b2\u01b1\u0001\u0000\u0000\u0000\u01b2"+
		"\u01b3\u0001\u0000\u0000\u0000\u01b3\u01b4\u0001\u0000\u0000\u0000\u01b4"+
		"\u01b6\u0005\u00df\u0000\u0000\u01b5\u01b0\u0001\u0000\u0000\u0000\u01b5"+
		"\u01b6\u0001\u0000\u0000\u0000\u01b6\u01be\u0001\u0000\u0000\u0000\u01b7"+
		"\u01b8\u0007N\u0000\u0000\u01b8\u01b9\u0005X\u0000\u0000\u01b9\u01ba\u0003"+
		"6\u001b\u0000\u01ba\u01bb\u0005\u00df\u0000\u0000\u01bb\u01be\u0001\u0000"+
		"\u0000\u0000\u01bc\u01be\u0005\u010b\u0000\u0000\u01bdg\u0001\u0000\u0000"+
		"\u0000\u01bdh\u0001\u0000\u0000\u0000\u01bdi\u0001\u0000\u0000\u0000\u01bd"+
		"j\u0001\u0000\u0000\u0000\u01bdn\u0001\u0000\u0000\u0000\u01bdo\u0001"+
		"\u0000\u0000\u0000\u01bdp\u0001\u0000\u0000\u0000\u01bdu\u0001\u0000\u0000"+
		"\u0000\u01bdv\u0001\u0000\u0000\u0000\u01bd}\u0001\u0000\u0000\u0000\u01bd"+
		"~\u0001\u0000\u0000\u0000\u01bd\u007f\u0001\u0000\u0000\u0000\u01bd\u0084"+
		"\u0001\u0000\u0000\u0000\u01bd\u0085\u0001\u0000\u0000\u0000\u01bd\u0086"+
		"\u0001\u0000\u0000\u0000\u01bd\u0087\u0001\u0000\u0000\u0000\u01bd\u008c"+
		"\u0001\u0000\u0000\u0000\u01bd\u0091\u0001\u0000\u0000\u0000\u01bd\u009f"+
		"\u0001\u0000\u0000\u0000\u01bd\u00a0\u0001\u0000\u0000\u0000\u01bd\u00a1"+
		"\u0001\u0000\u0000\u0000\u01bd\u00a2\u0001\u0000\u0000\u0000\u01bd\u00a3"+
		"\u0001\u0000\u0000\u0000\u01bd\u00a4\u0001\u0000\u0000\u0000\u01bd\u00a5"+
		"\u0001\u0000\u0000\u0000\u01bd\u00a6\u0001\u0000\u0000\u0000\u01bd\u00a7"+
		"\u0001\u0000\u0000\u0000\u01bd\u00a8\u0001\u0000\u0000\u0000\u01bd\u00a9"+
		"\u0001\u0000\u0000\u0000\u01bd\u00aa\u0001\u0000\u0000\u0000\u01bd\u00ab"+
		"\u0001\u0000\u0000\u0000\u01bd\u00ac\u0001\u0000\u0000\u0000\u01bd\u00b4"+
		"\u0001\u0000\u0000\u0000\u01bd\u00bc\u0001\u0000\u0000\u0000\u01bd\u00bd"+
		"\u0001\u0000\u0000\u0000\u01bd\u00be\u0001\u0000\u0000\u0000\u01bd\u00c2"+
		"\u0001\u0000\u0000\u0000\u01bd\u00c6\u0001\u0000\u0000\u0000\u01bd\u00c7"+
		"\u0001\u0000\u0000\u0000\u01bd\u00c8\u0001\u0000\u0000\u0000\u01bd\u00c9"+
		"\u0001\u0000\u0000\u0000\u01bd\u00ca\u0001\u0000\u0000\u0000\u01bd\u00cf"+
		"\u0001\u0000\u0000\u0000\u01bd\u00d0\u0001\u0000\u0000\u0000\u01bd\u00d1"+
		"\u0001\u0000\u0000\u0000\u01bd\u00d2\u0001\u0000\u0000\u0000\u01bd\u00dd"+
		"\u0001\u0000\u0000\u0000\u01bd\u00e3\u0001\u0000\u0000\u0000\u01bd\u00e9"+
		"\u0001\u0000\u0000\u0000\u01bd\u00ed\u0001\u0000\u0000\u0000\u01bd\u00ee"+
		"\u0001\u0000\u0000\u0000\u01bd\u00ef\u0001\u0000\u0000\u0000\u01bd\u00f0"+
		"\u0001\u0000\u0000\u0000\u01bd\u00f1\u0001\u0000\u0000\u0000\u01bd\u00f2"+
		"\u0001\u0000\u0000\u0000\u01bd\u00f3\u0001\u0000\u0000\u0000\u01bd\u00f4"+
		"\u0001\u0000\u0000\u0000\u01bd\u00fa\u0001\u0000\u0000\u0000\u01bd\u00fb"+
		"\u0001\u0000\u0000\u0000\u01bd\u00fc\u0001\u0000\u0000\u0000\u01bd\u00fd"+
		"\u0001\u0000\u0000\u0000\u01bd\u00fe\u0001\u0000\u0000\u0000\u01bd\u00ff"+
		"\u0001\u0000\u0000\u0000\u01bd\u0100\u0001\u0000\u0000\u0000\u01bd\u0101"+
		"\u0001\u0000\u0000\u0000\u01bd\u0102\u0001\u0000\u0000\u0000\u01bd\u0103"+
		"\u0001\u0000\u0000\u0000\u01bd\u0104\u0001\u0000\u0000\u0000\u01bd\u0105"+
		"\u0001\u0000\u0000\u0000\u01bd\u0106\u0001\u0000\u0000\u0000\u01bd\u0107"+
		"\u0001\u0000\u0000\u0000\u01bd\u0108\u0001\u0000\u0000\u0000\u01bd\u0109"+
		"\u0001\u0000\u0000\u0000\u01bd\u010a\u0001\u0000\u0000\u0000\u01bd\u010b"+
		"\u0001\u0000\u0000\u0000\u01bd\u010c\u0001\u0000\u0000\u0000\u01bd\u010d"+
		"\u0001\u0000\u0000\u0000\u01bd\u010e\u0001\u0000\u0000\u0000\u01bd\u010f"+
		"\u0001\u0000\u0000\u0000\u01bd\u0110\u0001\u0000\u0000\u0000\u01bd\u0111"+
		"\u0001\u0000\u0000\u0000\u01bd\u0112\u0001\u0000\u0000\u0000\u01bd\u0113"+
		"\u0001\u0000\u0000\u0000\u01bd\u0114\u0001\u0000\u0000\u0000\u01bd\u0115"+
		"\u0001\u0000\u0000\u0000\u01bd\u0116\u0001\u0000\u0000\u0000\u01bd\u0117"+
		"\u0001\u0000\u0000\u0000\u01bd\u0118\u0001\u0000\u0000\u0000\u01bd\u0119"+
		"\u0001\u0000\u0000\u0000\u01bd\u011a\u0001\u0000\u0000\u0000\u01bd\u011b"+
		"\u0001\u0000\u0000\u0000\u01bd\u011c\u0001\u0000\u0000\u0000\u01bd\u011d"+
		"\u0001\u0000\u0000\u0000\u01bd\u011e\u0001\u0000\u0000\u0000\u01bd\u011f"+
		"\u0001\u0000\u0000\u0000\u01bd\u0120\u0001\u0000\u0000\u0000\u01bd\u0121"+
		"\u0001\u0000\u0000\u0000\u01bd\u0122\u0001\u0000\u0000\u0000\u01bd\u0123"+
		"\u0001\u0000\u0000\u0000\u01bd\u0124\u0001\u0000\u0000\u0000\u01bd\u0125"+
		"\u0001\u0000\u0000\u0000\u01bd\u0126\u0001\u0000\u0000\u0000\u01bd\u0127"+
		"\u0001\u0000\u0000\u0000\u01bd\u0128\u0001\u0000\u0000\u0000\u01bd\u0129"+
		"\u0001\u0000\u0000\u0000\u01bd\u012a\u0001\u0000\u0000\u0000\u01bd\u012b"+
		"\u0001\u0000\u0000\u0000\u01bd\u012c\u0001\u0000\u0000\u0000\u01bd\u0130"+
		"\u0001\u0000\u0000\u0000\u01bd\u0131\u0001\u0000\u0000\u0000\u01bd\u0132"+
		"\u0001\u0000\u0000\u0000\u01bd\u0133\u0001\u0000\u0000\u0000\u01bd\u0134"+
		"\u0001\u0000\u0000\u0000\u01bd\u0138\u0001\u0000\u0000\u0000\u01bd\u0139"+
		"\u0001\u0000\u0000\u0000\u01bd\u013a\u0001\u0000\u0000\u0000\u01bd\u0145"+
		"\u0001\u0000\u0000\u0000\u01bd\u014c\u0001\u0000\u0000\u0000\u01bd\u0152"+
		"\u0001\u0000\u0000\u0000\u01bd\u0153\u0001\u0000\u0000\u0000\u01bd\u0154"+
		"\u0001\u0000\u0000\u0000\u01bd\u0155\u0001\u0000\u0000\u0000\u01bd\u015a"+
		"\u0001\u0000\u0000\u0000\u01bd\u015e\u0001\u0000\u0000\u0000\u01bd\u015f"+
		"\u0001\u0000\u0000\u0000\u01bd\u0160\u0001\u0000\u0000\u0000\u01bd\u0161"+
		"\u0001\u0000\u0000\u0000\u01bd\u0169\u0001\u0000\u0000\u0000\u01bd\u0172"+
		"\u0001\u0000\u0000\u0000\u01bd\u0179\u0001\u0000\u0000\u0000\u01bd\u017a"+
		"\u0001\u0000\u0000\u0000\u01bd\u017b\u0001\u0000\u0000\u0000\u01bd\u0180"+
		"\u0001\u0000\u0000\u0000\u01bd\u0181\u0001\u0000\u0000\u0000\u01bd\u0188"+
		"\u0001\u0000\u0000\u0000\u01bd\u0189\u0001\u0000\u0000\u0000\u01bd\u018a"+
		"\u0001\u0000\u0000\u0000\u01bd\u018b\u0001\u0000\u0000\u0000\u01bd\u018c"+
		"\u0001\u0000\u0000\u0000\u01bd\u01a0\u0001\u0000\u0000\u0000\u01bd\u01a1"+
		"\u0001\u0000\u0000\u0000\u01bd\u01a5\u0001\u0000\u0000\u0000\u01bd\u01a6"+
		"\u0001\u0000\u0000\u0000\u01bd\u01ab\u0001\u0000\u0000\u0000\u01bd\u01af"+
		"\u0001\u0000\u0000\u0000\u01bd\u01b7\u0001\u0000\u0000\u0000\u01bd\u01bc"+
		"\u0001\u0000\u0000\u0000\u01be\u0007\u0001\u0000\u0000\u0000\u01bf\u01c0"+
		"\u00057\u0000\u0000\u01c0\u01c1\u0005\u0012\u0000\u0000\u01c1\u01c2\u0003"+
		".\u0017\u0000\u01c2\u01c4\u00054\u0000\u0000\u01c3\u01c5\u0005\u011b\u0000"+
		"\u0000\u01c4\u01c3\u0001\u0000\u0000\u0000\u01c4\u01c5\u0001\u0000\u0000"+
		"\u0000\u01c5\t\u0001\u0000\u0000\u0000\u01c6\u01c7\u00057\u0000\u0000"+
		"\u01c7\u01c8\u0005\u00ea\u0000\u0000\u01c8\u01c9\u00030\u0018\u0000\u01c9"+
		"\u01cb\u00054\u0000\u0000\u01ca\u01cc\u0005\u011b\u0000\u0000\u01cb\u01ca"+
		"\u0001\u0000\u0000\u0000\u01cb\u01cc\u0001\u0000\u0000\u0000\u01cc\u000b"+
		"\u0001\u0000\u0000\u0000\u01cd\u01ce\u00057\u0000\u0000\u01ce\u01cf\u0005"+
		"\u00ed\u0000\u0000\u01cf\u01d0\u0003.\u0017\u0000\u01d0\u01d2\u00054\u0000"+
		"\u0000\u01d1\u01d3\u0005\u011b\u0000\u0000\u01d2\u01d1\u0001\u0000\u0000"+
		"\u0000\u01d2\u01d3\u0001\u0000\u0000\u0000\u01d3\r\u0001\u0000\u0000\u0000"+
		"\u01d4\u01d5\u0005\u0019\u0000\u0000\u01d5\u01e4\u0003\u0010\b\u0000\u01d6"+
		"\u01d8\u0005\u0121\u0000\u0000\u01d7\u01d6\u0001\u0000\u0000\u0000\u01d8"+
		"\u01db\u0001\u0000\u0000\u0000\u01d9\u01d7\u0001\u0000\u0000\u0000\u01d9"+
		"\u01da\u0001\u0000\u0000\u0000\u01da\u01e0\u0001\u0000\u0000\u0000\u01db"+
		"\u01d9\u0001\u0000\u0000\u0000\u01dc\u01e1\u0003\u001e\u000f\u0000\u01dd"+
		"\u01e1\u0003 \u0010\u0000\u01de\u01e1\u0003\u0014\n\u0000\u01df\u01e1"+
		"\u0005\u00f5\u0000\u0000\u01e0\u01dc\u0001\u0000\u0000\u0000\u01e0\u01dd"+
		"\u0001\u0000\u0000\u0000\u01e0\u01de\u0001\u0000\u0000\u0000\u01e0\u01df"+
		"\u0001\u0000\u0000\u0000\u01e1\u01e3\u0001\u0000\u0000\u0000\u01e2\u01d9"+
		"\u0001\u0000\u0000\u0000\u01e3\u01e6\u0001\u0000\u0000\u0000\u01e4\u01e2"+
		"\u0001\u0000\u0000\u0000\u01e4\u01e5\u0001\u0000\u0000\u0000\u01e5\u01ea"+
		"\u0001\u0000\u0000\u0000\u01e6\u01e4\u0001\u0000\u0000\u0000\u01e7\u01e9"+
		"\u0005\u0121\u0000\u0000\u01e8\u01e7\u0001\u0000\u0000\u0000\u01e9\u01ec"+
		"\u0001\u0000\u0000\u0000\u01ea\u01e8\u0001\u0000\u0000\u0000\u01ea\u01eb"+
		"\u0001\u0000\u0000\u0000\u01eb\u01ed\u0001\u0000\u0000\u0000\u01ec\u01ea"+
		"\u0001\u0000\u0000\u0000\u01ed\u01ee\u0005\u011b\u0000\u0000\u01ee\u000f"+
		"\u0001\u0000\u0000\u0000\u01ef\u01f3\u00036\u001b\u0000\u01f0\u01f3\u0003"+
		"4\u001a\u0000\u01f1\u01f3\u00038\u001c\u0000\u01f2\u01ef\u0001\u0000\u0000"+
		"\u0000\u01f2\u01f0\u0001\u0000\u0000\u0000\u01f2\u01f1\u0001\u0000\u0000"+
		"\u0000\u01f3\u01f6\u0001\u0000\u0000\u0000\u01f4\u01f5\u0007O\u0000\u0000"+
		"\u01f5\u01f7\u0003\u0012\t\u0000\u01f6\u01f4\u0001\u0000\u0000\u0000\u01f6"+
		"\u01f7\u0001\u0000\u0000\u0000\u01f7\u0011\u0001\u0000\u0000\u0000\u01f8"+
		"\u01fb\u00036\u001b\u0000\u01f9\u01fb\u00034\u001a\u0000\u01fa\u01f8\u0001"+
		"\u0000\u0000\u0000\u01fa\u01f9\u0001\u0000\u0000\u0000\u01fb\u0013\u0001"+
		"\u0000\u0000\u0000\u01fc\u0200\u0005\u00dd\u0000\u0000\u01fd\u01ff\u0005"+
		"\u0121\u0000\u0000\u01fe\u01fd\u0001\u0000\u0000\u0000\u01ff\u0202\u0001"+
		"\u0000\u0000\u0000\u0200\u01fe\u0001\u0000\u0000\u0000\u0200\u0201\u0001"+
		"\u0000\u0000\u0000\u0201\u0203\u0001\u0000\u0000\u0000\u0202\u0200\u0001"+
		"\u0000\u0000\u0000\u0203\u020c\u0003\u001c\u000e\u0000\u0204\u0206\u0005"+
		"\u0121\u0000\u0000\u0205\u0204\u0001\u0000\u0000\u0000\u0206\u0207\u0001"+
		"\u0000\u0000\u0000\u0207\u0205\u0001\u0000\u0000\u0000\u0207\u0208\u0001"+
		"\u0000\u0000\u0000\u0208\u0209\u0001\u0000\u0000\u0000\u0209\u020b\u0003"+
		"\u001c\u000e\u0000\u020a\u0205\u0001\u0000\u0000\u0000\u020b\u020e\u0001"+
		"\u0000\u0000\u0000\u020c\u020a\u0001\u0000\u0000\u0000\u020c\u020d\u0001"+
		"\u0000\u0000\u0000\u020d\u0015\u0001\u0000\u0000\u0000\u020e\u020c\u0001"+
		"\u0000\u0000\u0000\u020f\u0214\u0003\u0018\f\u0000\u0210\u0213\u0003\u000e"+
		"\u0007\u0000\u0211\u0213\u0003.\u0017\u0000\u0212\u0210\u0001\u0000\u0000"+
		"\u0000\u0212\u0211\u0001\u0000\u0000\u0000\u0213\u0216\u0001\u0000\u0000"+
		"\u0000\u0214\u0212\u0001\u0000\u0000\u0000\u0214\u0215\u0001\u0000\u0000"+
		"\u0000\u0215\u0218\u0001\u0000\u0000\u0000\u0216\u0214\u0001\u0000\u0000"+
		"\u0000\u0217\u0219\u0003\u001a\r\u0000\u0218\u0217\u0001\u0000\u0000\u0000"+
		"\u0218\u0219\u0001\u0000\u0000\u0000\u0219\u0017\u0001\u0000\u0000\u0000"+
		"\u021a\u0222\u0005\u00dc\u0000\u0000\u021b\u021d\u0005\u0121\u0000\u0000"+
		"\u021c\u021b\u0001\u0000\u0000\u0000\u021d\u0220\u0001\u0000\u0000\u0000"+
		"\u021e\u021c\u0001\u0000\u0000\u0000\u021e\u021f\u0001\u0000\u0000\u0000"+
		"\u021f\u0221\u0001\u0000\u0000\u0000\u0220\u021e\u0001\u0000\u0000\u0000"+
		"\u0221\u0223\u0003\u001c\u000e\u0000\u0222\u021e\u0001\u0000\u0000\u0000"+
		"\u0223\u0224\u0001\u0000\u0000\u0000\u0224\u0222\u0001\u0000\u0000\u0000"+
		"\u0224\u0225\u0001\u0000\u0000\u0000\u0225\u0226\u0001\u0000\u0000\u0000"+
		"\u0226\u0227\u0005\u011b\u0000\u0000\u0227\u0019\u0001\u0000\u0000\u0000"+
		"\u0228\u0229\u0005\u00dc\u0000\u0000\u0229\u022a\u0005\u00c8\u0000\u0000"+
		"\u022a\u022b\u0005\u011b\u0000\u0000\u022b\u001b\u0001\u0000\u0000\u0000"+
		"\u022c\u0230\u0003\"\u0011\u0000\u022d\u022f\u0005\u0121\u0000\u0000\u022e"+
		"\u022d\u0001\u0000\u0000\u0000\u022f\u0232\u0001\u0000\u0000\u0000\u0230"+
		"\u022e\u0001\u0000\u0000\u0000\u0230\u0231\u0001\u0000\u0000\u0000\u0231"+
		"\u0233\u0001\u0000\u0000\u0000\u0232\u0230\u0001\u0000\u0000\u0000\u0233"+
		"\u0237\u0005\u000f\u0000\u0000\u0234\u0236\u0005\u0121\u0000\u0000\u0235"+
		"\u0234\u0001\u0000\u0000\u0000\u0236\u0239\u0001\u0000\u0000\u0000\u0237"+
		"\u0235\u0001\u0000\u0000\u0000\u0237\u0238\u0001\u0000\u0000\u0000\u0238"+
		"\u023a\u0001\u0000\u0000\u0000\u0239\u0237\u0001\u0000\u0000\u0000\u023a"+
		"\u0242\u0003$\u0012\u0000\u023b\u023d\u0005\u0121\u0000\u0000\u023c\u023b"+
		"\u0001\u0000\u0000\u0000\u023d\u0240\u0001\u0000\u0000\u0000\u023e\u023c"+
		"\u0001\u0000\u0000\u0000\u023e\u023f\u0001\u0000\u0000\u0000\u023f\u0241"+
		"\u0001\u0000\u0000\u0000\u0240\u023e\u0001\u0000\u0000\u0000\u0241\u0243"+
		"\u0003\u001e\u000f\u0000\u0242\u023e\u0001\u0000\u0000\u0000\u0242\u0243"+
		"\u0001\u0000\u0000\u0000\u0243\u024b\u0001\u0000\u0000\u0000\u0244\u0246"+
		"\u0005\u0121\u0000\u0000\u0245\u0244\u0001\u0000\u0000\u0000\u0246\u0249"+
		"\u0001\u0000\u0000\u0000\u0247\u0245\u0001\u0000\u0000\u0000\u0247\u0248"+
		"\u0001\u0000\u0000\u0000\u0248\u024a\u0001\u0000\u0000\u0000\u0249\u0247"+
		"\u0001\u0000\u0000\u0000\u024a\u024c\u0003 \u0010\u0000\u024b\u0247\u0001"+
		"\u0000\u0000\u0000\u024b\u024c\u0001\u0000\u0000\u0000\u024c\u001d\u0001"+
		"\u0000\u0000\u0000\u024d\u0251\u0007O\u0000\u0000\u024e\u0250\u0005\u0121"+
		"\u0000\u0000\u024f\u024e\u0001\u0000\u0000\u0000\u0250\u0253\u0001\u0000"+
		"\u0000\u0000\u0251\u024f\u0001\u0000\u0000\u0000\u0251\u0252\u0001\u0000"+
		"\u0000\u0000\u0252\u0256\u0001\u0000\u0000\u0000\u0253\u0251\u0001\u0000"+
		"\u0000\u0000\u0254\u0257\u00036\u001b\u0000\u0255\u0257\u00034\u001a\u0000"+
		"\u0256\u0254\u0001\u0000\u0000\u0000\u0256\u0255\u0001\u0000\u0000\u0000"+
		"\u0257\u001f\u0001\u0000\u0000\u0000\u0258\u025c\u0005\u00ca\u0000\u0000"+
		"\u0259\u025b\u0005\u0121\u0000\u0000\u025a\u0259\u0001\u0000\u0000\u0000"+
		"\u025b\u025e\u0001\u0000\u0000\u0000\u025c\u025a\u0001\u0000\u0000\u0000"+
		"\u025c\u025d\u0001\u0000\u0000\u0000\u025d\u0261\u0001\u0000\u0000\u0000"+
		"\u025e\u025c\u0001\u0000\u0000\u0000\u025f\u0262\u00036\u001b\u0000\u0260"+
		"\u0262\u00034\u001a\u0000\u0261\u025f\u0001\u0000\u0000\u0000\u0261\u0260"+
		"\u0001\u0000\u0000\u0000\u0262!\u0001\u0000\u0000\u0000\u0263\u0268\u0003"+
		"6\u001b\u0000\u0264\u0268\u00034\u001a\u0000\u0265\u0268\u0003,\u0016"+
		"\u0000\u0266\u0268\u00032\u0019\u0000\u0267\u0263\u0001\u0000\u0000\u0000"+
		"\u0267\u0264\u0001\u0000\u0000\u0000\u0267\u0265\u0001\u0000\u0000\u0000"+
		"\u0267\u0266\u0001\u0000\u0000\u0000\u0268#\u0001\u0000\u0000\u0000\u0269"+
		"\u026e\u00036\u001b\u0000\u026a\u026e\u00034\u001a\u0000\u026b\u026e\u0003"+
		",\u0016\u0000\u026c\u026e\u00032\u0019\u0000\u026d\u0269\u0001\u0000\u0000"+
		"\u0000\u026d\u026a\u0001\u0000\u0000\u0000\u026d\u026b\u0001\u0000\u0000"+
		"\u0000\u026d\u026c\u0001\u0000\u0000\u0000\u026e%\u0001\u0000\u0000\u0000"+
		"\u026f\u0271\u00050\u0000\u0000\u0270\u0272\u0005\u011b\u0000\u0000\u0271"+
		"\u0270\u0001\u0000\u0000\u0000\u0271\u0272\u0001\u0000\u0000\u0000\u0272"+
		"\'\u0001\u0000\u0000\u0000\u0273\u0275\u0007P\u0000\u0000\u0274\u0276"+
		"\u0005\u011b\u0000\u0000\u0275\u0274\u0001\u0000\u0000\u0000\u0275\u0276"+
		"\u0001\u0000\u0000\u0000\u0276)\u0001\u0000\u0000\u0000\u0277\u0278\u0005"+
		"\u00fc\u0000\u0000\u0278\u027a\u00036\u001b\u0000\u0279\u027b\u0005\u011b"+
		"\u0000\u0000\u027a\u0279\u0001\u0000\u0000\u0000\u027a\u027b\u0001\u0000"+
		"\u0000\u0000\u027b+\u0001\u0000\u0000\u0000\u027c\u027e\u0005\u011c\u0000"+
		"\u0000\u027d\u027f\u0003.\u0017\u0000\u027e\u027d\u0001\u0000\u0000\u0000"+
		"\u027e\u027f\u0001\u0000\u0000\u0000\u027f\u0280\u0001\u0000\u0000\u0000"+
		"\u0280\u0281\u0005\u011c\u0000\u0000\u0281-\u0001\u0000\u0000\u0000\u0282"+
		"\u0285\u00032\u0019\u0000\u0283\u0285\u0005\u0121\u0000\u0000\u0284\u0282"+
		"\u0001\u0000\u0000\u0000\u0284\u0283\u0001\u0000\u0000\u0000\u0285\u0286"+
		"\u0001\u0000\u0000\u0000\u0286\u0284\u0001\u0000\u0000\u0000\u0286\u0287"+
		"\u0001\u0000\u0000\u0000\u0287/\u0001\u0000\u0000\u0000\u0288\u028d\u0003"+
		"2\u0019\u0000\u0289\u028d\u0005\u0019\u0000\u0000\u028a\u028d\u0005\u00dc"+
		"\u0000\u0000\u028b\u028d\u0005\u0121\u0000\u0000\u028c\u0288\u0001\u0000"+
		"\u0000\u0000\u028c\u0289\u0001\u0000\u0000\u0000\u028c\u028a\u0001\u0000"+
		"\u0000\u0000\u028c\u028b\u0001\u0000\u0000\u0000\u028d\u028e\u0001\u0000"+
		"\u0000\u0000\u028e\u028c\u0001\u0000\u0000\u0000\u028e\u028f\u0001\u0000"+
		"\u0000\u0000\u028f1\u0001\u0000\u0000\u0000\u0290\u0298\u00034\u001a\u0000"+
		"\u0291\u0298\u00036\u001b\u0000\u0292\u0298\u00038\u001c\u0000\u0293\u0298"+
		"\u0005\u0124\u0000\u0000\u0294\u0298\u0005\u011b\u0000\u0000\u0295\u0298"+
		"\u0005X\u0000\u0000\u0296\u0298\u0005\u00df\u0000\u0000\u0297\u0290\u0001"+
		"\u0000\u0000\u0000\u0297\u0291\u0001\u0000\u0000\u0000\u0297\u0292\u0001"+
		"\u0000\u0000\u0000\u0297\u0293\u0001\u0000\u0000\u0000\u0297\u0294\u0001"+
		"\u0000\u0000\u0000\u0297\u0295\u0001\u0000\u0000\u0000\u0297\u0296\u0001"+
		"\u0000\u0000\u0000\u0298\u0299\u0001\u0000\u0000\u0000\u0299\u0297\u0001"+
		"\u0000\u0000\u0000\u0299\u029a\u0001\u0000\u0000\u0000\u029a3\u0001\u0000"+
		"\u0000\u0000\u029b\u029e\u0005\u011f\u0000\u0000\u029c\u029e\u0003:\u001d"+
		"\u0000\u029d\u029b\u0001\u0000\u0000\u0000\u029d\u029c\u0001\u0000\u0000"+
		"\u0000\u029e5\u0001\u0000\u0000\u0000\u029f\u02a0\u0007Q\u0000\u0000\u02a0"+
		"7\u0001\u0000\u0000\u0000\u02a1\u02a2\u0005\u0120\u0000\u0000\u02a29\u0001"+
		"\u0000\u0000\u0000\u02a3\u02a4\u0007R\u0000\u0000\u02a4;\u0001\u0000\u0000"+
		"\u0000JHJQUW]b{\u0094\u0097\u009a\u009d\u00b1\u00b9\u00d9\u00e1\u00e7"+
		"\u00f8\u0141\u0150\u0166\u0170\u0176\u0186\u018f\u0192\u0195\u0198\u019b"+
		"\u019e\u01b2\u01b5\u01bd\u01c4\u01cb\u01d2\u01d9\u01e0\u01e4\u01ea\u01f2"+
		"\u01f6\u01fa\u0200\u0207\u020c\u0212\u0214\u0218\u021e\u0224\u0230\u0237"+
		"\u023e\u0242\u0247\u024b\u0251\u0256\u025c\u0261\u0267\u026d\u0271\u0275"+
		"\u027a\u027e\u0284\u0286\u028c\u028e\u0297\u0299\u029d";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}