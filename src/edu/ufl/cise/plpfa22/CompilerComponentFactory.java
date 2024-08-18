package edu.ufl.cise.plpfa22;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import edu.ufl.cise.plpfa22.IToken.Kind;
import edu.ufl.cise.plpfa22.IToken.SourceLocation;
import edu.ufl.cise.plpfa22.ast.ASTNode;
import edu.ufl.cise.plpfa22.ast.ASTVisitor;
import edu.ufl.cise.plpfa22.ast.Block;
import edu.ufl.cise.plpfa22.ast.ConstDec;
import edu.ufl.cise.plpfa22.ast.Declaration;
import edu.ufl.cise.plpfa22.ast.Expression;
import edu.ufl.cise.plpfa22.ast.ExpressionBinary;
import edu.ufl.cise.plpfa22.ast.ExpressionBooleanLit;
import edu.ufl.cise.plpfa22.ast.ExpressionIdent;
import edu.ufl.cise.plpfa22.ast.ExpressionNumLit;
import edu.ufl.cise.plpfa22.ast.ExpressionStringLit;
import edu.ufl.cise.plpfa22.ast.Ident;
import edu.ufl.cise.plpfa22.ast.ProcDec;
import edu.ufl.cise.plpfa22.ast.Program;
import edu.ufl.cise.plpfa22.ast.Statement;
import edu.ufl.cise.plpfa22.ast.StatementAssign;
import edu.ufl.cise.plpfa22.ast.StatementBlock;
import edu.ufl.cise.plpfa22.ast.StatementCall;
import edu.ufl.cise.plpfa22.ast.StatementEmpty;
import edu.ufl.cise.plpfa22.ast.StatementIf;
import edu.ufl.cise.plpfa22.ast.StatementInput;
import edu.ufl.cise.plpfa22.ast.StatementOutput;
import edu.ufl.cise.plpfa22.ast.StatementWhile;
import edu.ufl.cise.plpfa22.ast.Types;
import edu.ufl.cise.plpfa22.ast.VarDec;
import edu.ufl.cise.plpfa22.ast.Types.Type;

public class CompilerComponentFactory {

	public static class Token implements IToken {
		public final Kind kind;
		public final String text;
		public final SourceLocation sl;
		public final int index;
		public final char[] textAsArray;
		public final int length;

	public Token(Kind kind, int globalIndex, int length, int lineNum, int lineIndex, String text) {
			
			this.textAsArray = text.toCharArray();
			
			if(kind == Kind.STRING_LIT) {
				String tempString = "";
				
				for(int i = 1; i < text.length() - 1; i++) {
					if(text.charAt(i) == '\\' && i < text.length() - 2) {
						if(text.charAt(i+1) == 'b') {
							i++;
							tempString += '\b';
						}
						else if(text.charAt(i+1) == 't') {
							i++;
							tempString += '\t';
						}
						else if(text.charAt(i+1) == 'n') {
							i++;
							tempString += '\n';
						}
						else if(text.charAt(i+1) == 'f') {
							i++;
							tempString += '\f';
						}
						else if(text.charAt(i+1) == 'r') {
							i++;
							tempString += '\r';
						}
						else if(text.charAt(i+1) == '\\') {
							i++;
							tempString += '\\';
						}
						else if(text.charAt(i+1) == '\'') {
							i++;
							tempString += '\'';
						}
						else if(text.charAt(i+1) == '\"') {
							i++;
							tempString += '\"';
						}
						
					}
					else {
						tempString += text.charAt(i);
					}
				}
												
				this.text = tempString;
			}
			else
				this.text = text;
			
			
			
			if(kind == Kind.NUM_LIT) {
				Kind tempKind = Kind.NUM_LIT;
				try {
				    Integer.parseInt(text);
				} catch(NumberFormatException ex) {
					tempKind = Kind.ERROR;
				}
				finally {
					this.kind = tempKind;
				}
			}
			else
				this.kind = kind;
			
			this.index = globalIndex;
			this.sl = new SourceLocation(lineNum, lineIndex);
			this.length = length;
		}

		@Override
		public char[] getText() {
			return textAsArray;
		}

		@Override
		public Kind getKind() {
			return this.kind;
		}

		@Override
		public SourceLocation getSourceLocation() {
			return this.sl;
		}

		@Override
		public int getIntValue() {
			// If kind is NUM_LIT then text to int
			return Integer.parseInt(this.text);
		}

		@Override
		public boolean getBooleanValue() {
			// If kind is BOOLEAN_LIT then TRUE to true FALSE to false
			return this.text.equals("TRUE") ? true : false;
		}

		@Override
		public String getStringValue() {
			return this.text;
		}
	}

	public static class Lexer implements ILexer {
		private int currToken = 0;
		private ArrayList<IToken> tokens = new ArrayList<IToken>();

		public static enum Status {
			START,
			IDENT,
			NUM_LIT,
			STRING_LIT,
			COMMENT
		}

		public Lexer(String text) {

			Status state = Status.START;
			int index = 0;
			int line = 1;
			int localIndex = 1;
			int startLocalIndex = 1;
			int startIndex = 0;
			int startLine = 1;
			String temp_text = "";
			
			while (index < text.length()) {

				char ch = text.charAt(index);
				switch (state) {
					case START -> {
						startIndex = index;
						startLocalIndex = localIndex;
						startLine = line;
						switch (ch) {
							case ' ', '\t' -> {
								index++;
								localIndex++;
							}
							case '\r' -> {
								index++;
								localIndex = 1;
							}
							case '\n' -> {
								index++;
								line++;
								localIndex = 1;
							}
							case '(' -> {
								temp_text += ch;
								this.tokens.add(new Token(Kind.LPAREN, startIndex, 1, startLine, startLocalIndex, temp_text));
								temp_text = "";
								index++;
								localIndex++;
							}
							case ')' -> {
								temp_text += ch;
								this.tokens.add(new Token(Kind.RPAREN, startIndex, 1, startLine, startLocalIndex, temp_text));
								temp_text = "";
								index++;
								localIndex++;
							}
							case '+' -> {
								temp_text += ch;
								this.tokens.add(new Token(Kind.PLUS, startIndex, 1, startLine, startLocalIndex, temp_text));
								temp_text = "";
								index++;
								localIndex++;
							}
							case '-' -> {
								temp_text += ch;
								this.tokens.add(new Token(Kind.MINUS, startIndex, 1, startLine, startLocalIndex, temp_text));
								temp_text = "";
								index++;
								localIndex++;
							}
							case '/' -> {
								if(index < text.length() - 1) {
									switch (text.charAt(index + 1)) {
										case '/' -> {
											temp_text = "";
											index += 2;
											localIndex += 2;
											state = Status.COMMENT;
										}
										default -> {
											temp_text = "/";
											this.tokens
													.add(new Token(Kind.DIV, startIndex, 1, startLine, startLocalIndex, temp_text));
											temp_text = "";
											index++;
											localIndex++;
										}
									}
								}
								else {
									temp_text = "/";
									this.tokens
											.add(new Token(Kind.DIV, startIndex, 1, startLine, startLocalIndex, temp_text));
									temp_text = "";
									index++;
									localIndex++;
								}
							}
							case '*' -> {
								temp_text += ch;
								this.tokens.add(new Token(Kind.TIMES, startIndex, 1, startLine, startLocalIndex, temp_text));
								temp_text = "";
								index++;
								localIndex++;
							}
							case '<' -> {
								if(index < text.length() - 1) {
									switch (text.charAt(index + 1)) {
										case '=' -> {
											temp_text = "<=";
											this.tokens.add(new Token(Kind.LE, startIndex, 1, startLine, startLocalIndex, temp_text));
											temp_text = "";
											index += 2;
											localIndex += 2;
										}
										default -> {
											temp_text = "<";
											this.tokens.add(new Token(Kind.LT, startIndex, 1, startLine, startLocalIndex, temp_text));
											temp_text = "";
											index++;
											localIndex++;
										}
									}
								}
								else {
									temp_text = "<";
									this.tokens.add(new Token(Kind.LT, startIndex, 1, startLine, startLocalIndex, temp_text));
									temp_text = "";
									index++;
									localIndex++;
								}
							}
							case '>' -> {
								if(index < text.length() - 1) {
									switch (text.charAt(index + 1)) {
										case '=' -> {
											temp_text = ">=";
											this.tokens.add(new Token(Kind.GE, startIndex, 1, startLine, startLocalIndex, temp_text));
											temp_text = "";
											index += 2;
											localIndex += 2;
										}
										default -> {
											temp_text = ">";
											this.tokens.add(new Token(Kind.GT, startIndex, 1, startLine, startLocalIndex, temp_text));
											temp_text = "";
											index++;
											localIndex++;
										}
									}
								}
								else {
									temp_text = ">";
									this.tokens.add(new Token(Kind.GT, startIndex, 1, startLine, startLocalIndex, temp_text));
									temp_text = "";
									index++;
									localIndex++;
								}
							}
							case ';' -> {
								temp_text += ch;
								this.tokens.add(new Token(Kind.SEMI, startIndex, 1, startLine, startLocalIndex, temp_text));
								temp_text = "";
								index++;
								localIndex++;
							}
							case ',' -> {
								temp_text += ch;
								this.tokens.add(new Token(Kind.COMMA, startIndex, 1, startLine, startLocalIndex, temp_text));
								temp_text = "";
								index++;
								localIndex++;
							}
							case '.' -> {
								temp_text += ch;
								this.tokens.add(new Token(Kind.DOT, startIndex, 1, startLine, startLocalIndex, temp_text));
								temp_text = "";
								index++;
								localIndex++;
							}
							case '%' -> {
								temp_text += ch;
								this.tokens.add(new Token(Kind.MOD, startIndex, 1, startLine, startLocalIndex, temp_text));
								temp_text = "";
								index++;
								localIndex++;
							}
							case '?' -> {
								temp_text += ch;
								this.tokens.add(new Token(Kind.QUESTION, startIndex, 1, startLine, startLocalIndex, temp_text));
								temp_text = "";
								index++;
								localIndex++;
							}
							case '#' -> {
								temp_text += ch;
								this.tokens.add(new Token(Kind.NEQ, startIndex, 1, startLine, startLocalIndex, temp_text));
								temp_text = "";
								index++;
								localIndex++;
							}
							case '!' -> {
								temp_text += ch;
								this.tokens.add(new Token(Kind.BANG, startIndex, 1, startLine, startLocalIndex, temp_text));
								temp_text = "";
								index++;
								localIndex++;
							}
							case '=' -> {
								temp_text += ch;
								this.tokens.add(new Token(Kind.EQ, startIndex, 1, startLine, startLocalIndex, temp_text));
								temp_text = "";
								index++;
								localIndex++;
							}
							case ':' -> {
								if(index < text.length() - 1) {
									switch (text.charAt(index + 1)) {
										case '=' -> {
											temp_text = ":=";
											this.tokens.add(
													new Token(Kind.ASSIGN, startIndex, 1, startLine, startLocalIndex, temp_text));
											temp_text = "";
											index += 2;
											localIndex += 2;
										}
										default -> {
											temp_text += ch;
											this.tokens
													.add(new Token(Kind.ERROR, startIndex, 1, startLine, startLocalIndex, temp_text));
											temp_text = "";
											index++;
											localIndex++;
										}
									}
								}
								else {
									temp_text += ch;
									this.tokens
											.add(new Token(Kind.ERROR, startIndex, 1, startLine, startLocalIndex, temp_text));
									temp_text = "";
									index++;
									localIndex++;
								}
							}
							case '\"' -> {
								temp_text += ch;
								index++;
								localIndex++;
								state = Status.STRING_LIT;
							}
							case '0' -> {
								temp_text += ch;
								this.tokens.add(new Token(Kind.NUM_LIT, startIndex, 1, startLine, startLocalIndex, temp_text));
								temp_text = "";
								index++;
								localIndex++;
							}
							case '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
								temp_text += ch;
								index++;
								localIndex++;
								state = Status.NUM_LIT;
							}
							default -> {
								switch (ch) {
									case 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p',
											'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E',
											'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
											'U', 'V', 'W', 'X', 'Y', 'Z', '$', '_' -> {
										temp_text += ch;
										index++;
										localIndex++;
										state = Status.IDENT;
									}
									default -> {
										temp_text += ch;
										this.tokens
												.add(new Token(Kind.ERROR, startIndex, 1, startLine, startLocalIndex, temp_text));
										temp_text = "";
										index++;
										localIndex++;
									}
								}
							}
						}
					}
					case IDENT -> {
						switch (ch) {
							case 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q',
									'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
									'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y',
									'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '$', '_' -> {
								
								temp_text += ch;
								if(index < text.length() - 1 && (text.charAt(index + 1) == 'a' || text.charAt(index + 1) == 'b' || text.charAt(index + 1) == 'c' || 
										text.charAt(index + 1) == 'd' || text.charAt(index + 1) == 'e' || text.charAt(index + 1) == 'f' || text.charAt(index + 1) == 'g' || 
										text.charAt(index + 1) == 'h' || text.charAt(index + 1) == 'i' || text.charAt(index + 1) == 'j' || text.charAt(index + 1) == 'k' || 
										text.charAt(index + 1) == 'l' || text.charAt(index + 1) == 'm' || text.charAt(index + 1) == 'n' || text.charAt(index + 1) == 'o' || 
										text.charAt(index + 1) == 'p' || text.charAt(index + 1) == 'q' || text.charAt(index + 1) == 'r' || text.charAt(index + 1) == 's' || 
										text.charAt(index + 1) == 't' || text.charAt(index + 1) == 'u' || text.charAt(index + 1) == 'v' || text.charAt(index + 1) == 'w' || 
										text.charAt(index + 1) == 'x' || text.charAt(index + 1) == 'y' || text.charAt(index + 1) == 'z' || text.charAt(index + 1) == 'A' || 
										text.charAt(index + 1) == 'B' || text.charAt(index + 1) == 'C' || text.charAt(index + 1) == 'D' || text.charAt(index + 1) == 'E' || 
										text.charAt(index + 1) == 'F' || text.charAt(index + 1) == 'G' || text.charAt(index + 1) == 'H' || text.charAt(index + 1) == 'I' || 
										text.charAt(index + 1) == 'J' || text.charAt(index + 1) == 'K' || text.charAt(index + 1) == 'L' || text.charAt(index + 1) == 'M' || 
										text.charAt(index + 1) == 'N' || text.charAt(index + 1) == 'O' || text.charAt(index + 1) == 'P' || text.charAt(index + 1) == 'Q' || 
										text.charAt(index + 1) == 'R' || text.charAt(index + 1) == 'S' || text.charAt(index + 1) == 'T' || text.charAt(index + 1) == 'U' || 
										text.charAt(index + 1) == 'V' || text.charAt(index + 1) == 'W' || text.charAt(index + 1) == 'X' || text.charAt(index + 1) == 'Y' || 
										text.charAt(index + 1) == 'Z' || text.charAt(index + 1) == '0' || text.charAt(index + 1) == '1' || text.charAt(index + 1) == '2' || 
										text.charAt(index + 1) == '3' || text.charAt(index + 1) == '4' || text.charAt(index + 1) == '5' || text.charAt(index + 1) == '6' || 
										text.charAt(index + 1) == '7' || text.charAt(index + 1) == '8' || text.charAt(index + 1) == '9' || text.charAt(index + 1) == '$' || 
										text.charAt(index + 1) == '_')) {
									index++;
									localIndex++;
								}
								else if (temp_text.equals("VAR")) {
									this.tokens.add(new Token(Token.Kind.KW_VAR, startIndex, index - startIndex, startLine,
											startLocalIndex, temp_text));
									temp_text = "";
									index++;
									localIndex++;
									state = Status.START;
								} else if (temp_text.equals("CONST")) {
									this.tokens.add(new Token(Token.Kind.KW_CONST, startIndex, index - startIndex, startLine,
											startLocalIndex, temp_text));
									temp_text = "";
									index++;
									localIndex++;
									state = Status.START;
								} else if (temp_text.equals("PROCEDURE")) {
									this.tokens.add(new Token(Token.Kind.KW_PROCEDURE, startIndex, index - startIndex,
											startLine, startLocalIndex, temp_text));
									temp_text = "";
									index++;
									localIndex++;
									state = Status.START;
								} else if (temp_text.equals("CALL")) {
									this.tokens.add(new Token(Token.Kind.KW_CALL, startIndex, index - startIndex, startLine,
											startLocalIndex, temp_text));
									temp_text = "";
									index++;
									localIndex++;
									state = Status.START;
								} else if (temp_text.equals("BEGIN")) {
									this.tokens.add(new Token(Token.Kind.KW_BEGIN, startIndex, index - startIndex, startLine,
											startLocalIndex, temp_text));
									temp_text = "";
									index++;
									localIndex++;
									state = Status.START;
								} else if (temp_text.equals("END")) {
									this.tokens.add(new Token(Token.Kind.KW_END, startIndex, index - startIndex, startLine,
											startLocalIndex, temp_text));
									temp_text = "";
									index++;
									localIndex++;
									state = Status.START;
								} else if (temp_text.equals("DO")) {
									this.tokens.add(new Token(Token.Kind.KW_DO, startIndex, index - startIndex, startLine,
											startLocalIndex, temp_text));
									temp_text = "";
									index++;
									localIndex++;
									state = Status.START;
								} else if (temp_text.equals("IF")) {
									this.tokens.add(new Token(Token.Kind.KW_IF, startIndex, index - startIndex, startLine,
											startLocalIndex, temp_text));
									temp_text = "";
									index++;
									localIndex++;
									state = Status.START;
								} else if (temp_text.equals("WHILE")) {
									this.tokens.add(new Token(Token.Kind.KW_WHILE, startIndex, index - startIndex, startLine,
											startLocalIndex, temp_text));
									temp_text = "";
									index++;
									localIndex++;
									state = Status.START;
								} else if (temp_text.equals("THEN")) {
									this.tokens.add(new Token(Token.Kind.KW_THEN, startIndex, index - startIndex, startLine,
											startLocalIndex, temp_text));
									temp_text = "";
									index++;
									localIndex++;
									state = Status.START;
								} else if (temp_text.equals("TRUE")) {
									this.tokens.add(new Token(Kind.BOOLEAN_LIT, startIndex, index - startIndex, startLine,
											startLocalIndex, temp_text));
									temp_text = "";
									index++;
									localIndex++;
									state = Status.START;
								} else if (temp_text.equals("FALSE")) {
									this.tokens.add(new Token(Kind.BOOLEAN_LIT, startIndex, index - startIndex, startLine,
											startLocalIndex, temp_text));
									temp_text = "";
									index++;
									localIndex++;
									state = Status.START;
								} else {
									index++;
									localIndex++;
								}
							}
							default -> {
								this.tokens.add(new Token(Kind.IDENT, startIndex, index - startIndex, startLine, startLocalIndex,
										temp_text));
								temp_text = "";
								state = Status.START;
							}
						}
					}
					case COMMENT -> {
						switch (ch) {
							case '\r' -> {
								index++;
								localIndex = 1;
							}
							case '\n' -> {
								index++;
								line++;
								localIndex = 1;
								state = Status.START;
							}
							default -> {
								index++;
								localIndex++;
							}
						}
					}
					case NUM_LIT -> {
						switch (ch) {
							case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
								temp_text += ch;
								index++;
								localIndex++;
							}
							default -> {
								this.tokens.add(new Token(Kind.NUM_LIT, startIndex, index - startIndex, startLine,
										startLocalIndex, temp_text));
								temp_text = "";
								state = Status.START;
							}
						}
					}
					case STRING_LIT -> {
						switch (ch) {
							case '\\' -> {
								switch (text.charAt(index + 1)) {
									case 'b' -> {
										temp_text += '\\';
										temp_text += 'b';
										index += 2;
										localIndex += 2;
									}
									case 't' -> {
										temp_text += '\\';
										temp_text += 't';
										index += 2;
										localIndex += 2;
									}
									case 'n' -> {
										temp_text += '\\';
										temp_text += 'n';
										index += 2;
									}
									case 'f' -> {
										temp_text += '\\';
										temp_text += 'f';
										index += 2;
										localIndex += 2;
									}
									case 'r' -> {
										temp_text += '\\';
										temp_text += 'r';
										index += 2;
									}
									case '\"' -> {
										temp_text += '\\';
										temp_text += '\"';
										index += 2;
										localIndex += 2;
									}
									case '\'' -> {
										temp_text += '\\';
										temp_text += '\'';
										index += 2;
										localIndex += 2;
									}
									case '\\' -> {
										temp_text += '\\';
										temp_text += '\\';
										index += 2;
										localIndex += 2;
									}
									default -> {
										temp_text += ch;
										this.tokens.add(new Token(Kind.ERROR, startIndex, index - startIndex, startLine,
												startLocalIndex, temp_text));
										temp_text = "";
										index++;
										localIndex++;
										state = Status.START;
									}
								}
							}
							case '\r' -> {
								temp_text += ch;
								index++;
								localIndex = 1;
							}
							case '\n' -> {
								temp_text += ch;
								index++;
								line++;
								localIndex = 1;
							}
							case '\"' -> {
								temp_text += ch;
								this.tokens.add(new Token(Token.Kind.STRING_LIT, startIndex, index - startIndex, startLine,
										startLocalIndex, temp_text));
								temp_text = "";
								index++;
								localIndex++;
								state = Status.START;
							}
							default -> {
								temp_text += ch;
								index++;
								localIndex++;
							}
						}
					}
					default -> throw new IllegalStateException("lexer bug");
				}
			}

			switch (state) {
				case IDENT -> {
					this.tokens.add(new Token(Kind.IDENT, startIndex, index - startIndex, startLine, startLocalIndex, temp_text));
					this.tokens.add(new Token(Kind.EOF, index + 1, index + 1, startLine, startLocalIndex + 1, "\0"));
				}
				case NUM_LIT -> {
					this.tokens
							.add(new Token(Kind.NUM_LIT, startIndex, index - startIndex, startLine, startLocalIndex, temp_text));
					this.tokens.add(new Token(Kind.EOF, index + 1, index + 1, startLine, startLocalIndex + 1, "\0"));
				}
				case STRING_LIT -> {
					this.tokens.add(new Token(Kind.ERROR, startIndex, index - startIndex, startLine, startLocalIndex, temp_text));
					this.tokens.add(new Token(Kind.EOF, index + 1, index + 1, startLine, startLocalIndex + 1, "\0"));
				}
				default -> {
					this.tokens.add(new Token(Kind.EOF, index + 1, index + 1, startLine, startLocalIndex + 1, "\0"));
				}
			}
		}

		@Override
		public IToken next() throws LexicalException {
			if (this.tokens.get(this.currToken).getKind() == Kind.ERROR) {
				currToken++;
				throw new LexicalException();
			}
			else if (this.tokens.get(this.currToken).getKind() == Kind.EOF)
				return this.tokens.get(this.currToken);
			else
				return this.tokens.get(this.currToken++);
		}

		@Override
		public IToken peek() throws LexicalException {
			if (this.tokens.get(this.currToken).getKind() == Kind.ERROR)
				throw new LexicalException();
			else
				return this.tokens.get(this.currToken);
		}
	}

	public static ILexer getLexer(String input) {
		return new Lexer(input);
	}

	public static class Parser implements IParser{
		ILexer lexer;
		IToken token;
		
		Parser(ILexer lexer) throws LexicalException{
			this.lexer = lexer;
			this.token = this.lexer.next();
		}
		
		public void match(Kind kind) throws SyntaxException, LexicalException{
			SourceLocation source = this.token.getSourceLocation();
			if(this.token.getKind() == kind) {
				consume();
				return;
			}
			throw new SyntaxException("Got this " + this.token.getKind() + " but wanted this " + kind, source.line(), source.column());
		}
		
		//Finish consume() method
		public void consume() throws LexicalException {
			this.token = this.lexer.next();
		}
		
		@Override
		public ASTNode parse() throws PLPException {

			while (token.getKind() != Kind.EOF) {
				return program();
			}
			return null;
		}

		private Program program() throws PLPException {
			IToken first = this.token;
			if(this.token.getKind() != Kind.EOF){
				Block block = block();
				match(Kind.DOT);
				if(this.token.getKind() == Kind.EOF)
					return new Program(first, block);
				else{
					SourceLocation source = this.token.getSourceLocation();
					throw new SyntaxException("Code after a program", source.line(), source.column());
				}
			}
			else{
				SourceLocation source = this.token.getSourceLocation();
				throw new SyntaxException("Empty", source.line(), source.column());
			}
		}

		private Block block() throws SyntaxException, LexicalException{
			IToken first = this.token;
			int step = -1;
			Boolean inList = false;
			List<ConstDec> constDecs = new ArrayList<>();
			List<VarDec> varDecs = new ArrayList<>();
			List<ProcDec> procDecs = new ArrayList<>();
			Statement statement = null;
			while(this.token.getKind() != Kind.EOF){
				if(inList){
					if (this.token.getKind() == Kind.COMMA){
						match(Kind.COMMA);

						if(step == 0){
							constDecs.add(constDec());
						}
						else if(step == 1){
							varDecs.add(varDec());
						}
						else{
							SourceLocation source = this.token.getSourceLocation();
							throw new SyntaxException("Unexpected comma", source.line(), source.column());
						}

						if(this.token.getKind() == Kind.SEMI){
							match(Kind.SEMI);
							inList = false;
						}

					}
					else{
						SourceLocation source = this.token.getSourceLocation();
						throw new SyntaxException("Unexpected Token during declarations", source.line(), source.column());
					}
				}
				else if(this.token.getKind() == Kind.KW_CONST){
					if(step > 0){
						SourceLocation source = this.token.getSourceLocation();
						throw new SyntaxException("Consts mut be defined at the beginning of a block", source.line(), source.column());
					}

					match(Kind.KW_CONST);
					constDecs.add(constDec());
					step = 0;
					if(this.token.getKind() == Kind.COMMA)
						inList = true;
					else if(this.token.getKind() == Kind.SEMI)
						match(Kind.SEMI);

				}
				else if(this.token.getKind() == Kind.KW_VAR){

					if(step > 1){
						SourceLocation source = this.token.getSourceLocation();
						throw new SyntaxException("Vars must be defined before anything this is not a const", source.line(), source.column());
					}

					match(Kind.KW_VAR);
					varDecs.add(varDec());
					step = 1;
					if(this.token.getKind() == Kind.COMMA)
						inList = true;
					else if(this.token.getKind() == Kind.SEMI)
						match(Kind.SEMI);

				}
				else if(this.token.getKind() == Kind.KW_PROCEDURE){

					if(step > 2){
						SourceLocation source = this.token.getSourceLocation();
						throw new SyntaxException("Procedures must be defined before anything this is not a const or var", source.line(), source.column());
					}

					match(Kind.KW_PROCEDURE);
					procDecs.add(procDec());
					step = 2;
					
				}
				else if (this.token.getKind() == Kind.COMMA){
					SourceLocation source = this.token.getSourceLocation();
					throw new SyntaxException("Procedures must be defined before anything this is not a const or var", source.line(), source.column());
				}
				else if (this.token.getKind() == Kind.DOT || this.token.getKind() == Kind.SEMI){
					if(step == 4){
						break;
					}
					
					statement = emptyStatement();
					break;
				}
				else {
					if(step > 3){
						SourceLocation source = this.token.getSourceLocation();
						throw new SyntaxException("Only one statement is allowed", source.line(), source.column());
					}
					step = 4;
					statement = statement();
				}
			}
			if(statement == null){
				SourceLocation source = first.getSourceLocation();
				throw new SyntaxException("No statement", source.line(), source.column());
			}
			return new Block(first, constDecs, varDecs, procDecs, statement);
		}

		private ConstDec constDec() throws SyntaxException, LexicalException {
			IToken first = this.token;
			IToken iden = this.token;
			match(Kind.IDENT);
			match(Kind.EQ);
			IToken val = this.token;
			consume();
			if(val.getKind() == Kind.BOOLEAN_LIT){
				return new ConstDec(first, iden, val.getBooleanValue());
			}
			else if(val.getKind() == Kind.NUM_LIT){
				return new ConstDec(first, iden, val.getIntValue());
			}
			else if(val.getKind() == Kind.STRING_LIT){
				return new ConstDec(first, iden, val.getStringValue());
			}
			else{
				SourceLocation source = first.getSourceLocation();
				throw new SyntaxException("No statement", source.line(), source.column());
			}
		}

		private VarDec varDec() throws SyntaxException, LexicalException {
			IToken first = this.token;
			IToken iden = this.token;
			match(Kind.IDENT);
			return new VarDec(first, iden);
		}

		private ProcDec procDec() throws SyntaxException, LexicalException {
			IToken first = this.token;
			IToken iden = this.token;
			match(Kind.IDENT);
			match(Kind.SEMI);
			Block block = block();
			match(Kind.SEMI);
			return new ProcDec(first, iden, block);
		}

		private Statement emptyStatement(){
			return new StatementEmpty(this.token);
		}

		private Statement statement() throws SyntaxException, LexicalException{
			IToken first = this.token;
			if(this.token.getKind() == Kind.IDENT){
				Ident ident = ident();
				match(Kind.ASSIGN);
				Expression expression = expression();
				return new StatementAssign(first, ident, expression);
			}
			else if(this.token.getKind() == Kind.KW_CALL){
				match(Kind.KW_CALL);
				Ident ident = ident();
				return new StatementCall(first, ident);
			}
			else if(this.token.getKind() == Kind.QUESTION){
				match(Kind.QUESTION);
				Ident ident = ident();
				return new StatementInput(first, ident);
			}
			else if(this.token.getKind() == Kind.BANG){
				match(Kind.BANG);
				Expression expression = expression();
				return new StatementOutput(first, expression);
			}
			else if(this.token.getKind() == Kind.KW_BEGIN){
				List<Statement> statements = new ArrayList<>();
				match(Kind.KW_BEGIN);
				int i = 0;
				do{
					if(i > 0)
						match(Kind.SEMI);
			
					if(this.token.getKind() == Kind.SEMI || this.token.getKind() == Kind.KW_END){
						statements.add(emptyStatement());
					}
					else{
						statements.add(statement());
					}
				
					i++;
				} while(this.token.getKind() == Kind.SEMI);
				match(Kind.KW_END);
				return new StatementBlock(first, statements);
			}
			else if(this.token.getKind() == Kind.KW_IF){
				match(Kind.KW_IF);
				Expression expression = expression();
				match(Kind.KW_THEN);
				Statement statement;
				if(this.token.getKind() == Kind.DOT){
					statement = emptyStatement();
				}
				else{
					statement = statement();
				}
				return new StatementIf(first, expression, statement);
			}
			else if(this.token.getKind() == Kind.KW_WHILE){
				match(Kind.KW_WHILE);
				Expression expression = expression();
				match(Kind.KW_DO);
				Statement statement;
				if(this.token.getKind() == Kind.DOT){
					statement = emptyStatement();
				}
				else{
					statement = statement();
				}
				return new StatementWhile(first, expression, statement);
			}
			else {
				SourceLocation source = this.token.getSourceLocation();
				throw new SyntaxException("Not a valid statement", source.line(), source.column());
			}
		}

		private Expression litExpr() throws SyntaxException, LexicalException {
			IToken first = this.token;
			if(this.token.getKind() == Kind.BOOLEAN_LIT){
				match(Kind.BOOLEAN_LIT);
				return new ExpressionBooleanLit(first);
			}
			else if(this.token.getKind() == Kind.STRING_LIT){
				match(Kind.STRING_LIT);
				return new ExpressionStringLit(first);
			}
			else if(this.token.getKind() == Kind.NUM_LIT){
				match(Kind.NUM_LIT);
				return new ExpressionNumLit(first);
			}
			else{
				SourceLocation source = this.token.getSourceLocation();
				throw new SyntaxException("Invalid literal type", source.line(), source.column());
			}
		}

		private Ident ident() throws SyntaxException, LexicalException{
			IToken first = this.token;
			match(Kind.IDENT);
			return new Ident(first);
		}

		private Expression expression() throws SyntaxException, LexicalException{
			IToken first = this.token;
			Expression e0 = addExpression();
			Expression e1 = null;
			Expression temp = e0;
			
			while(this.token.getKind() == Kind.LT || this.token.getKind() == Kind.GT || this.token.getKind() == Kind.EQ
			|| this.token.getKind() == Kind.NEQ || this.token.getKind() == Kind.LE || this.token.getKind() == Kind.GE){
				IToken op = this.token;
				consume();
				e1 = addExpression();
				temp = new ExpressionBinary(first, temp, op, e1);
			}

			return temp;
		}

		private Expression addExpression() throws SyntaxException, LexicalException{
			IToken first = this.token;
			Expression e0 = multExpression();
			Expression e1 = null;
			Expression temp = e0;
			
			while(this.token.getKind() == Kind.PLUS || this.token.getKind() == Kind.MINUS){
				IToken op = this.token;
				consume();
				e1 = multExpression();
				temp = new ExpressionBinary(first, temp, op, e1);
			}

			return temp;
		}

		private Expression multExpression() throws SyntaxException, LexicalException{
			IToken first = this.token;
			Expression e0 = primaryExpression();
			Expression e1 = null;
			Expression temp = e0;
			
			while(this.token.getKind() == Kind.TIMES || this.token.getKind() == Kind.DIV || this.token.getKind() == Kind.MOD){
				IToken op = this.token;
				consume();
				e1 = primaryExpression();
				temp = new ExpressionBinary(first, temp, op, e1);
			}

			return temp;
		}

		private Expression primaryExpression() throws SyntaxException, LexicalException{
			IToken first = this.token;
			if(this.token.getKind() == Kind.IDENT){
				match(Kind.IDENT);
				return new ExpressionIdent(first);
			}
			else if(this.token.getKind() == Kind.LPAREN){
				match(Kind.LPAREN);
				Expression expression = expression();
				match(Kind.RPAREN);
				return expression;
			}
			else if(this.token.getKind() == Kind.NUM_LIT || this.token.getKind() == Kind.STRING_LIT || this.token.getKind() == Kind.BOOLEAN_LIT){
				return litExpr();
			}
			else{
				SourceLocation source = this.token.getSourceLocation();
				throw new SyntaxException("Invalid literal type", source.line(), source.column());
			}
		}
	}

    public static IParser getParser(ILexer lexer) throws LexicalException {
        return new Parser(lexer);
    }
      
    public static class Visitor implements ASTVisitor{
    	
    	 public class SymbolTable {
    			private Stack<Integer> stk = new Stack<Integer>();
    			private HashMap<String, HashMap<Integer, List<Object>>> table = new HashMap<String, HashMap<Integer, List<Object>>>();
    			private int curr_level, next_level, curr_scope, next_scope;

    			public SymbolTable(){
    				curr_scope = 0;
    				next_scope = 0;
    				curr_level = 0;
    				next_level = 0;
    				stk.push(curr_scope);
    			}

    			public void enterScope(){ 
    				curr_scope = next_scope;
    				next_scope++;
    				curr_level = next_level;
    				next_level++;
    				stk.push(curr_scope);
    			}
    			
    			public void resetScopes(){ 
    				curr_scope = 0;
    				next_scope = 0;
    				curr_level = 0;
    				next_level = 0;
    				stk.push(curr_scope);
    			}

    			public void leaveScope(){ 
    				next_level = curr_level;
    				curr_level--;
    				stk.pop(); 
    				if(!stk.empty()){
    					curr_scope = stk.peek();
    				}
    			}

    			public boolean insert(String ident, Declaration dec) {
    				if(table.containsKey(ident)){
    					if(table.get(ident).containsKey(curr_scope)){
    						return false;
    					}
    					else{
    						Object[] array = {curr_level, curr_scope, dec};
    						table.get(ident).put(curr_scope, Arrays.asList(array));
    					}
    				}
    				else{
    					Object[] array = {curr_level, curr_scope, dec};
    					HashMap<Integer, List<Object>> temp = new HashMap<Integer, List<Object>>();
    					temp.put(curr_scope, Arrays.asList(array));
    					table.put(ident, temp);
    				}
    		
    				return true;
    			}

    			public List<Object> lookup(String ident) {
    				List<Object> temp = null;
    				if(table.containsKey(ident)){
    					for (int x: stk) {
    						if(table.get(ident).containsKey(x)){
    							temp = table.get(ident).get(x);
    							temp.set(0, curr_level);
    						}
    					}
    				}
    				return temp;
    			}
    		}
	 
    	SymbolTable table;
    	
    	public Visitor() {
    		table = new SymbolTable();
    	}
		@Override
		public Object visitBlock(Block block, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			table.enterScope();
			List<ConstDec> constDecs = block.constDecs;
			List<VarDec> varDecs = block.varDecs;
			List<ProcDec> procedureDecs = block.procedureDecs;
			Statement states = block.statement;
			for(ConstDec cDec: constDecs) {
				cDec.visit(this, arg);
			}
			
			for(VarDec vDec: varDecs) {
				vDec.visit(this, arg);
			}
			
			for(ProcDec pDec: procedureDecs) {
				pDec.visit(this, arg);
			}
			
			states.visit(this, arg);
			
			table.leaveScope();
			return null;
		}

		@Override
		public Object visitProgram(Program program, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			//true = procedure walk, otherwise false
			program.block.visit(this, true);
			table.resetScopes();
			program.block.visit(this, false);
			return null;
		}

		@Override
		public Object visitStatementAssign(StatementAssign statementAssign, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			statementAssign.ident.visit(this, arg);
			statementAssign.expression.visit(this, arg);
			return null;
		}

		@Override
		public Object visitVarDec(VarDec varDec, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			if(!(boolean) arg) {
				if(!table.insert(String.valueOf(varDec.ident.getText()), varDec)) {
					throw new ScopeException("No insert detected.");
				}
				List<Object> vals = table.lookup(String.valueOf(varDec.ident.getText()));
				if(vals == null) {
					throw new ScopeException("Declaration is null");
				}
				int level = (int) vals.get(0);
				varDec.setNest(level);
			}
			return null;
		}

		@Override
		public Object visitStatementCall(StatementCall statementCall, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			statementCall.ident.visit(this, arg);
			return null;
		}

		@Override
		public Object visitStatementInput(StatementInput statementInput, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			statementInput.ident.visit(this, arg);
			return null;
		}

		@Override
		public Object visitStatementOutput(StatementOutput statementOutput, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			statementOutput.expression.visit(this, arg);
			return null;
		}

		@Override
		public Object visitStatementBlock(StatementBlock statementBlock, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			List<Statement> statements = statementBlock.statements;
			for(Statement state: statements) {
				state.visit(this, arg);
			}
			return null;
		}

		@Override
		public Object visitStatementIf(StatementIf statementIf, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			statementIf.expression.visit(this, arg);
			statementIf.statement.visit(this, arg);
			return null;
		}

		@Override
		public Object visitStatementWhile(StatementWhile statementWhile, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			statementWhile.expression.visit(this, arg);
			statementWhile.statement.visit(this, arg);
			return null;
		}

		@Override
		public Object visitExpressionBinary(ExpressionBinary expressionBinary, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			expressionBinary.e0.visit(this, arg);
			expressionBinary.e1.visit(this, arg);
			return null;
		}

		@Override
		public Object visitExpressionIdent(ExpressionIdent expressionIdent, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			if(!(boolean) arg) {
				List<Object> vals = table.lookup(String.valueOf(expressionIdent.firstToken.getText()));
				if(vals == null) {
					throw new ScopeException("Declaration is null");
				}
				Declaration dec = (Declaration) vals.get(2);
				int level = (int) vals.get(0);
				//expressionIdent.getDec().visit(this, arg);
				expressionIdent.setDec(dec);
				expressionIdent.setNest(level);
			}
			return null;
		}

		@Override
		public Object visitExpressionNumLit(ExpressionNumLit expressionNumLit, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object visitExpressionStringLit(ExpressionStringLit expressionStringLit, Object arg)
				throws PLPException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object visitExpressionBooleanLit(ExpressionBooleanLit expressionBooleanLit, Object arg)
				throws PLPException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object visitProcedure(ProcDec procDec, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			if((boolean) arg) {
				if(!table.insert(String.valueOf(procDec.ident.getText()), procDec)) {
					throw new ScopeException("No insert detected.");
				}
				List<Object> vals = table.lookup(String.valueOf(procDec.ident.getText()));
				if(vals == null) {
					throw new ScopeException("Declaration is null");
				}
				int level = (int) vals.get(0);
				procDec.setNest(level);
			}
			procDec.block.visit(this, arg);
			return null;
		}

		@Override
		public Object visitConstDec(ConstDec constDec, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			if(!(boolean) arg) {
				if(!table.insert(String.valueOf(constDec.ident.getText()), constDec)) {
					throw new ScopeException("No insert detected.");
				}
				List<Object> vals = table.lookup(String.valueOf(constDec.ident.getText()));
				if(vals == null) {
					throw new ScopeException("Declaration is null");
				}
				int level = (int) vals.get(0);
				constDec.setNest(level);
			}
			return null;
		}

		@Override
		public Object visitStatementEmpty(StatementEmpty statementEmpty, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object visitIdent(Ident ident, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			if(!(boolean) arg) {
				List<Object> vals = table.lookup(String.valueOf(ident.firstToken.getText()));
				if(vals == null) {
					throw new ScopeException("Declaration is null");
				}
				Declaration dec = (Declaration) vals.get(2);
				int level = (int) vals.get(0);
				ident.setDec(dec);
				ident.setNest(level);
			}
			return null;
		}
    }

	public static class TypeCheckVisitor implements ASTVisitor{
    	
		Boolean changed;

		public TypeCheckVisitor() {
			changed = false;
		}

		
		@Override
		public Object visitProgram(Program program, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			//true = procedure walk, otherwise false
			Boolean valid = false;
			do{
				changed = false;
				Object r1 = program.block.visit(this, false);
				valid = (Boolean) r1;
			} while(changed && !valid);

			if(!valid){
				throw new TypeCheckException("Error");
			}

			return true;
		}

		@Override
		public Object visitBlock(Block block, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			Boolean status = true;
			List<ConstDec> constDecs = block.constDecs;
			List<VarDec> varDecs = block.varDecs;
			List<ProcDec> procedureDecs = block.procedureDecs;
			Statement states = block.statement;
			for(ConstDec cDec: constDecs) {
				Object r1 = cDec.visit(this, arg);	
				status = status && (Boolean) r1;
			}
			
			for(VarDec vDec: varDecs) {
				Object r1 = vDec.visit(this, arg);	
				status = status && (Boolean) r1;
			}
			
			for(ProcDec pDec: procedureDecs) {
				Object r1 = pDec.visit(this, arg);	
				status = status && (Boolean) r1;
			}
			
			Object r1 = states.visit(this, arg);	
			status = status && (Boolean) r1;
			
			return status;
		}

		@Override
		public Object visitStatementAssign(StatementAssign statementAssign, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			Boolean status = true;
			Object r1 = statementAssign.ident.visit(this, arg);	
			Object r2 = statementAssign.expression.visit(this, arg);
			status = status && (Boolean) r1;
			status = status && (Boolean) r2;

			Declaration leftDec = statementAssign.ident.getDec();
			
			if(leftDec instanceof ConstDec){
				status = status && false;
			}
			else{
				if(leftDec.getType() == null && statementAssign.expression.getType() == null){
					status = status && false;
				}
				else if(leftDec.getType() != null && statementAssign.expression.getType() != null){
					if((leftDec.getType() == Type.BOOLEAN || leftDec.getType() == Type.NUMBER || leftDec.getType() == Type.STRING) && (statementAssign.expression.getType() == Type.BOOLEAN || statementAssign.expression.getType() == Type.NUMBER || statementAssign.expression.getType() == Type.STRING))
						status = status && (leftDec.getType() == statementAssign.expression.getType());
					else
						status = status && false;
				}
				else if(statementAssign.expression.getType() != null){
					leftDec.setType(statementAssign.expression.getType());
					changed = true;
				}
				else if(leftDec.getType() != null){
					statementAssign.expression.setType(leftDec.getType());
					changed = true;
				}
			}
			
			return status;
		}

		@Override
		public Object visitVarDec(VarDec varDec, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public Object visitStatementCall(StatementCall statementCall, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			Boolean status = true;
			Object r1 = statementCall.ident.visit(this, arg);	
			status = status && (Boolean) r1;
			
			Declaration dec = statementCall.ident.getDec();
			status = status && (dec.getType() == Type.PROCEDURE);
			
			return status;
		}

		@Override
		public Object visitStatementInput(StatementInput statementInput, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			Boolean status = true;
			Object r1 = statementInput.ident.visit(this, arg);	
			status = status && (Boolean) r1;
			Declaration dec = statementInput.ident.getDec();
			
			if(dec instanceof ConstDec){
				status = status && false;
			}
			else{
				status = status && (dec.getType() == Type.NUMBER || dec.getType() == Type.STRING ||  dec.getType() == Type.BOOLEAN);
			}
			return status;
		}

		@Override
		public Object visitStatementOutput(StatementOutput statementOutput, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			Boolean status = true;
			Object r1 = statementOutput.expression.visit(this, arg);	
			status = status && (Boolean) r1;

			Type exprType = statementOutput.expression.getType();
			status = status && (exprType == Type.NUMBER || exprType == Type.STRING || exprType == Type.BOOLEAN);
				
			return status;
		}

		@Override
		public Object visitStatementBlock(StatementBlock statementBlock, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			Boolean status = true;
			List<Statement> statements = statementBlock.statements;
			for(Statement state: statements) {
				Object r1 = state.visit(this, arg);	
				status = status && (Boolean) r1;
			}

			return status;
		}

		@Override
		public Object visitStatementIf(StatementIf statementIf, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			Boolean status = true;
			Object r1 = statementIf.expression.visit(this, arg);	
			Object r2 = statementIf.statement.visit(this, arg);
			status = status && (Boolean) r1;
			status = status && (Boolean) r2;
			
			Type exprType = statementIf.expression.getType();
			status = status && (exprType == Type.BOOLEAN);
				
			return status;
		}

		@Override
		public Object visitStatementWhile(StatementWhile statementWhile, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			Boolean status = true;
			Object r1 = statementWhile.expression.visit(this, arg);	
			Object r2 = statementWhile.statement.visit(this, arg);
			status = status && (Boolean) r1;
			status = status && (Boolean) r2;
			
			Type exprType = statementWhile.expression.getType();
			status = status && (exprType == Type.BOOLEAN);

			return status;
		}

		@Override
		public Object visitExpressionBinary(ExpressionBinary expressionBinary, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			Boolean status = true;
			Object r1 = expressionBinary.e0.visit(this, arg);	
			Object r2 = expressionBinary.e1.visit(this, arg);
			status = status && (Boolean) r1;
			status = status && (Boolean) r2;
			Type e0t = expressionBinary.e0.getType();	
			Type e1t = expressionBinary.e1.getType();

			if(expressionBinary.op.getKind() == Kind.PLUS){
				if(e0t == null && e1t == null){
					Type expType = expressionBinary.getType();
					if(expType == Type.NUMBER || expType == Type.STRING || expType == Type.BOOLEAN){
						expressionBinary.e0.setType(expType);
						expressionBinary.e1.setType(expType);
						changed = true;
					}
					else{
						status = status && false;
					}
				}
				else if(e0t == null){
					if(e1t == Type.NUMBER || e1t == Type.STRING || e1t == Type.BOOLEAN){
						expressionBinary.e0.setType(e1t);
						expressionBinary.setType(e1t);
						status = status && true;
						changed = true;
					}
					else{
						status = status && false;
					}
				}
				else if(e1t == null){
					if(e0t == Type.NUMBER || e0t == Type.STRING || e0t == Type.BOOLEAN){
						expressionBinary.e1.setType(e0t);
						expressionBinary.setType(e0t);
						changed = true;
						status = status && true;
					}
					else{
						status = status && false;
					}
				}
				else{
					if((e0t == Type.NUMBER || e0t == Type.STRING || e0t == Type.BOOLEAN) && (e1t == Type.NUMBER || e1t == Type.STRING || e1t == Type.BOOLEAN)){
						if(e0t == e1t){
							if(expressionBinary.getType() == null){
								changed = true;
							}
							expressionBinary.setType(e0t);
							status = status && true;
						}
						else{
							status = status && false;
						}
					}
				}
			}
			else if(expressionBinary.op.getKind() == Kind.MINUS || expressionBinary.op.getKind() == Kind.DIV || expressionBinary.op.getKind() == Kind.MOD){
				if(e0t == null && e1t == null){
					if(expressionBinary.getType() != Type.NUMBER){
						status = status && false;
					}
					else{
						expressionBinary.e0.setType(Type.NUMBER);
						expressionBinary.e1.setType(Type.NUMBER);
						expressionBinary.setType(Type.NUMBER);
						changed = true;
						status = status && true;
					}
				}
				else if(e0t == null){
					if(e1t == Type.NUMBER){
						expressionBinary.e0.setType(e1t);
						expressionBinary.setType(e1t);
						changed = true;
						status = status && true;
					}
					else{
						status = status && false;
					}
				}
				else if(e1t == null){
					if(e0t == Type.NUMBER){
						expressionBinary.e1.setType(e0t);
						expressionBinary.setType(e0t);
						changed = true;
						status = status && true;
					}
					else{
						status = status && false;
					}
				}
				else{
					if((e0t == Type.NUMBER) && (e1t == Type.NUMBER)){
						if(e0t == e1t){
							if(expressionBinary.getType() == null){
								changed = true;
							}
							expressionBinary.setType(e0t);
							status = status && true;
						}
						else{
							status = status && false;
						}
					}
				}
			}
			else if(expressionBinary.op.getKind() == Kind.TIMES){
				if(e0t == null && e1t == null){
					Type expType = expressionBinary.getType();
					if(expType == Type.NUMBER || expType == Type.BOOLEAN){
						expressionBinary.e0.setType(expType);
						expressionBinary.e1.setType(expType);
						changed = true;
					}
					else{
						status = status && false;
					}
				}
				else if(e0t == null){
					if(e1t == Type.NUMBER || e1t == Type.BOOLEAN){
						expressionBinary.e0.setType(e1t);
						expressionBinary.setType(e1t);
						changed = true;
						status = status && true;
					}
					else{
						status = status && false;
					}
				}
				else if(e1t == null){
					if(e0t == Type.NUMBER|| e0t == Type.BOOLEAN){
						expressionBinary.e1.setType(e0t);
						expressionBinary.setType(e0t);
						changed = true;
						status = status && true;
					}
					else{
						status = status && false;
					}
				}
				else{
					if((e0t == Type.NUMBER || e0t == Type.BOOLEAN) && (e1t == Type.NUMBER || e1t == Type.BOOLEAN)){
						if(e0t == e1t){
							if(expressionBinary.getType() == null){
								changed = true;
							}
							expressionBinary.setType(e0t);
							status = status && true;
						}
						else{
							status = status && false;
						}
					}
				}
			}
			else if(expressionBinary.op.getKind() == Kind.EQ || expressionBinary.op.getKind() == Kind.NEQ || expressionBinary.op.getKind() == Kind.LT || expressionBinary.op.getKind() == Kind.LE || expressionBinary.op.getKind() == Kind.GT || expressionBinary.op.getKind() == Kind.GE){
				if(e0t == null && e1t == null)
					status = status && false;
				else if(e0t == null){
					if(e1t == Type.NUMBER || e1t == Type.STRING || e1t == Type.BOOLEAN){
						expressionBinary.e0.setType(e1t);
						expressionBinary.setType(Type.BOOLEAN);
						changed = true;
						status = status && true;
					}
					else{
						status = status && false;
					}
				}
				else if(e1t == null){
					if(e0t == Type.NUMBER || e0t == Type.STRING || e0t == Type.BOOLEAN){
						expressionBinary.e1.setType(e0t);
						expressionBinary.setType(Type.BOOLEAN);
						changed = true;
						status = status && true;
					}
					else{
						status = status && false;
					}
				}
				else{
					if((e0t == Type.NUMBER || e0t == Type.STRING || e0t == Type.BOOLEAN) && (e1t == Type.NUMBER || e1t == Type.STRING || e1t == Type.BOOLEAN)){
						if(e0t == e1t){
							if(expressionBinary.getType() == null){
								changed = true;
							}
							expressionBinary.setType(Type.BOOLEAN);
							status = status && true;
						}
						else{
							status = status && false;
						}
					}
				}
			}
			
			return status;
		}

		@Override
		public Object visitExpressionIdent(ExpressionIdent expressionIdent, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			Boolean status = true;
			Type decType = expressionIdent.getDec().getType();
			Type expType = expressionIdent.getType();
			if(decType != null && expType != null)
				status = status && (decType == expType);
			else if(decType == null && expType == null)
				status = status && false;
			else if(decType == null){
				expressionIdent.getDec().setType(expType);
				changed = true;
				status = status && true;
			}
			else if(expType == null){
				expressionIdent.setType(decType);
				changed = true;
				status = status && true;
			}

			return status;
		}

		@Override
		public Object visitExpressionNumLit(ExpressionNumLit expressionNumLit, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			if(expressionNumLit.getType() == null){
				changed = true;
			}
			expressionNumLit.setType(Type.NUMBER);

			return true;
		}

		@Override
		public Object visitExpressionStringLit(ExpressionStringLit expressionStringLit, Object arg)
				throws PLPException {
			// TODO Auto-generated method stub
			if(expressionStringLit.getType() == null){
				changed = true;
			}
			expressionStringLit.setType(Type.STRING);

			return true;
		}

		@Override
		public Object visitExpressionBooleanLit(ExpressionBooleanLit expressionBooleanLit, Object arg)
				throws PLPException {
			// TODO Auto-generated method stub
			if(expressionBooleanLit.getType() == null){
				changed = true;
			}
			expressionBooleanLit.setType(Type.BOOLEAN);

			return true;
		}

		@Override
		public Object visitProcedure(ProcDec procDec, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			Boolean status = true;
			if(procDec.getType() == null){
				changed = true;
			}
			procDec.setType(Type.PROCEDURE);
			Object r1 = procDec.block.visit(this, arg);	
			status = status && (Boolean) r1;

			return status;
		}

		@Override
		public Object visitConstDec(ConstDec constDec, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			if (constDec.val instanceof String){
				if(constDec.getType() == null){
					changed = true;
				}
				constDec.setType(Type.STRING);
			}
			else if (constDec.val instanceof Integer){
				if(constDec.getType() == null){
					changed = true;
				}
				constDec.setType(Type.NUMBER);
			}
			else if (constDec.val instanceof Boolean){
				if(constDec.getType() == null){
					changed = true;
				}
				constDec.setType(Type.BOOLEAN);
			}
			
			return true;
		}

		@Override
		public Object visitStatementEmpty(StatementEmpty statementEmpty, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public Object visitIdent(Ident ident, Object arg) throws PLPException {
			// TODO Auto-generated method stub
			return true;
		}
	}
    
    public static ASTVisitor getScopeVisitor() {
    	return new Visitor();
    }
    
	public static ASTVisitor getTypeInferenceVisitor() {
		// TODO Auto-generated method stub
		return new TypeCheckVisitor();
	}

	public static ASTVisitor getCodeGenVisitor(String className, String packageName, String sourceFileName) {
		// TODO Auto-generated method stub
		return new CodeGenVisitor(className, packageName, sourceFileName);
	}
}

