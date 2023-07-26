class Environment(private val env: MutableMap<String, Any?> = mutableMapOf()) {
    operator fun set(name: String, value: Any?) {
        this.env[name] = value
    }

    operator fun set(name: Token, value: Any?) {
        if (name.lexeme in this.env) {
            this.env[name.lexeme] = value
        } else {
            throw RuntimeException("Undefined variable '${name.lexeme}' @ ${name.line}")
        }
    }

    operator fun get(name: Token): Any? {
        return this.env.getOrElse(name.lexeme) {
            throw RuntimeException("Undefined variable '${name.lexeme}' @ ${name.line}")
        }
    }
}