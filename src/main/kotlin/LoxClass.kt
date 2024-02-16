data class LoxClass(val name: String, val methods: Map<String, LoxFunction>) : LoxCallable {
    override val arity: Int
        get() {
            val initializer = methods["init"]
            return initializer?.arity ?: 0
        }

    override fun invoke(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val instance = LoxInstance(this)
        val initializer = methods["init"]
        initializer?.bind(instance)?.let { it(interpreter, arguments) }

        return instance
    }

    override fun toString(): String {
        return "LoxClass($name)"
    }
}