class Parser(private val tokens: List<Token>) {
    private var current = 0

    fun parse(): List<StmtAST?> {
        return buildList {
            while (!this@Parser.isAtEnd()) {
                add(this@Parser.declaration())
            }
        }
    }

    private fun declaration(): StmtAST? {
        return try {
            if (match(TokenType.VAR, TokenType.VAL)) {
                this.variableDeclaration()
            } else {
                this.statement()
            }
        } catch (err: IllegalStateException) {
            null
        }
    }

    private fun variableDeclaration(): StmtAST {
        val type = this.previous().type
        val name = expect(TokenType.IDENTIFIER, "Expected a variable name")

        val initializer = if (match(TokenType.ASSIGN)) {
            expression()
        } else {
            null
        }

        expect(TokenType.EOS, "Expect ';' after variable declaration")

        return VarStmt(type, name, initializer)
    }

    private fun statement(): StmtAST {
        return if (match(TokenType.PRINT)) {
            this.printStatement()
        } else if (match(TokenType.LEFT_BRACE)) {
            Block(this.block())
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

    private fun block(): List<StmtAST?> {
        return buildList {
            while (!checkCurrent(TokenType.RIGHT_BRACE) && !this@Parser.isAtEnd()) {
                add(declaration())
            }

            expect(TokenType.RIGHT_BRACE, "Expect '}' after a block")
        }
    }

    private fun expression(): ExprAST {
        return assignment()
    }

    private fun assignment(): ExprAST {
        val expr = equality()

        if (match(TokenType.ASSIGN)) {
            val equals = previous()
            val value = assignment()

            if (expr is Variable) {
                val name = expr.name
                return Assign(name, value)
            }

            // todo: don't throw here when parser synchronization is implemented
            error("Error @ ${equals.line}: Invalid Assignment Target.")
        }

        return expr
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
            match(TokenType.IDENTIFIER) -> Variable(this.previous())
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