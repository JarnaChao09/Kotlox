class Environment(
    private val enclosing: Environment? = null,
    private val env: MutableMap<String, Any?> = mutableMapOf(),
) {
    operator fun set(name: String, value: Any?) {
        this.env[name] = value
    }

    operator fun set(name: Token, value: Any?) {
        if (name.lexeme in this.env) {
            this.env[name.lexeme] = value
        } else {
            this.enclosing?.set(name, value)
                ?: throw RuntimeException("Undefined variable '${name.lexeme}' @ ${name.line}")
        }
    }

    operator fun get(name: Token): Any? {
        return this.env.getOrElse(name.lexeme) {
            this.enclosing?.get(name) ?: throw RuntimeException("Undefined variable '${name.lexeme}' @ ${name.line}")
        }
    }
}