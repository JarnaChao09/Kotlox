class Parser(private val tokens: List<Token>) {
    private var current = 0

    fun parse(): AST? {
        return try {
            val expr = this.expression()

            if (this.peek().type != TokenType.EOF) {
                error("Invalid Expression")
            }

            expr
        } catch (e: Exception) {
            println("[ERROR]: ${e.message}")
            null
        }
    }

    private fun expression(): AST {
        return equality()
    }

    private fun equality(): AST {
        var expr = comparison()

        while (match(TokenType.NOT_EQ, TokenType.EQUALS)) {
            val operator = this.previous()
            val right = comparison()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun comparison(): AST {
        var expr = term()

        while (match(TokenType.GE, TokenType.GT, TokenType.LE, TokenType.LT)) {
            val operator = this.previous()
            val right = term()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun term(): AST {
        var expr = factor()

        while (match(TokenType.PLUS, TokenType.MINUS)) {
            val operator = this.previous()
            val right = factor()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun factor(): AST {
        var expr = unary()

        while (match(TokenType.STAR, TokenType.SLASH, TokenType.MOD)) {
            val operator = this.previous()
            val right = unary()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun unary(): AST {
        if (match(TokenType.PLUS, TokenType.MINUS, TokenType.NOT)) {
            val operator = this.previous()
            return Unary(operator, unary())
        }

        return atom()
    }

    private fun atom(): AST {
        return when {
            match(TokenType.NUMBER) -> Literal(this.previous().literal)
            match(TokenType.LEFT_PAREN) -> {
                val expr = expression()
                expect(TokenType.RIGHT_PAREN, "Expecting ')' after expression")
                expr
            }
            else -> error("Invalid expression")
        }
    }

    private fun isAtEnd(): Boolean {
        return this.peek().type == TokenType.EOF
    }

    private fun advance(): Token {
        if (!this.isAtEnd()) {
            current++
        }

        return this.previous()
    }

    private fun peek(): Token {
        return this.tokens[current]
    }

    private fun previous(): Token {
        return this.tokens[current - 1]
    }

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (this.checkCurrent(type)) {
                this.advance()
                return true
            }
        }

        return false
    }

    private fun checkCurrent(type: TokenType): Boolean {
        return !this.isAtEnd() && this.peek().type == type
    }

    private fun expect(type: TokenType, message: String): Token {
        return if (this.checkCurrent(type)) {
            advance()
        } else {
            error(message)
        }
    }
}