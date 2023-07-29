sealed interface ExprAST {
    interface Visitor<R> {
        fun visit(ast: ExprAST): R
    }

    fun <R> accept(visitor: Visitor<R>): R {
        return visitor.visit(this)
    }
}

data class Assign(val name: Token, val expression: ExprAST) : ExprAST

data class Binary(val left: ExprAST, val operator: Token, val right: ExprAST) : ExprAST

data class Literal(val value: Any?) : ExprAST

data class Unary(val operator: Token, val expr: ExprAST) : ExprAST

data class Variable(val name: Token) : ExprAST

sealed interface StmtAST {
    interface Visitor<R> {
        fun visit(ast: StmtAST): R
    }

    fun <R> accept(visitor: Visitor<R>): R {
        return visitor.visit(this)
    }
}

data class Block(val statements: List<StmtAST?>) : StmtAST

data class Expression(val expr: ExprAST) : StmtAST

data class Print(val expr: ExprAST) : StmtAST

sealed interface VarStmt : StmtAST {
    val name: Token

    val initializer: ExprAST?

    companion object {
        operator fun invoke(type: TokenType, name: Token, init: ExprAST?): VarStmt {
            return when (type) {
                TokenType.VAL -> Val(name, init)
                TokenType.VAR -> Var(name, init)
                else -> error("unreachable: should only be triggered with val/var")
            }
        }
    }
}

data class Var(override val name: Token, override val initializer: ExprAST?) : VarStmt

data class Val(override val name: Token, override val initializer: ExprAST?) : VarStmt