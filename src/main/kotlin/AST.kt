sealed class AST {
    interface Visitor<R> {
        fun visit(ast: AST): R
    }

    fun <R> accept(visitor: Visitor<R>): R {
        return visitor.visit(this)
    }
}

data class Literal(val value: Any?) : AST()

data class Unary(val operator: Token, val expr: AST) : AST()

data class Binary(val left: AST, val operator: Token, val right: AST) : AST()