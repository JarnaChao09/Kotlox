data class Token(val type: TokenType, val lexme: String, val line: Int, val literal: Any?) {
    override fun toString(): String = "$type \"$lexme\" @ $line${literal?.let { " $literal" } ?: ""}"
}

enum class TokenType {
    LEFT_PAREN,
    RIGHT_PAREN,
    LEFT_BRACE,
    RIGHT_BRACE,
    LEFT_BRACKET,
    RIGHT_BRACKET,

    COMMA,
    DOT,

    PLUS,
    MINUS,
    STAR,
    SLASH,
    MOD,

    EOS,

    VAR,
    VAL,
    ASSIGN,

    NOT,
    NOT_EQ,
    EQUALS,
    GT,
    GE,
    LT,
    LE,

    TRUE,
    FALSE,
    AND,
    OR,

    BIT_AND,
    BIT_OR,

    IDENTIFIER,
    STRING,
    NUMBER,
    NULL,

    IF,
    ELSE,

    FOR,
    WHILE,

    FUN,
    RETURN,

    CLASS,
    THIS,
    SUPER,

    EOF,

    ERROR,

    PRINT,
}