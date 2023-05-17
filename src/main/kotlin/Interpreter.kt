object Interpreter : AST.Visitor<Double> {
    fun interpret(ast: AST): Double {
        return ast.accept(this)
    }

    private fun AST.evaluate(): Double {
        return this.accept(this@Interpreter)
    }

    private val unaryHandler: Map<TokenType, (Double) -> Double> = mapOf(
        TokenType.PLUS to Double::unaryPlus,
        TokenType.MINUS to Double::unaryMinus,
    )

    private val binaryHandler: Map<TokenType, (Double, Double) -> Double> = mapOf(
        TokenType.PLUS to Double::plus,
        TokenType.MINUS to Double::minus,
        TokenType.STAR to Double::times,
        TokenType.SLASH to Double::div,
        TokenType.MOD to Double::mod,
    )

    override fun visit(ast: AST): Double {
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