// Generated from Cobol85Preprocessor.g4 by ANTLR 4.13.2
package io.proleap.benchmark.antlr;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link Cobol85PreprocessorParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface Cobol85PreprocessorVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#startRule}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStartRule(Cobol85PreprocessorParser.StartRuleContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#compilerOptions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompilerOptions(Cobol85PreprocessorParser.CompilerOptionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#compilerXOpts}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompilerXOpts(Cobol85PreprocessorParser.CompilerXOptsContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#compilerOption}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompilerOption(Cobol85PreprocessorParser.CompilerOptionContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#execCicsStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecCicsStatement(Cobol85PreprocessorParser.ExecCicsStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#execSqlStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecSqlStatement(Cobol85PreprocessorParser.ExecSqlStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#execSqlImsStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecSqlImsStatement(Cobol85PreprocessorParser.ExecSqlImsStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#copyStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopyStatement(Cobol85PreprocessorParser.CopyStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#copySource}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopySource(Cobol85PreprocessorParser.CopySourceContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#copyLibrary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCopyLibrary(Cobol85PreprocessorParser.CopyLibraryContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#replacingPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReplacingPhrase(Cobol85PreprocessorParser.ReplacingPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#replaceArea}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReplaceArea(Cobol85PreprocessorParser.ReplaceAreaContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#replaceByStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReplaceByStatement(Cobol85PreprocessorParser.ReplaceByStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#replaceOffStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReplaceOffStatement(Cobol85PreprocessorParser.ReplaceOffStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#replaceClause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReplaceClause(Cobol85PreprocessorParser.ReplaceClauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#directoryPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDirectoryPhrase(Cobol85PreprocessorParser.DirectoryPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#familyPhrase}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFamilyPhrase(Cobol85PreprocessorParser.FamilyPhraseContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#replaceable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReplaceable(Cobol85PreprocessorParser.ReplaceableContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#replacement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReplacement(Cobol85PreprocessorParser.ReplacementContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#ejectStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEjectStatement(Cobol85PreprocessorParser.EjectStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#skipStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSkipStatement(Cobol85PreprocessorParser.SkipStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#titleStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTitleStatement(Cobol85PreprocessorParser.TitleStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#pseudoText}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPseudoText(Cobol85PreprocessorParser.PseudoTextContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#charData}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCharData(Cobol85PreprocessorParser.CharDataContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#charDataSql}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCharDataSql(Cobol85PreprocessorParser.CharDataSqlContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#charDataLine}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCharDataLine(Cobol85PreprocessorParser.CharDataLineContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#cobolWord}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCobolWord(Cobol85PreprocessorParser.CobolWordContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral(Cobol85PreprocessorParser.LiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#filename}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilename(Cobol85PreprocessorParser.FilenameContext ctx);
	/**
	 * Visit a parse tree produced by {@link Cobol85PreprocessorParser#charDataKeyword}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCharDataKeyword(Cobol85PreprocessorParser.CharDataKeywordContext ctx);
}