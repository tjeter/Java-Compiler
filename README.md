# Small Programming Language Compiler

This project was completed by Ariel Weitzenfeld (@arielweitz) and Tre' Jeter (@tjeter).

The structure of this project is broken into 6 assignments that combine into one functioning compiler.
All classes for this project are in package edu.ufl.cise.plpfa22;

## Assignment 1:
Write a Lexer/Scanner that follows the given lexical structure:
Lexical Structure
<token> ::= <ident> | <num_lit> | <string_lit> | . | , | ; | ( | ) | + | - | * | / | % (updated 9/6 3pm)
| ? | ! | := | = | # | < | <= | > | >= | <reserved>
<ident> ::= ( a..z | A..Z | _ | $ ) ( a..z | A..Z | _ | $ | 0 .. 9 )* but not <reserved>
<num_lit> ::= 0 | (1 .. 9) (0 .. 9)*
<string_lit> ::= “ ( \ ( b | t | n | f | r | “ | ‘ | \ ) | NOT ( \ | “ ) ) * “ (updated 9/3 10:21am)
<reserved> ::= <boolean_lit> | <keyword> |
<boolean_lit> ::= TRUE | FALSE
<keywords> ::= CONST | VAR | PROCEDURE | CALL | BEGIN | END
| IF | THEN | WHILE | DO
<comment> ::= // (NOT( '\n’|'\r'))* ( ( ‘\r’ | ε ) ‘\n’) | EOF
In the above (borrowing Java notation) , ‘\n’ represents the LF or NL ascii control character. ‘\r’
represents the CR ascii control character. Depending on the platform, a line will end with either
‘\n’, or ‘\r’‘\n’. This does not matter for recognizing tokens, but does matter for the token’s line
number in the source code.
You may assume that a ‘\r’ without a following ‘\n’ will not occur in your input. Therefore, a
comment starts with //, and ends with either ‘\r’’\n’, ‘\n’, or the end of the input. EOF means the
end of the input.
<white space> ::= (' '|'\t'|'\r'|'\n')+
In the definition of white space, the alternatives are (borrowing Java notation again): space, tab,
DR, NL.
<white_space> and <comment> serve to separate tokens, but are otherwise ignored.

### What was developed:
1.) A class (or classes) that implements the IToken interface and represents tokens. 

2.) A class that implements the ILexer interface and tokenizes its input according to the given lexical structure.
- repeatedly invoking next() should return all of the tokens in order.  After all the tokens have been returned, subsequent calls should return a token with kind EOF.
- If an error is discovered, throw a LexicalException.  The error message will not be graded but you will appreciate it later if your error messages are informative.
- You may implement your lexer so that it generates tokens on demand, or you may compute all the tokens first and then return them when requested.  If you choose the latter approach, make sure that no exceptions are thrown until the error occurs.  (For example, if the input is "123 @", the first call to next should return a NUM_LIT token with 123.  The second call should throw an exception.)  The ERROR kind may be useful for managing exceptions, it is not required to be used and no ITokens with kind ERROR should ever be returned from next() or peek().

3.) Modify CompilerComponentFactory.java so that its getLexer method returns an instance of your ILexer implementing class.


## Assignment 2:
Write a parser and AST generator that follows the given phrase structure:
Phrase Structure
Phrase Structure
Items with a pink font correspond to terminals (i.e. tokens).  Items in blue are the name of the AST class corresponding to that element of the grammar.  

<program>	::=	 <block> .     Program
<block>     	::=        Block(List<ConstDec>,List<VarDec>,List<ProcDec>,Statement)
 	(CONST <ident> = <const_val> ( ,  <ident> = <const_val> )*  ; )* 
                        List<ConstDec>
                                    (VAR   <ident> ( , <ident> )* ) ; )*
                                                List<VarDec>
			(PROCEDURE <ident> ; <block> ;  )*
				List<ProcDec>
                                    <statement>  
				Statement
<statement    ::=	<ident> := <expression>   StatementAssign(Ident,Expression)
                                    |  CALL <ident>                 StatementCall(Ident)
 			|  ?  <ident>                       StatementInput(Ident)
			|  !  <expression>              StatementOutput(Expression)
			|  BEGIN <statement> ( ;  <statement> )*  END
						      StatementBlock(List<Statement>)
			I  IF <expression> THEN <statement>
						      StatementIf(Expression, Statement)
			|  WHILE <expression> DO <statement>
						      StatementWhile(Expression, Statement)
                                    | ε			      StatementEmpty
                                    			
<expression> 	::= <additive_expression>  ( ( < |  > | = | # | <= | >=) <additive_expression>)*
			Expression   or ExpressionBinary(Expression, OP, Expression)
<additive_expression> ::= <multiplicative_expression> ((  +  |  - )  <multiplicative_expression>)*
			Expression   or ExpressionBinary(Expression, OP, Expression)
<multiplicative_expression> ::= <primary_expression> (  ( * | / | % ) <primary_expression> )*
			Expression   or ExpressionBinary(Expression, OP, Expression)
<primary_expression> ::= <ident>       ExpressionIdent(IToken)
			|  <const_val>   Expression
			|  (  <expression> )   Expression
<const_val> 	:=          <num_lit>    		ExpressionNumLit
                                   | <string_lit>   		ExpressionStringLit
                                   | <boolean_lit>		ExpressionBooleanLit

### What was developed:
1.) Correct any errors discovered in your lexer.

2.) Modify your CompilerComponentFactory class by adding a method that returns an instance of your parser.  The signature should be
         public static IParser getParser(ILexer lexer) 

3.) Develop a class that implements the IParser interface.  It should return an AST for the provided input if it is correct according to the context-free-grammar and lexical structure.  If there is an error, an exception should be thrown.  Errors found during lexical analysis should result in a LexicalException.  Errors found during parsing should result in a SyntaxException.

4.) All of your classes should be in package edu.ufl.cise.plpfa22

5.) Do not modify the provided code except for the following
- You may modify the toString method for the AST classes. 
- You must modify the  CompilerComponentFactory.java as described above. 


## Assignment 3:
Create an ASTVisitor (i.e. a class that implements the ASTVisitor interface) that uses a symbol table to set up scopes in the language.  Our language follows standard block structure as discussed in class where identifiers are valid from the point where they are declared through the end of the scope.  The only exception to the preceding rule is that procedure names are valid in the entire scope.  The visitor should decorate each declaration with its nesting level.  It should also decorate each reference to an identifier with the corresponding declaration and nesting level of the reference.  The LeBlanc-Cook symbol table as described in class is an appropriate data structure for the symbol table. 

### What was developed:
1.) Correct any errors  in your lexer or parser.

2.) Modify your CompilerComponentFactory class by adding a method that returns an instance of your scope checking ASTVisitor.  The signature should be
             
	- public static ASTVisitor getScopeVisitor() 

3.) Develop a class that implements the ASTVisitor interface and checks that names are properly declared and sets new fields in certain nodes.

4.) Add field nest along with getter and setter methods to the following AST classes classes:
- Declaration, ExpressionIdent, Ident
    - int nest;
    public void setNest(int nest) {
        
	this.nest = nest;
    
    }   
    
    public int getNest() {
    	
	return nest;
    
    }

5.) Add field dec along with getter and setter methods to the following classes
- ExpressionIdent, Ident
- Declaration dec;
- public Declaration getDec() {
        
	return dec;
    
    }
- public void setDec(Declaration dec) {
        
	this.dec = dec;
    
    }

6.) All of your classes should be in package edu.ufl.cise.plpfa22

7.) You may want to modify the toString method for some or all of the AST classes.

8.) The added get and nest fields should be set by your ASTVisitor.  If an AST node does not have one of these fields, the corresponding visit method will usually just visit its children.  (Remember to set the nest field for the subclasses of Declaration)

9.) The requirement that procedure names are visible in the entire scope means that in each block, you will need to visit ProcDec nodes twice.  The first time adds the name and other info to the symbol table.  The second time visits the block. 

10.) This assignment does not do any type checking.  (We will add that in assignment 4).  The only errors are when an identifier is not declared as required, or when the same identifier is declared twice in the same scope. 


## Assignment 4:
Create an ASTVisitor (i.e. a class that implements the ASTVisitor interface) that infers and checks types of names and expressions in our language.  The visited AST should already have been visited by the Visitor created in Assignment 3 and thus each identifier is linked to its declaration. 

The assigned types must satisfy the constraints given below.

### What was developed:
1.) Correct any errors from previous assignments.

2.) Modify your CompilerComponentFactory class by adding a method that returns an instance of your type checking ASTVisitor.  The signature should be
                 
	- public static ASTVisitor getTypeInferenceVisitor()


3.) Develop a class that implements the ASTVisitor interface and infers types of variables and expressions and ensures that they are used correctly
- Use the enum in the Types class to represent types. 
- package edu.ufl.cise.plpfa22.ast;

public class Types {
    
    public static enum Type {NUMBER, BOOLEAN, STRING, PROCEDURE};

}

- Your visitor will need to traverse the AST multiple times, terminating when either all nodes in the AST have been fully typed (a node is fully typed if any necessary types at this node have been inferred and checked, and all of its children have been fully typed.) or no additional nodes were typed on the previous pass. 
- If the visitor terminates without having inferred all needed types, this indicates that insufficient type information was available to type all nodes.  In this case, a TypeCheckException should be thrown.  (See test insufficientTypeInfo)
- Declarations of variables that are declared, but never used do not need to be typed.  (See test unusedVariable). 
- Information about the target of assignments may be used to infer types of expression on the right hand side of an assignment statement.  (See inferexzunusedy where the type of x in line 5 comes from the type of z. )

#### Type System:
A program is correctly typed if the following constraints are satisfied. 

- ProcDec has type PROCEDURE and its children are correctly typed
- ConstDec type matches the type of its value
- VarDec obtains its type from the context of a reference to a variable with this declaration.  VarDecs for variables that are never used in the program do not need a type.
- ExpressionNumLit has type NUMBER
- ExpressionStringLit has type STRING
- ExpressionBooleanLit has type BOOLEAN
- StatementAssign: The type of the variable on the left side is the same as the type of expression on the right side.
- StatementCall: The name used in the call statement must have type PROCEDURE
- StatementInput: The type of the target variable must be NUMBER, STRING, or BOOLEAN.
- StatementOutput: The expression type must be NUMBER, STRING, or BOOELAN
- If Statement: The guard expression must have type BOOLEAN, the statement must be correctly typed.
- While Statement: The guard expression must have type BOOLEAN, the statement must be correctly typed.
- ExpressionIdent: The expression’s type is the same as the type of its declaration.
- BinaryExpression: The constraints depend on the operator. 

PLUS:					α x α  → α, where α ∈ {NUMBER, STRING, BOOLEAN}

MINUS, DIV, MOD:			NUMBER x NUMBER → NUMBER

TIMES:					α x α  → α, where α ∈ {NUMBER, BOOLEAN}

EQ, NEQ, LT, LE, GT, GE:		α x α  → BOOLEAN, where α ∈ {NUMBER, STRING, BOOLEAN}

Program, Block, and StatementBlock are correctly typed if all of their children are correctly typed.


## Assignment 5:
Your task in this assignment is to create an ASTVisitor  that generates code for the subset of our language that does not use constants, variables, or procedures. 

Download asm 9.4.  (See the lecture on Code Generation and ASM for details)

You will want to watch the lectures on the JVM and ASM before attempting this assignment.

- Correct any errors from previous assignments.

- Modify your CompilerComponentFactory class by adding a method that returns an instance of your  ASTVisitor  The signature should be:

                     - public static ASTVisitor getCodeGenVisitor(String packageName, String className, String sourceFileName) ;


1.) Complete the impementation of the CodeGenVisitor class so that it can handle any programs in the subset of our language that does not involve constants, variables, or procedures.  (We will add these in the next assignment)  Your submission for this assignment should have an implementation of visitBlock, visitProgram, visitStatementOutput, visitStatementBlock, visitStatementIf, visitExpressionBinary, visitExpressionNumLit, visitExpressionStringLit, and visitExpressionBooleanLit.

2.) The semantics of the language should be self-explanatory for the most part.  We consider FALSE < TRUE, + means OR, and * means AND.   For Strings S0 and S1, S0+S1 is the concatenation of S0 with S1. S0 < S1 means that S0 is a prefix of S1 and they are not equal to each other.  S0 > S1 means that S1 is a suffix of S0 and they are not equal.  In the java/lang/String class, methods concat, startsWith, endsWith, and equals will likely be useful.  " ! exp " means output the value of the expression on the standard output.  Use System.out.println but note that you will need to give the type of the expression as an argument to the INVOKEVIRTUAL instruction.


## Assignment 6:

Complete the compiler for our language.
