object Interpreter : AST.Visitor<Any> {
    fun interpret(ast: AST): Any {
        return ast.accept(this)
    }

    private fun AST.evaluate(): Any {
        return this.accept(this@Interpreter)
    }

    private val unaryHandler: Map<TokenType, (Any) -> Any> = mapOf(
        TokenType.PLUS to { +(it as Double) },
        TokenType.MINUS to { -(it as Double) },
        TokenType.NOT to { !(it as Boolean) }
    )

    private val binaryHandler: Map<TokenType, (Any, Any) -> Any> = mapOf(
        TokenType.PLUS to { l, r -> (l as Double) + (r as Double) },
        TokenType.MINUS to { l, r -> (l as Double) - (r as Double) },
        TokenType.STAR to { l, r -> (l as Double) * (r as Double) },
        TokenType.SLASH to { l, r -> (l as Double) / (r as Double) },
        TokenType.MOD to { l, r -> (l as Double).mod(r as Double) },
        TokenType.EQUALS to { l, r -> (l as Double) == (r as Double) },
        TokenType.NOT_EQ to { l, r -> (l as Double) != (r as Double) },
        TokenType.GE to { l, r -> (l as Double) >= (r as Double) },
        TokenType.GT to { l, r -> (l as Double) > (r as Double) },
        TokenType.LE to { l, r -> (l as Double) <= (r as Double) },
        TokenType.LT to { l, r -> (l as Double) < (r as Double) },
    )

    override fun visit(ast: AST): Any {
        return when (ast) {
            is Binary -> {
                binaryHandler[ast.operator.type]?.let {
                    it(ast.left.evaluate(), ast.right.evaluate())
                } ?: error("Unhandled binary operator ${ast.operator}")
            }
            is Literal -> {
                ast.value as Double
            }
            is Unary -> {
                unaryHandler[ast.operator.type]?.let {
                    it(ast.expr.evaluate())
                } ?: error("Unhandled unary operator ${ast.operator}")
            }
        }
    }
}