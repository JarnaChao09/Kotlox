sealed interface ExprAST {
    interface Visitor<R> {
        fun visit(ast: ExprAST): R
    }

    fun <R> accept(visitor: Visitor<R>): R {
        return visitor.visit(this)
    }
}

data class Literal(val value: Any?) : ExprAST

data class Unary(val operator: Token, val expr: ExprAST) : ExprAST

data class Binary(val left: ExprAST, val operator: Token, val right: ExprAST) : ExprAST