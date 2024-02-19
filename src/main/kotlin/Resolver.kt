object Resolver : ExprAST.Visitor<Unit>, StmtAST.Visitor<Unit> {
    private enum class FunctionType {
        NONE,
        FUNCTION,
        INITIALIZER,
        METHOD,
    }

    private enum class ClassType {
        NONE,
        CLASS,
        SUBCLASS,
    }

    private val scopes: ArrayDeque<MutableMap<String, Boolean>> = ArrayDeque()
    private var currentFunctionType: FunctionType = FunctionType.NONE
    private var currentClassType: ClassType = ClassType.NONE

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

            is Get -> {
                ast.instance.resolve()
            }

            is Grouping -> {
                ast.expr.resolve()
            }

            is Literal -> {}
            is Logical -> {
                ast.left.resolve()
                ast.right.resolve()
            }

            is Set -> {
                ast.value.resolve()
                ast.instance.resolve()
            }

            is Super -> {
                if (currentClassType == ClassType.NONE) {
                    error("Can't use 'super' outside of a class.")
                } else if (currentClassType != ClassType.SUBCLASS) {
                    error("Can't use 'super' in a class with no superclass")
                }
                resolveLocal(ast, ast.keyword)
            }

            is This -> {
                if (currentClassType == ClassType.NONE) {
                    error("Can't use 'this' outside of a class.")
                }
                resolveLocal(ast, ast.keyword)
            }

            is Unary -> {
                ast.expr.resolve()
            }

            is Variable -> {
                if (scopes.isNotEmpty() && scopes.last()[ast.name.lexeme] == false) {
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

            is Class -> {
                val enclosingClass = currentClassType
                currentClassType = ClassType.CLASS

                ast.name.declare()
                ast.name.define()

                ast.superClass?.let {
                    if (ast.name.lexeme == it.name.lexeme) {
                        error("A class can't inherit from itself")
                    }
                    currentClassType = ClassType.SUBCLASS
                    it.resolve()
                }

                ast.superClass?.let {
                    beginScope()
                    scopes.last()["super"] = true
                }

                beginScope()
                scopes.last()["this"] = true

                ast.methods.forEach {
                    val declaration = if (it.name.lexeme == "init") {
                        FunctionType.INITIALIZER
                    } else {
                        FunctionType.METHOD
                    }
                    it.resolveFunction(declaration)
                }

                endScope()

                ast.superClass?.let {
                    endScope()
                }

                currentClassType = enclosingClass
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

                ast.value?.let {
                    if (currentFunctionType == FunctionType.INITIALIZER) {
                        error("Can't return a value from an initializer")
                    }
                    it.resolve()
                }
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
        scopes.addLast(mutableMapOf())
    }

    private fun endScope() {
        scopes.removeLast()
    }

    private fun resolveLocal(expr: ExprAST, name: Token) {
        for (i in scopes.indices.reversed()) {
            if (name.lexeme in scopes[i]) {
                Interpreter.resolve(expr, scopes.size - 1 - i)
                return
            }
        }
    }

    @JvmName("privateResolve")
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
        scopes.lastOrNull()?.let {
            if (this.lexeme in it) {
                error("Variable ${this.lexeme} already declared in scope")
            }
            it[this.lexeme] = false
        }
    }

    private fun Token.define() {
        scopes.lastOrNull()?.let {
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