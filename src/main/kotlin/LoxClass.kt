data class LoxClass(private val name: String, private val superClass: LoxClass?, private val methods: Map<String, LoxFunction>) : LoxCallable {
    override val arity: Int
        get() {
            val initializer = findMethod("init")
            return initializer?.arity ?: 0
        }

    override fun invoke(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val instance = LoxInstance(this)
        val initializer = findMethod("init")
        initializer?.bind(instance)?.let { it(interpreter, arguments) }

        return instance
    }

    override fun toString(): String {
        return "LoxClass($name)"
    }

    fun findMethod(name: String): LoxFunction? {
        return methods[name] ?: superClass?.findMethod(name)
    }
}