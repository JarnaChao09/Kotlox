import java.nio.charset.Charset
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.readBytes
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size > 1) {
        println("Usage: kotlox [flags] [script]")
        exitProcess(64)
    } else if (args.size == 1) {
        Path(args[0]).eval()
    } else {
        runREPL()
    }
}

fun Path.eval() = this.readBytes().toString(Charset.defaultCharset()).eval().run {
    if (this) {
        exitProcess(65)
    }
}

fun runREPL() {
    while (true) {
        print(">>> ")
        readlnOrNull()?.let {
            if (it == ":q") null else it
        }?.eval() ?: break
    }
}

fun String.eval(): Boolean {
    val lexer = Lexer(this)
    val parser = Parser(lexer.tokens)
    val ast = parser.parse()

    Interpreter.interpret(ast)

    return false
}