grammar gradle;

@header {
package net.runelite.pluginhub.sanitizer;
}

// -------- PARSER --------

file
    : statement* EOF
    ;

statement
  : dependenciesBlock
  | versionAssignment
  | .
  ;

versionAssignment: VERSION '=' STRING ;

dependenciesBlock
    : DEPENDENCIES LBRACE dependencyStmt* RBRACE
    ;

dependencyStmt
    : configuration (mapNotation | stringNotation) NEWLINE*
    ;

configuration
    : COMPILE_ONLY
    | IMPLEMENTATION
    | TEST_IMPLEMENTATION
    | ANNOTATION_PROCESSOR
    ;

mapNotation
    : (GROUP COLON value COMMA?)?
      (NAME COLON value COMMA?)?
      (VERSION COLON value COMMA?)?
    ;

stringNotation
    : STRING
    ;

value
    : STRING
    | IDENTIFIER
    ;

// -------- LEXER --------

DEPENDENCIES        : 'dependencies';
COMPILE_ONLY        : 'compileOnly';
IMPLEMENTATION      : 'implementation';
TEST_IMPLEMENTATION : 'testImplementation';
ANNOTATION_PROCESSOR: 'annotationProcessor';

GROUP   : 'group';
NAME    : 'name';
VERSION : 'version';

// symbols
LBRACE : '{';
RBRACE : '}';
COLON  : ':';
COMMA  : ',';

// strings: 'foo:bar:1.0'
STRING
    : '\'' ( ~['\\] | '\\' . )* '\''
    ;

// identifiers (e.g. runeLiteVersion)
IDENTIFIER
    : [a-zA-Z_][a-zA-Z0-9_.]*
    ;

// whitespace / noise
NEWLINE : [\r\n]+ -> skip;
WS      : [ \t]+ -> skip;

// ignore everything else
OTHER
    : .
    -> skip
    ;