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
            when {
                match(TokenType.CLASS) -> classDeclaration()
                match(TokenType.VAR, TokenType.VAL) -> variableDeclaration()
                match(TokenType.FUN) -> functionDeclaration("function")
                else -> statement()
            }
        } catch (err: IllegalStateException) {
            println("[ERROR]: ${err.message}")
            null
        }
    }

    private fun classDeclaration(): StmtAST {
        val name = expect(TokenType.IDENTIFIER, "Expected class name.")

        val superClass = if (match(TokenType.LT)) {
            expect(TokenType.IDENTIFIER, "Expected superclass name.")
            Variable(previous())
        } else {
            null
        }

        expect(TokenType.LEFT_BRACE, "Expected '{' before a class body.")

        val methods = buildList {
            while (!checkCurrent(TokenType.RIGHT_BRACE) && !this@Parser.isAtEnd()) {
                add(functionDeclaration("method"))
            }
        }

        expect(TokenType.RIGHT_BRACE, "Expected '}' after class body.")

        return Class(name, superClass, methods)
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
        return when {
            match(TokenType.FOR) -> forStatement()
            match(TokenType.IF) -> ifStatement()
            match(TokenType.PRINT) -> printStatement()
            match(TokenType.RETURN) -> returnStatement()
            match(TokenType.WHILE) -> whileStatement()
            match(TokenType.LEFT_BRACE) -> Block(block())
            else -> expressionStatement()
        }
    }

    private fun expressionStatement(): StmtAST {
        return Expression(expression()).also {
            expect(TokenType.EOS, "Expected a ';' after value")
        }
    }

    private fun functionDeclaration(kind: String): Function {
        val name = expect(TokenType.IDENTIFIER, "Expect $kind name")
        expect(TokenType.LEFT_PAREN, "Expect '(' after $kind name")
        val parameters = buildList {
            if (!checkCurrent(TokenType.RIGHT_PAREN)) {
                do {
                    if (size >= 255) {
                        error("Cant have more than 255 parameters")
                    }

                    add(expect(TokenType.IDENTIFIER, "Expected parameter name"))
                } while (match(TokenType.COMMA))
            }
            expect(TokenType.RIGHT_PAREN, "Expect ')' after parameters")
        }

        expect(TokenType.LEFT_BRACE, "Expect '{' before $kind body")

        return Function(name, parameters, block())
    }

    private fun forStatement(): StmtAST {
        expect(TokenType.LEFT_PAREN, "Expect '(' after 'for'")

        val initializer = if (match(TokenType.EOS)) {
            null
        } else if (match(TokenType.VAL, TokenType.VAR)) {
            variableDeclaration()
        } else {
            expressionStatement()
        }

        val condition = if (!checkCurrent(TokenType.EOS)) {
            expression()
        } else {
            Literal(true)
        }
        expect(TokenType.EOS, "Expect ';' after loop condition")

        val increment = if (!checkCurrent(TokenType.RIGHT_PAREN)) {
            expression()
        } else {
            null
        }
        expect(TokenType.RIGHT_PAREN, "Expect ')' after for clauses")

        var body = statement()

        increment?.let {
            body = Block(listOf(body, Expression(increment)))
        }

        body = While(condition, body)

        initializer?.let {
            body = Block(listOf(initializer, body))
        }

        return body
    }

    private fun ifStatement(): StmtAST {
        expect(TokenType.LEFT_PAREN, "Expect a '(' after 'if'")

        val condition = expression()

        expect(TokenType.RIGHT_PAREN, "Expect a ')' after 'if' condition")

        val trueBranch = statement()
        val falseBranch = if (match(TokenType.ELSE)) {
            statement()
        } else {
            null
        }

        return If(condition, trueBranch, falseBranch)
    }

    private fun printStatement(): StmtAST {
        return Print(expression()).also {
            expect(TokenType.EOS, "Expected a ';' after value")
        }
    }

    private fun returnStatement(): StmtAST {
        val keyword = this.previous()
        val value = if (!checkCurrent(TokenType.EOS)) {
            expression()
        } else {
            null
        }

        expect(TokenType.EOS, "Expect ';' after return value")
        return Return(keyword, value)
    }

    private fun whileStatement(): StmtAST {
        expect(TokenType.LEFT_PAREN, "Expect '(' after 'while'")

        val condition = expression()

        expect(TokenType.RIGHT_PAREN, "Expect ')' after 'while' condition")

        val body = statement()

        return While(condition, body)
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
        val expr = or()

        if (match(TokenType.ASSIGN)) {
            val equals = previous()
            val value = assignment()

            if (expr is Variable) {
                val name = expr.name
                return Assign(name, value)
            } else if (expr is Get) {
                return Set(expr.instance, expr.name, value)
            }

            // todo: don't throw here when parser synchronization is implemented
            error("Error @ ${equals.line}: Invalid Assignment Target.")
        }

        return expr
    }

    private fun or(): ExprAST {
        var expr = and()

        while (match(TokenType.OR)) {
            val operator = this.previous()
            val right = and()
            expr = Logical(expr, operator, right)
        }

        return expr
    }

    private fun and(): ExprAST {
        var expr = equality()

        while (match(TokenType.AND)) {
            val operator = this.previous()
            val right = equality()
            expr = Logical(expr, operator, right)
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

        return call()
    }

    private fun call(): ExprAST {
        var expr = atom()

        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr)
            } else if (match(TokenType.DOT)) {
                val name = expect(TokenType.IDENTIFIER, "Expected property name after '.'.")
                expr = Get(expr, name)
            } else {
                break
            }
        }

        return expr
    }

    private fun finishCall(callee: ExprAST): ExprAST {
        val arguments = buildList {
            if (!checkCurrent(TokenType.RIGHT_PAREN)) {
                do {
                    if (size >= 255) {
                        error("Can't have more than 255 arguments")
                    }
                    add(expression())
                } while (match(TokenType.COMMA))
            }
        }

        return Call(callee, expect(TokenType.RIGHT_PAREN, "Expect ')' after arguments"), arguments)
    }

    private fun atom(): ExprAST {
        return when {
            match(TokenType.TRUE) -> Literal(true)
            match(TokenType.FALSE) -> Literal(false)
            match(TokenType.NULL) -> Literal(null)
            match(TokenType.SUPER) -> {
                val keyword = previous()
                expect(TokenType.DOT, "Expect '.' after 'super'.")
                val method = expect(TokenType.IDENTIFIER, "Expect superclass method name")
                Super(keyword, method)
            }
            match(TokenType.THIS) -> This(this.previous())
            match(TokenType.IDENTIFIER) -> Variable(this.previous())
            match(TokenType.NUMBER, TokenType.STRING) -> Literal(this.previous().literal)
            match(TokenType.LEFT_PAREN) -> {
                val expr = expression()
                expect(TokenType.RIGHT_PAREN, "Expecting ')' after expression")
                Grouping(expr)
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