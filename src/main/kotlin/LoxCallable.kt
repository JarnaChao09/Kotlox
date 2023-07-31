interface LoxCallable {
    val arity: Int
    operator fun invoke(interpreter: Interpreter, arguments: List<Any?>): Any?
}