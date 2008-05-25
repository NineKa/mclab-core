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
    return new Symbol(type, yyline + 1, yycolumn + 1, yylength());
  }
  private Symbol symbol(short type, Object value) {
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

{LineTerminator} { yybegin(YYINITIAL); return symbol(LINE_TERMINATOR); }

{Number} { yybegin(YYINITIAL); return symbol(NUMBER, yytext()); }

{HelpComment} { yybegin(YYINITIAL); return symbol(HELP_COMMENT, yytext()); }
{Comment} { yybegin(YYINITIAL); return symbol(COMMENT, yytext()); }

\( { yybegin(YYINITIAL); return symbol(LPAREN); }
\) { yybegin(YYINITIAL); return symbol(RPAREN); }
\[ { yybegin(YYINITIAL); return symbol(LSQUARE); }
\] { yybegin(YYINITIAL); return symbol(RSQUARE); }
\{ { yybegin(YYINITIAL); return symbol(LCURLY); }
\} { yybegin(YYINITIAL); return symbol(RCURLY); }

, { yybegin(YYINITIAL); return symbol(COMMA); }
; { yybegin(YYINITIAL); return symbol(SEMICOLON); }



<YYINITIAL> {
    break { return symbol(BREAK); }
    case { return symbol(CASE); }
    catch { return symbol(CATCH); }
    continue { return symbol(CONTINUE); }
    do { return symbol(DO); }
    else { return symbol(ELSE); }
    elseif { return symbol(ELSEIF); }
    end { return symbol(END); }
    endfunction { return symbol(ENDFUNCTION); }
    for { return symbol(FOR); }
    function { return symbol(FUNCTION); }
    global { return symbol(GLOBAL); }
    if { return symbol(IF); }
    otherwise { return symbol(OTHERWISE); }
    return { return symbol(RETURN); }
    static { return symbol(STATIC); }
    switch { return symbol(SWITCH); }
    try { return symbol(TRY); }
    until { return symbol(UNTIL); }
    varargin { return symbol(VARARGIN); }
    varargout { return symbol(VARARGOUT); }
    while { return symbol(WHILE); }
    
    //NB: lower precedence than keywords
    {Identifier} { return symbol(IDENTIFIER, yytext()); }
    
    //NB: lower precedence than ellipsis
    \. { yybegin(FIELD_NAME); return symbol(DOT); }
}

<FIELD_NAME> {
    {Identifier} {  yybegin(YYINITIAL); return symbol(IDENTIFIER, yytext()); }
    
    //NB: lower precedence than ellipsis
    \. { yybegin(YYINITIAL); return symbol(DOT); }
}

/* error fallback */
.|\n          { throw new Scanner.Exception("Illegal character '" + yytext() + "'"); }

<<EOF>>       { return symbol(EOF); }