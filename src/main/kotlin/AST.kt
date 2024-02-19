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

data class Call(val callee: ExprAST, val paren: Token, val arguments: List<ExprAST>) : ExprAST

data class Get(val instance: ExprAST, val name: Token) : ExprAST

data class Grouping(val expr: ExprAST) : ExprAST

data class Literal(val value: Any?) : ExprAST

data class Logical(val left: ExprAST, val operator: Token, val right: ExprAST) : ExprAST

data class Set(val instance: ExprAST, val name: Token, val value: ExprAST) : ExprAST

data class Super(val keyword: Token, val method: Token) : ExprAST

data class This(val keyword: Token) : ExprAST

data class Unary(val operator: Token, val expr: ExprAST) : ExprAST

class Variable(val name: Token) : ExprAST

sealed interface StmtAST {
    interface Visitor<R> {
        fun visit(ast: StmtAST): R
    }

    fun <R> accept(visitor: Visitor<R>): R {
        return visitor.visit(this)
    }
}

data class Block(val statements: List<StmtAST?>) : StmtAST

data class Class(val name: Token, val superClass: Variable?, val methods: List<Function>) : StmtAST

data class Expression(val expr: ExprAST) : StmtAST

data class Function(val name: Token, val params: List<Token>, val body: List<StmtAST?>) : StmtAST

data class If(val condition: ExprAST, val trueBranch: StmtAST, val falseBranch: StmtAST?) : StmtAST

data class Print(val expr: ExprAST) : StmtAST

data class Return(val keyword: Token, val value: ExprAST?) : StmtAST

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

data class While(val condition: ExprAST, val body: StmtAST) : StmtAST