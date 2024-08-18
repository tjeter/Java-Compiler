package edu.ufl.cise.plpfa22;

public interface ILexer {

	/**
	 * Return an IToken and advance the internal position so that subsequent calls will return subsequent ITokens.
	 * @return
	 * @throws LexicalException
	 */
	IToken next() throws LexicalException;
	
	/**
	 * Return an IToken without advancing the internal position.  A subsequent call to next or peek will return the same IToken. 
	 * @return
	 * @throws LexicalException
	 */
	IToken peek() throws LexicalException;  
}
