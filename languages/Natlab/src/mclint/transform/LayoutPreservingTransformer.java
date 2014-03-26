package mclint.transform;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import mclint.util.AstUtil;
import mclint.util.Parsing;

import org.antlr.runtime.Token;

import ast.ASTNode;
import ast.Program;

import com.google.common.io.CharStreams;

/**
 * A utility for performing layout-preserving AST transformations.
 * 
 * Given the text of an input program, this class maintains a token stream together with an AST
 * and keeps them synchronized as the AST changes.
 *
 */
class LayoutPreservingTransformer implements Transformer {
  private TokenStream tokenStream;
  private Program program;

  static LayoutPreservingTransformer of(Reader source) throws IOException {
    String code = CharStreams.toString(source);
    Program program = Parsing.string(code);
    TokenStream tokens = TokenStream.create(code);
    return new LayoutPreservingTransformer(program, tokens);
  }
  
  @Override
  public void remove(ASTNode<?> node) {
    tokenStream.getTokensForAstNode(node).clear();
    AstUtil.remove(node);
    tokenStream.index();
  }
  
  @Override
  public void replace(ASTNode<?> oldNode, ASTNode<?> newNode) {
    List<Token> tokens = tokenStream.getTokensForAstNode(oldNode);
    tokens.clear();
    List<Token> newTokens = tokenStream.getTokensForAstNode(newNode);
    
    tokens.addAll(newTokens);
    newTokens.get(0).setLine(oldNode.getStartLine());
    newTokens.get(0).setCharPositionInLine(oldNode.getStartColumn() - 1);
    
    AstUtil.replace(oldNode, newNode);
  }
  
  @Override
  public void insert(ASTNode<?> node, ASTNode<?> newNode, int i) {
    ASTNode<?> target = node.getChild(clamp(i, 0, node.getNumChild() - 1));
    List<Token> tokens = tokenStream.getTokensForAstNode(target);
    tokens.addAll(tokenStream.getTokensForAstNode(newNode));
    node.insertChild(newNode, i);
  }

  private static int clamp(int i, int min, int max) {
    if (i < min) {
      return i;
    }
    if (i > max) {
      return max;
    }
    return i;
  }

  public Program getProgram() {
    return program;
  }
  
  public String reconstructText() {
    return tokenStream.asString();
  }
  
  private LayoutPreservingTransformer(Program program, TokenStream tokens) {
    this.program = program;
    this.tokenStream = tokens;
  }
}
