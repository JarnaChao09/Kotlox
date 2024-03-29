object Interpreter : ExprAST.Visitor<Any?>, StmtAST.Visitor<Unit> {
    val globals: Environment = Environment()
    private var environment: Environment = globals
    private var locals: MutableMap<ExprAST, Int> = mutableMapOf()

    init {
        globals["clock"] = object : LoxCallable {
            override val arity: Int
                get() = 0

            override fun invoke(interpreter: Interpreter, arguments: List<Any?>): Any {
                return System.currentTimeMillis() / 1000.0
            }

            override fun toString(): String = "<native function>"
        }
    }

    fun interpret(ast: List<StmtAST?>) {
        ast.forEach {
            it?.execute()
        }
    }

    fun resolve(expr: ExprAST, depth: Int) {
        this.locals[expr] = depth
    }

    fun executeBlock(statements: List<StmtAST?>, environment: Environment) {
        val previous = this.environment
        try {
            this.environment = environment

            statements.forEach {
                it?.execute()
            }
        } finally {
            this.environment = previous
        }
    }

    private fun lookUpVariable(name: Token, expr: ExprAST): Any? {
        return this.locals[expr]?.let {
            this.environment.getAt(it, name.lexeme)
        } ?: this.globals[name]
    }

    private fun StmtAST.execute() {
        this.accept(this@Interpreter)
    }

    private fun ExprAST.evaluate(): Any? {
        return this.accept(this@Interpreter)
    }

    private fun Any?.isTruthy(): Boolean {
        return this?.let {
            when (it) {
                is Boolean -> it
                else -> true
            }
        } ?: false
    }

    private fun Any?.isNotTruthy(): Boolean = !isTruthy()

    private val unaryHandler: Map<TokenType, (Any) -> Any> = mapOf(
        TokenType.PLUS to { +(it as Double) },
        TokenType.MINUS to { -(it as Double) },
        TokenType.NOT to { !(it as Boolean) }
    )

    private val binaryHandler: Map<TokenType, (Any, Any) -> Any> = mapOf(
        TokenType.PLUS to { l, r ->
            when (l) {
                is String -> when (r) {
                    is String -> l + r
                    is Double -> error("Illegal type for right hand side, expected String")
                    else -> error("Illegal type for right hand side, expected String")
                }

                is Double -> when (r) {
                    is String -> error("Illegal type for right hand side, expected Double")
                    is Double -> l + r
                    else -> error("Illegal Type for right hande side, expected Double")
                }

                else -> error("Illegal Type for left hand side, expected (String | Double)")
            }
        },
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

    override fun visit(ast: ExprAST): Any? {
        return when (ast) {
            is Binary -> {
                binaryHandler[ast.operator.type]?.let {
                    it(ast.left.evaluate()!!, ast.right.evaluate()!!)
                } ?: error("Unhandled binary operator ${ast.operator}")
            }

            is Call -> {
                val callee = ast.callee.evaluate()

                val arguments = ast.arguments.map { it.evaluate() }

                if (callee !is LoxCallable) {
                    throw RuntimeException("Can only call functions and classes")
                }

                if (arguments.size != callee.arity) {
                    throw RuntimeException("Expected ${callee.arity} arguments but got ${arguments.size}")
                }

                return callee(this, arguments)
            }

            is Get -> {
                val obj = ast.instance.evaluate()
                if (obj is LoxInstance) {
                    return obj[ast.name]
                }

                throw RuntimeException("Only instances have properties")
            }

            is Grouping -> {
                ast.expr.evaluate()
            }

            is Literal -> {
                ast.value
            }

            is Logical -> {
                val left = ast.left.evaluate()

                if (ast.operator.type == TokenType.OR) {
                    if (left.isTruthy()) {
                        return left
                    }
                } else {
                    if (left.isNotTruthy()) {
                        return left
                    }
                }

                ast.right.evaluate()
            }

            is Set -> {
                val obj = ast.instance.evaluate()

                if (obj !is LoxInstance) {
                    throw RuntimeException("Only instances have fields.")
                }

                val value = ast.value.evaluate()
                obj[ast.name] = value
                return value
            }

            is Super -> {
                val distance = locals[ast]!!
                val superClass = environment.getAt(distance, "super") as LoxClass

                val obj = environment.getAt(distance - 1, "this") as LoxInstance

                val method = superClass.findMethod(ast.method.lexeme) ?: error("Undefined property '${ast.method.lexeme}'.")

                return method.bind(obj)
            }

            is This -> {
                lookUpVariable(ast.keyword, ast)
            }

            is Unary -> {
                unaryHandler[ast.operator.type]?.let {
                    it(ast.expr.evaluate()!!)
                } ?: error("Unhandled unary operator ${ast.operator}")
            }

            is Variable -> {
                lookUpVariable(ast.name, ast)
            }

            is Assign -> {
                val value = ast.expression.evaluate()

                this.locals[ast]?.let {
                    this.environment.assignAt(it, ast.name, value)
                } ?: run {
                    this.globals[ast.name] = value
                }

                value
            }
        }
    }

    override fun visit(ast: StmtAST) {
        when (ast) {
            is Block -> {
                executeBlock(ast.statements, Environment(environment))
            }

            is Class -> {
                val superClass = ast.superClass?.let { sc ->
                    sc.evaluate().let {
                        if (it !is LoxClass) {
                            error("Superclass must be a class")
                        } else {
                            it
                        }
                    }
                }

                environment[ast.name.lexeme] = null

                val previousEnvironment = ast.superClass?.let {
                    val enclosing = environment
                    environment = Environment(environment)
                    environment["super"] = superClass
                    enclosing
                }

                val methods = buildMap {
                    ast.methods.forEach {
                        val function = LoxFunction(it, environment, it.name.lexeme == "init")
                        put(it.name.lexeme, function)
                    }
                }

                val klass = LoxClass(ast.name.lexeme, superClass, methods)

                previousEnvironment?.let {
                    environment = it
                }

                environment[ast.name] = klass
            }

            is Expression -> {
                ast.expr.evaluate()
            }

            is Function -> {
                val function = LoxFunction(ast, environment, false)
                environment[ast.name.lexeme] = function
            }

            is Print -> {
                ast.expr.evaluate().also(::println)
            }

            is VarStmt -> {
                environment[ast.name.lexeme] = ast.initializer?.evaluate()
            }

            is If -> {
                if (ast.condition.evaluate().isTruthy()) {
                    ast.trueBranch.execute()
                } else {
                    ast.falseBranch?.execute()
                }
            }

            is While -> {
                while (ast.condition.evaluate()!!.isTruthy()) {
                    ast.body.execute()
                }
            }

            is Return -> {
                val value = ast.value?.evaluate()

                throw LoxReturn(value)
            }
        }
    }
}