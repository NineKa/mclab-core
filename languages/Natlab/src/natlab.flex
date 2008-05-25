package natlab;

import static natlab.NatlabParser.Terminals.*;

import beaver.Symbol;
import beaver.Scanner;

%%

%public
%final
%class NatlabScanner
%extends Scanner
%unicode
%function nextToken
%type Symbol
%yylexthrow Scanner.Exception
%line
%column

%{
  private Symbol symbol(short type) {
    if(yystate() == FIELD_NAME) {
        yybegin(YYINITIAL);
    }
    return new Symbol(type, yyline + 1, yycolumn + 1, yylength());
  }
  private Symbol symbol(short type, Object value) {
    if(yystate() == FIELD_NAME) {
        yybegin(YYINITIAL);
    }
    return new Symbol(type, yyline + 1, yycolumn + 1, yylength(), value);
  }
%}

LineTerminator = \r|\n|\r\n
OtherWhiteSpace = [ \t\f]

Ellipsis = \.\.\.
//NB: require preceding whitespace to prevent '....' case.
EscapedLineTerminator = {OtherWhiteSpace}+{Ellipsis}{LineTerminator}

Letter = [a-zA-Z]
Digit = [0-9]
HexDigit = {Digit} | [a-fA-F]
Identifier = ([_$] | {Letter}) ([_$] | {Letter} | {Digit})*
SciExp = [Ee][+-]?{Digit}+
Imaginary = [iIjJ]
DecimalNumber = (({Digit}+\.?{Digit}*) | (\.?{Digit}+)){SciExp}?{Imaginary}?
HexNumber = 0[xX]{HexDigit}+{Imaginary}?
Number = {DecimalNumber} | {HexNumber}

CommentSymbol = %
HelpComment={CommentSymbol}{CommentSymbol}.*
Comment={CommentSymbol}.*

%state FIELD_NAME

%%

{EscapedLineTerminator} { /* ignore */ }
{OtherWhiteSpace} { /* ignore */ }

{LineTerminator} { return symbol(LINE_TERMINATOR); }

{Number} { return symbol(NUMBER, yytext()); }

{HelpComment} { return symbol(HELP_COMMENT, yytext()); }
{Comment} { return symbol(COMMENT, yytext()); }

\( { return symbol(LPAREN); }
\) { return symbol(RPAREN); }
\[ { return symbol(LSQUARE); }
\] { return symbol(RSQUARE); }
\{ { return symbol(LCURLY); }
\} { return symbol(RCURLY); }

, { return symbol(COMMA); }
; { return symbol(SEMICOLON); }

: { return symbol(COLON); }

<YYINITIAL> {
    //from matlab "iskeyword" function
    break { return symbol(BREAK); }
    case { return symbol(CASE); }
    catch { return symbol(CATCH); }
    continue { return symbol(CONTINUE); }
    else { return symbol(ELSE); }
    elseif { return symbol(ELSEIF); }
    end { return symbol(END); }
    for { return symbol(FOR); }
    function { return symbol(FUNCTION); }
    global { return symbol(GLOBAL); }
    if { return symbol(IF); }
    otherwise { return symbol(OTHERWISE); }
    persistent { return symbol(PERSISTENT); }
    return { return symbol(RETURN); }
    switch { return symbol(SWITCH); }
    try { return symbol(TRY); }
    while { return symbol(WHILE); }
    
    //NB: lower precedence than keywords
    {Identifier} { return symbol(IDENTIFIER, yytext()); }
    
    //NB: lower precedence than ellipsis
    \. {
            //NB: have to change the state AFTER calling symbol
            Symbol result = symbol(DOT);
            yybegin(FIELD_NAME);
            return result;
       }
}

<FIELD_NAME> {
    {Identifier} { return symbol(IDENTIFIER, yytext()); }
    
    //NB: lower precedence than ellipsis
    \. { return symbol(DOT); }
}

/* error fallback */
.|\n          { throw new Scanner.Exception("Illegal character '" + yytext() + "'"); }

<<EOF>>       { return symbol(EOF); }