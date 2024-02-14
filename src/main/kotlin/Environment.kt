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

    fun getAt(distance: Int, name: String): Any? {
        return this.ancestor(distance).env[name]
    }

    fun assignAt(distance: Int, name: Token, value: Any?) {
        this.ancestor(distance).env[name.lexeme] = value
    }

    fun ancestor(distance: Int): Environment {
        var env = this
        for (i in 0..<distance) {
            env = env.enclosing!!
        }

        return env
    }

    override fun toString(): String {
        return "${this.env} -> ${this.enclosing}"
    }
}