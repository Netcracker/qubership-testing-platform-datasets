grammar DataSetParameter;
parameter: (macro | text)+;

macro: macroStart (parameter)? macroEnd ;

macroStart: MACROS;

macroEnd: CLOSE;

MACROS: MACROS_MARKER MACROS_NAME OPEN;
MACROS_NAME: (LETTER | DIGIT | LOW_LINE)+;

text:
ESC
| TEXT
| MACROS_NAME
| MACROS_MARKER
| OPEN
| CLOSE
| QUO
| SEPARATOR
| text text
;

MACROS_MARKER: '#' | '$';
SEPARATOR : WS* COMMA WS*;
OPEN : '(';
CLOSE: ')';
QUO: '\'';
ESC: ESC_CHAR (ESC_SYMB | 'n' | 't' | 'r');
TEXT: ~[$#()']+;

fragment LETTER : [a-zA-Z];
fragment DOT_CHAR : [.];
fragment SPECIAL: [-_+$#:;*|];
fragment LOW_LINE: [_];
fragment DIGIT  : [0-9] ;
fragment WS     : [ \r\t];
fragment ESC_CHAR : '\\';
fragment ESC_SYMB : ([&$"'\\\][<>]);
fragment COMMA   : ',';