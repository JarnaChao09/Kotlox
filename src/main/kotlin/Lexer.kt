@file:OptIn(ExperimentalStdlibApi::class)

private val defaultKeywords: Map<String, TokenType> = mapOf(
    "var" to TokenType.VAR,
    "val" to TokenType.VAL,
    "true" to TokenType.TRUE,
    "false" to TokenType.FALSE,
    "null" to TokenType.NULL,
    "if" to TokenType.IF,
    "else" to TokenType.ELSE,
    "for" to TokenType.FOR,
    "while" to TokenType.WHILE,
    "fun" to TokenType.FUN,
    "return" to TokenType.RETURN,
    "class" to TokenType.CLASS,
    "this" to TokenType.THIS,
    "super" to TokenType.SUPER,

    "print" to TokenType.PRINT,
)

class Lexer(
    private val source: String,
    private val keywords: Map<String, TokenType> = defaultKeywords,
    private val backing: MutableList<Token> = mutableListOf()
) {
    private var start: Int = 0
    private var current: Int = 0
    private var line = 1

    val tokens: List<Token>
        get() {
            while (!this.isAtEnd()) {
                backing += this.nextToken()
            }

            backing += Token(TokenType.EOF, "", line, null)
            return this.backing.toList()
        }

    private fun nextToken(): Token {
        this.skipWhitespace()

        this.start = this.current

        return when (val char = this.advance()) {
            '(' -> this.createToken(TokenType.LEFT_PAREN)
            ')' -> this.createToken(TokenType.RIGHT_PAREN)
            '{' -> this.createToken(TokenType.LEFT_BRACE)
            '}' -> this.createToken(TokenType.RIGHT_BRACE)
            '[' -> this.createToken(TokenType.LEFT_BRACKET)
            ']' -> this.createToken(TokenType.RIGHT_BRACKET)
            ',' -> this.createToken(TokenType.COMMA)
            '.' -> this.createToken(TokenType.DOT)
            ';' -> this.createToken(TokenType.EOS)
            '\n' -> Token(TokenType.EOS, "\\n", line++, null)
            '+' -> this.createToken(TokenType.PLUS)
            '-' -> this.createToken(TokenType.MINUS)
            '*' -> this.createToken(TokenType.STAR)
            '/' -> this.createToken(TokenType.SLASH)
            '%' -> this.createToken(TokenType.MOD)
            '!' -> this.createToken(if (this.match('=')) TokenType.NOT_EQ else TokenType.NOT)
            '=' -> this.createToken(if (this.match('=')) TokenType.EQUALS else TokenType.ASSIGN)
            '>' -> this.createToken(if (this.match('=')) TokenType.LE else TokenType.LT)
            '<' -> this.createToken(if (this.match('=')) TokenType.GE else TokenType.GT)
            '&' -> this.createToken(if (this.match('&')) TokenType.AND else TokenType.BIT_AND)
            '|' -> this.createToken(if (this.match('|')) TokenType.OR else TokenType.BIT_OR)
            '"' -> this.createString()
            in '0'..'9' -> this.createNumber()
            in 'a'..'z', in 'A'..'z' -> this.createIdentifier()
            else -> Token(TokenType.ERROR, "Unexpected character $char", line, null)
        }
    }

    private fun skipWhitespace() {
        while (true) {
            when (this.peek()) {
                ' ', '\r', '\t' -> this.advance()
                '/' -> {
                    if (this.peek(1) == '/') {
                        while (this.peek() != '\n' && !this.isAtEnd()) {
                            this.advance()
                        }
                    } else {
                        return
                    }
                }

                else -> return
            }
        }
    }

    private fun advance(): Char = this.source[this.current++]

    private fun isAtEnd(dist: Int = 0): Boolean = this.current + dist >= this.source.length

    private fun peek(dist: Int = 0): Char = if (this.isAtEnd(dist)) '\u0000' else this.source[current + dist]

    private fun createToken(type: TokenType, literal: Any? = null): Token =
        Token(type, this.source.substring(this.start..<this.current), line, literal)

    private fun match(expected: Char): Boolean = if (this.isAtEnd() || this.source[current] != expected) {
        false
    } else {
        this.current++
        true
    }

    private fun createString(): Token {
        while (this.peek() != '"' && !this.isAtEnd()) {
            if (this.peek() == '\n') {
                this.line++
            }

            this.advance()
        }

        if (this.isAtEnd()) {
            return Token(TokenType.ERROR, "Unterminated String", line, null)
        }

        this.advance()

        return this.createToken(TokenType.STRING, this.source.substring((this.start + 1)..<(this.current - 1)))
    }

    private fun createNumber(): Token {
        while (this.peek() in '0'..'9') {
            this.advance()
        }

        if (this.peek() == '.' && this.peek(1) in '0'..'9') {
            this.advance()
        }

        while (this.peek() in '0'..'9') {
            this.advance()
        }

        return this.createToken(TokenType.NUMBER, this.source.substring(this.start..<this.current).toDouble())
    }

    private fun createIdentifier(): Token {
        while (this.peek().isAlphaNumeric()) {
            this.advance()
        }

        return this.createToken(
            this.keywords.getOrDefault(
                this.source.substring(this.start..<this.current),
                TokenType.IDENTIFIER
            )
        )
    }

    private fun Char.isAlphaNumeric(): Boolean = this in 'a'..'z' || this in 'A'..'Z' || this == '_'
}