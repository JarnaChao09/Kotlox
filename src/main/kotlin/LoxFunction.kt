data class LoxFunction(private val declaration: Function, private val closure: Environment, private val isInitializer: Boolean) : LoxCallable {
    override val arity: Int
        get() = this.declaration.params.size

    fun bind(instance: LoxInstance): LoxFunction {
        val environment = Environment(this.closure)
        environment["this"] = instance
        return LoxFunction(declaration, environment, isInitializer)
    }

    override fun invoke(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val env = Environment(closure)
        arguments.forEachIndexed { i, v ->
            env[declaration.params[i].lexeme] = v
        }

        try {
            interpreter.executeBlock(declaration.body, env)
        } catch (returnValue: LoxReturn) {
            return if (isInitializer) closure.getAt(0, "this") else returnValue.returnValue
        }

        if (isInitializer) {
            return closure.getAt(0, "this")
        }

        return null
    }

    override fun toString(): String {
        return "<fn ${this.declaration.name.lexeme}>"
    }
}