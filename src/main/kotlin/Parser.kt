class Parser(private val tokens: List<Token>) {
    private var current = 0

    fun parse(): List<StmtAST> {
        return buildList {
            while (!this@Parser.isAtEnd()) {
                add(this@Parser.statement())
            }
        }
    }

    private fun statement(): StmtAST {
        return if (match(TokenType.PRINT)) {
            this.printStatement()
        } else {
            this.expressionStatement()
        }
    }

    private fun printStatement(): StmtAST {
        return Print(expression()).also {
            expect(TokenType.EOS, "Expected a ';' after value")
        }
    }

    private fun expressionStatement(): StmtAST {
        return Expression(expression()).also {
            expect(TokenType.EOS, "Expected a ';' after value")
        }
    }

    private fun expression(): ExprAST {
        return equality()
    }

    private fun equality(): ExprAST {
        var expr = comparison()

        while (match(TokenType.NOT_EQ, TokenType.EQUALS)) {
            val operator = this.previous()
            val right = comparison()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun comparison(): ExprAST {
        var expr = term()

        while (match(TokenType.GE, TokenType.GT, TokenType.LE, TokenType.LT)) {
            val operator = this.previous()
            val right = term()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun term(): ExprAST {
        var expr = factor()

        while (match(TokenType.PLUS, TokenType.MINUS)) {
            val operator = this.previous()
            val right = factor()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun factor(): ExprAST {
        var expr = unary()

        while (match(TokenType.STAR, TokenType.SLASH, TokenType.MOD)) {
            val operator = this.previous()
            val right = unary()
            expr = Binary(expr, operator, right)
        }

        return expr
    }

    private fun unary(): ExprAST {
        if (match(TokenType.PLUS, TokenType.MINUS, TokenType.NOT)) {
            val operator = this.previous()
            return Unary(operator, unary())
        }

        return atom()
    }

    private fun atom(): ExprAST {
        return when {
            match(TokenType.TRUE) -> Literal(true)
            match(TokenType.FALSE) -> Literal(false)
            match(TokenType.NUMBER, TokenType.STRING) -> Literal(this.previous().literal)
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