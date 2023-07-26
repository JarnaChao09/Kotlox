class Environment(private val env: MutableMap<String, Any?> = mutableMapOf()) {
    operator fun set(name: String, value: Any?) {
        this.env[name] = value
    }

    operator fun get(name: Token): Any? {
        return this.env.getOrElse(name.lexeme) {
            throw RuntimeException("Undefined variable '${name.lexeme}' @ ${name.line}")
        }
    }
}