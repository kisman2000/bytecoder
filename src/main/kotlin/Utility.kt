import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

fun sumFlags(
    flags : Array<Int>
) : Int {
    var result = 0

    for (flag in flags) {
        result = result or flag
    }

    return result
}

fun write(
    node : ClassNode,
    vararg flags : Int
) = ClassWriter(sumFlags(flags.toTypedArray())).also { node.accept(it) }.toByteArray()!!

fun read(
    bytes : ByteArray,
    visitor : ClassVisitor? = null,
    vararg flags : Int
) = ClassNode().also {
    ClassReader(bytes).accept(visitor ?: it, sumFlags(flags.toTypedArray()))
}

fun opcodeName(
    opcode : Int
) : String? {

    val fields = Opcodes::class.java.declaredFields

    for(field in fields) {
        if(field[null] == opcode) {
            return field.name
        }
    }

    return null
}