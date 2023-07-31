object Interpreter : ExprAST.Visitor<Any?>, StmtAST.Visitor<Unit> {
    private val globals: Environment = Environment()
    private var environment: Environment = globals

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

            is Unary -> {
                unaryHandler[ast.operator.type]?.let {
                    it(ast.expr.evaluate()!!)
                } ?: error("Unhandled unary operator ${ast.operator}")
            }

            is Variable -> {
                environment[ast.name]
            }

            is Assign -> {
                ast.expression.evaluate().also {
                    environment[ast.name] = it
                }
            }
        }
    }

    override fun visit(ast: StmtAST) {
        when (ast) {
            is Expression -> {
                ast.expr.evaluate()
            }

            is Print -> {
                ast.expr.evaluate().also(::println)
            }

            is VarStmt -> {
                environment[ast.name.lexeme] = ast.initializer?.evaluate()
            }

            is Block -> {
                val previous = this.environment
                try {
                    this.environment = Environment(this.environment)

                    ast.statements.forEach {
                        it?.execute()
                    }
                } finally {
                    this.environment = previous
                }
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
        }
    }
}