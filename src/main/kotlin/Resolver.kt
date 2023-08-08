object Resolver : ExprAST.Visitor<Unit>, StmtAST.Visitor<Unit> {
    private enum class FunctionType {
        NONE,
        FUNCTION
    }

    private val scopes: ArrayDeque<MutableMap<String, Boolean>> = ArrayDeque()
    private var currentFunctionType: FunctionType = FunctionType.NONE

    fun resolve(statements: List<StmtAST?>) {
        statements.resolve()
    }

    override fun visit(ast: ExprAST) {
        when (ast) {
            is Assign -> {
                ast.expression.resolve()
                resolveLocal(ast, ast.name)
            }
            is Binary -> {
                ast.left.resolve()
                ast.right.resolve()
            }
            is Call -> {
                ast.callee.resolve()

                ast.arguments.forEach {
                    it.resolve()
                }
            }
            is Grouping -> {
                ast.expr.resolve()
            }
            is Literal -> {}
            is Logical -> {
                ast.left.resolve()
                ast.right.resolve()
            }
            is Unary -> {
                ast.expr.resolve()
            }
            is Variable -> {
                if (scopes.isNotEmpty() && scopes.first()[ast.name.lexeme] == false) {
                    error("Can not read local variable in variable's own initializer")
                }

                resolveLocal(ast, ast.name)
            }
        }
    }

    override fun visit(ast: StmtAST) {
        when (ast) {
            is Block -> {
                beginScope()
                ast.statements.resolve()
                endScope()
            }
            is Expression -> {
                ast.expr.resolve()
            }
            is Function -> {
                ast.name.declare()
                ast.name.define()

                ast.resolveFunction(FunctionType.FUNCTION)
            }
            is If -> {
                ast.condition.resolve()
                ast.trueBranch.resolve()
                ast.falseBranch?.resolve()
            }
            is Print -> {
                ast.expr.resolve()
            }
            is Return -> {
                if (this.currentFunctionType == FunctionType.NONE) {
                    error("Top level return is not allowed")
                }
                
                ast.value?.resolve()
            }
            is VarStmt -> {
                ast.name.declare()
                ast.initializer?.let {
                    ast.initializer.resolve()
                }
                ast.name.define()
            }
            is While -> {
                ast.condition.resolve()
                ast.body.resolve()
            }
        }
    }

    private fun beginScope() {
        scopes.addFirst(mutableMapOf())
    }

    private fun endScope() {
        scopes.removeFirst()
    }

    private fun resolveLocal(expr: ExprAST, name: Token) {
        for (i in scopes.size downTo 0) {
            if (name.lexeme in scopes[i]) {
                Interpreter.resolve(expr, scopes.size - 1 - i)
            }
        }
    }

    private fun List<StmtAST?>.resolve() {
        this.forEach {
            it.resolve()
        }
    }

    private fun StmtAST?.resolve() {
        this?.accept(this@Resolver)
    }

    private fun ExprAST?.resolve() {
        this?.accept(this@Resolver)
    }

    private fun Token.declare() {
        scopes.firstOrNull()?.let {
            if (this.lexeme in it) {
                error("Variable ${this.lexeme} already declared in scope")
            }
            it[this.lexeme] = false
        }
    }

    private fun Token.define() {
        scopes.firstOrNull()?.let {
            it[this.lexeme] = true
        }
    }

    private fun Function.resolveFunction(type: FunctionType) {
        val enclosingFunction = this@Resolver.currentFunctionType
        this@Resolver.currentFunctionType = type

        beginScope()
        for (param in this.params) {
            param.declare()
            param.define()
        }

        this.body.resolve()
        endScope()

        this@Resolver.currentFunctionType = enclosingFunction
    }
}