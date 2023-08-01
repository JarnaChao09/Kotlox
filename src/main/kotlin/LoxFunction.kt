data class LoxFunction(private val declaration: Function) : LoxCallable {
    override val arity: Int
        get() = this.declaration.params.size

    override fun invoke(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val env = Environment(interpreter.globals)
        arguments.forEachIndexed { i, v ->
            env[declaration.params[i].lexeme] = v
        }

        interpreter.executeBlock(declaration.body, env)
        return null
    }
}