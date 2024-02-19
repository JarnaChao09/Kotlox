data class LoxInstance(val klass: LoxClass) {
    private val fields: MutableMap<String, Any?> = mutableMapOf()

    operator fun get(name: Token): Any? {
        return fields[name.lexeme] ?: run {
            klass.findMethod(name.lexeme)?.bind(this) ?: RuntimeException("Undefined property '${name.lexeme}'.")
        }
    }

    operator fun set(name: Token, value: Any?) {
        fields[name.lexeme] = value
    }
}