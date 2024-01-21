import imgui.ImGui
import imgui.app.Application
import imgui.app.Configuration
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiInputTextFlags
import imgui.flag.ImGuiWindowFlags
import imgui.type.ImString
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.nio.charset.Charset
import java.util.Stack
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.io.path.name
import kotlin.io.path.writeText

private val JAVA_CODE = ImString(1000)
private val BYTE_CODE = ImString(10000)

fun main() {
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

    fun generatePhantoms() : Collection<Phantom> {
        val phantoms = hashSetOf<Phantom>()
        val localFields = mutableListOf<LocalField>()
        val fields = mutableMapOf<String, MutableMap<String, String>>()
        val createdTypeClasses = hashSetOf<String>()
        val cachedExpressions = hashMapOf<String, Expression>()
        val javacode = JAVA_CODE.get().replace(Regex("\\s"), "").replace("\n", "").replace("{", "{;")
        val lines = javacode.split(";")

        var returnTypeCounter = 0
        var fieldTypeCounter = 0
        var subexpressionCounter = 0
        var stringCounter = 0
        var requiredRootType : String? = null
        var thisPhantomClass : String? = null

        for(line in lines) {
            val line1 = line.replace(Regex("\\s"), "")

            fun parseExpression(
                expression0 : String
            ) {
                var expression = expression0

                //TODO: chars

                var beginQuote = -1
                val quotes = mutableMapOf<Int, Int>()

                for((index, char) in expression.toCharArray().withIndex()) {
                    if(char == '"') {
                        println(char)

                        if(beginQuote == -1) {
                            beginQuote = index
                        } else {
                            quotes[beginQuote] = index
                            beginQuote = -1
                        }
                    }
                }

                val strings = mutableMapOf<String, String>()

                for((start, end) in quotes) {
                    val string = expression.substring(start..end)

                    stringCounter++

                    strings[string] = "STRING$stringCounter"
                }

                for((toReplace, replacement) in strings) {
                    expression = expression.replace(toReplace, replacement)
                }

                val circleBracketStack = Stack<Int>()
                val squareBracketStack = Stack<Int>()
                val brackets = mutableMapOf<Int, Int>()

                for((index, char) in expression.toCharArray().withIndex()) {
                    when(char) {
                        '(' -> circleBracketStack.add(index)
                        ')' -> brackets[circleBracketStack.pop()] = index
//                        '[' -> squareBracketStack.add(index)
//                        ']' -> brackets[squareBracketStack.pop()] = index
                    }
                }

                val subexpressions = mutableMapOf<String, Expression>()

                brackets[-1] = expression.length

                for((index, entry) in brackets.entries.withIndex()) {
                    val start = entry.key + 1
                    val end = entry.value
                    var substring = expression.substring(start, end)

                    fun parseSubexpression(
                        subexpression0 : String
                    ) : Expression {
                        var subexpression = subexpression0

                        return if(subexpression.contains("STRING")) {
                            if(cachedExpressions.contains(subexpression)) {
                                cachedExpressions[subexpression]!!
                            } else {
                                subexpressionCounter++

                                val stringExpression = Expression(subexpression, "SUBEXPRESSION$subexpressionCounter", "Ljava/lang/String;")

                                cachedExpressions[subexpression] = stringExpression
                                subexpressions["SUBEXPRESSION$subexpressionCounter"] = stringExpression

                                stringExpression
                            }
                        } else if(cachedExpressions.contains(subexpression)) {
                            cachedExpressions[subexpression]!!
                        } else {
                            subexpressionCounter++
                            if(subexpressionCounter == 4) {
                                println(">>> $subexpression")
                            }
                            if(subexpressionCounter == 3) {
                                println(">>>> $subexpression")
                            }
                            if(index > 0) {
                                val prev = subexpressions.entries.last().value
                                val string = prev.expressionString
                                val name = prev.name

                                subexpression = subexpression.replace(string, name)
                            }

                            val expression1 = Expression(subexpression, "SUBEXPRESSION$subexpressionCounter", null)

                            cachedExpressions[subexpression] = expression1
                            subexpressions["SUBEXPRESSION$subexpressionCounter"] = expression1

                            expression1
                        }

                        /*subexpressionCounter++
                        if(index > 0) {
                            val prev = subexpressions.entries.last().value
                            val string = prev.expressionString
                            val name = prev.name

                            subexpression = subexpression.replace(string, name)
                        }

                        val valueType = if(subexpression0.startsWith("STRING")) {
                            "Ljava/lang/String;"
                        } else {
                            null
                        }

                        val expression1 = Expression(subexpression, "SUBEXPRESSION$subexpressionCounter", valueType)

                        subexpressions["SUBEXPRESSION$subexpressionCounter"] = expression1

                        return expression1*/
                    }

                    if(substring.contains(",")) {
                        val substring2 = if(substring.contains("(")) substring.substring(substring.indexOf("(") + 1, substring.indexOf(")")) else substring
                        val split = substring2.split(",")
                        var substring3 = substring


                        for(subexpression in split) {
                            val parsedExpression = parseSubexpression(subexpression)

                            println("> parsing $subexpression ${parsedExpression.expressionString} ${parsedExpression.name}")

                            substring3 = substring3.replace(parsedExpression.expressionString, parsedExpression.name)
                        }
                        if(substring.contains("(")) {
                            println(">> parsing $substring $substring3")

                            parseSubexpression(substring3)
                        }
                    } else {
                        println("parsing $substring")

                        parseSubexpression(substring)
                    }
                }

                for((i, entry) in subexpressions.entries.withIndex()) {
                    val subexpression = entry.value
                    val string = subexpression.expressionString
                    val calls = string.split(".")
                    val className = calls[0]
                    var shouldBeStatic = true

                    var owner = if(className == "this") {
                        if(thisPhantomClass == null) {
                            thisPhantomClass = "ThisPhantomClass"

                            phantoms.add(Phantom(thisPhantomClass!!, Types.Class))
                        }

                        thisPhantomClass
                    } else {
                        phantoms.add(Phantom(className, Types.Class))

                        println("> creating ${entry.key} $className $string ${subexpression.name}")

                        className
                    }

                    for((j, call) in calls.toMutableList().also { it.removeFirst() }.withIndex()) {
                        val name : String
                        val type : Types
                        val typeClass : String
                        val valueType = if(j == calls.size - 2 && i == subexpressions.size - 1) requiredRootType else null

                        println("< $call $valueType $requiredRootType $j $i ${calls.size - 2} ${subexpressions.size - 1}")

                        if(call.contains("SUBEXPRESSION")) {
                            if(valueType != null) {
                                returnTypeCounter++
                            }

                            val regex = Regex("SUBEXPRESSION[0-9]+")

                            val subexpressions0 = regex.findAll(call).map { it.value }
                            val params = mutableListOf<String>()

                            for(subexpression0 in subexpressions0) {
                                val subexpression1 = subexpressions[subexpression0]!!

                                params.add(subexpression1.valueType!!)
                            }

                            phantoms.add(Phantom(call.replace(regex, "").replace("(", "").replace(")", "").replace(",", ""), Types.Method, owner, "(${params.joinToString("")})${valueType ?: "LReturnTypeClass$returnTypeCounter;"}", shouldBeStatic))

                            name = call.replace(Regex("(SUBEXPRESSION[0-9]+)"), "")
                            type = Types.Method
                            typeClass = "LReturnTypeClass$returnTypeCounter;"
                        } else {
                            if(fields.contains(owner)) {
                                val mappings = fields[owner]!!

                                if(mappings.contains(call)) {
                                    typeClass = mappings[call]!!
                                } else {
                                    if(valueType == null) {
                                        fieldTypeCounter++
                                        typeClass = "LFieldTypeClass$fieldTypeCounter;"
                                        mappings[call] = typeClass
                                    } else {
                                        typeClass = valueType
                                    }

                                    println("adding 1 $call $typeClass")

                                    phantoms.add(Phantom(call, Types.Field, "L$owner;", typeClass/*valueType ?: "LFieldTypeClass$fieldTypeCounter;"*/, shouldBeStatic))
                                }
                            } else {
                                if(valueType == null) {
                                    fieldTypeCounter++
                                    typeClass = "LFieldTypeClass$fieldTypeCounter;"
                                    fields[owner!!] = mutableMapOf(call to typeClass)
                                } else {
                                    typeClass = valueType
                                }

                                println("adding 2 $call $typeClass")

                                phantoms.add(Phantom(call, Types.Field, "L$owner;", typeClass/*valueType ?: "LFieldTypeClass$fieldTypeCounter;"*/, shouldBeStatic))
                            }
                        }

                        if(!createdTypeClasses.contains(typeClass) && typeClass != valueType) {
                            phantoms.add(Phantom(typeClass.removePrefix("L").removeSuffix(";"), Types.Class))

                            createdTypeClasses.add(typeClass)

                            println("creating $call $typeClass $valueType 1")
                        }

                        owner = valueType ?: typeClass
                        shouldBeStatic = false

                        if(subexpression.valueType == null) {
                            subexpression.valueType = owner
                        }
                    }
                }
            }

            val split = line.split(Regex("\\s")).stream().filter { it.isNotBlank() }.toArray()

            //TODO: wtf?
            if(split.size == 2 && !line.contains("\"")) {
                val name = split[1] as String
                val type = split[0] as String
                val localField = LocalField(name, type)

                localFields.add(localField)

                if(!localField.primitive) {
                    phantoms.add(Phantom(type, Types.Class))
                }
            }

            if(!line1.contains("}")) {
                val line2 = if(line1.startsWith("if")) {
                    requiredRootType = "Z"
                    println("meow")
                    line1.removePrefix("if(").removeSuffix("){")
                } else {
                    line1
                }

                val assignmentExpressions = line2.split(Regex("[+=*/]*="))

                //TODO: rewrite it
                val expressions = line2.split(Regex("[+\\-*/=&|]"))

                if(assignmentExpressions[0] == expressions[0] && expressions.size == 2) {
                    parseExpression(expressions[0])

                    val fieldName = expressions[0].split(".").last()
                    val fieldPhantom = phantoms.last { it.type == Types.Field && it.name == fieldName }
                    val fieldType = fieldPhantom.descriptor!!

                    /*val valueType = if(lastPhantom.type == Types.Method) {
                        lastPhantom.descriptor!!.split(")")[1]
                    } else {
                        lastPhantom.descriptor
                    }*/

                    println(">>> $fieldType ${expressions[1]} ${fieldPhantom.name}")

                    requiredRootType = fieldType

                    println("<<< $requiredRootType")

                    parseExpression(expressions[1])
                } else {
                    parseExpression(line2)
                }
            }

            requiredRootType = null
        }

        return phantoms
    }

    fun buildLibrary(
        phantoms : Collection<Phantom>
    ) : File {
        val classNodes = mutableMapOf<String, ClassNode>()

        for(phantom in phantoms) {
            if(phantom.type == Types.Class) {
                val className = phantom.name
                val classNode = ClassNode()

                classNode.visit(
                    61,
                    Opcodes.ACC_PUBLIC,
                    className.removePrefix("L").removeSuffix(";"),
                    null,
                    null,
                    emptyArray()
                )

                classNode.visitMethod(
                    Opcodes.ACC_PUBLIC,
                    "<init>",
                    "()V",
                    null,
                    emptyArray()
                )

                /*classNode.visitMethod(
                    Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
                    "<cinit>",
                    "()V",
                    null,
                    emptyArray()
                )*/

                classNodes["L${className.removePrefix("L").removeSuffix(";")};"] = classNode
            }
        }

        for(phantom in phantoms) {
            val name = phantom.name
            val owner = phantom.owner

            if(owner != null) {
                val classNode = classNodes[owner]

                if(classNode != null) {
                    val access = if(phantom.static) {
                        Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC
                    } else {
                        Opcodes.ACC_PUBLIC
                    }

                    when(phantom.type) {
                        Types.Field -> {
                            classNode.visitField(
                                access,
                                name,
                                phantom.descriptor!!,
                                null,
                                null
                            )
                        }
                        Types.Method -> {
                            val methodNode = classNode.visitMethod(
                                access,
                                name,
                                phantom.descriptor!!,
                                null,
                                emptyArray()
                            )

                            val returnType = phantom.descriptor.split(")")[1]

                            if(returnType != "V") {
                                var opcode1 : Int
                                var opcode2 : Int

                                if(returnType.contains("L")) {
                                    opcode1 = Opcodes.ACONST_NULL
                                    opcode2 = Opcodes.ARETURN
                                } else {
                                    opcode1 = when(returnType) {
                                        "I", "Z" -> Opcodes.ICONST_0
                                        "L" -> Opcodes.LCONST_0
                                        "F" -> Opcodes.FCONST_0
                                        "D" -> Opcodes.DCONST_0
                                        else -> -1
                                    }

                                    opcode2 = when(returnType) {
                                        "I", "Z" -> Opcodes.IRETURN
                                        "L" -> Opcodes.LRETURN
                                        "F" -> Opcodes.FRETURN
                                        "D" -> Opcodes.DRETURN
                                        else -> -1
                                    }
                                }

                                if(opcode1 != -1) {
                                    methodNode.visitInsn(opcode1)
                                }

                                if(opcode2 != -1) {
                                    methodNode.visitInsn(opcode2)
                                }
                            }
                        }
                        else -> { }
                    }
                }
            }
        }

        val file = kotlin.io.path.createTempFile(prefix = "bytecodertemplib", suffix = ".jar").toFile()
        val jos = JarOutputStream(file.outputStream())

        for((className, classNode) in classNodes) {
            val bytes = write(classNode)
            val entry = JarEntry("${className.removePrefix("L").removeSuffix(";")}.class")

            jos.putNextEntry(entry)
            jos.write(bytes)
            jos.closeEntry()
        }

        jos.close()

        return file
    }

    fun buildJavaFile(
        phantoms : Collection<Phantom>
    ) : File {
        val file = kotlin.io.path.createTempFile("bytecodertempcodetocompl", ".java")
        val className = file.name.removeSuffix(".java")
        var code = "public class $className"
        val shouldExtendThis = phantoms.find { it.name == "ThisPhantomClass" } != null

        if(shouldExtendThis) {
            code += " extends ThisPhantomClass"
        }

        code += " {\n\tpublic $className() {\n\t\tsuper();\n\t}\n\n\tpublic static void main(String... args) {\n${JAVA_CODE.get().split("\n").joinToString("\n") { "\t\t${it.replace("this.", "")}" }}\n\t}\n}"

        file.writeText(code)

        return file.toFile()
    }

    @Suppress("ControlFlowWithEmptyBody")
    fun compileJavaFile(
        code : File,
        library : File
    ) : File {
        val command = "javac -cp ${library.absolutePath} -d /tmp/bytecoder/classes/ ${code.absolutePath} -Xdiags:verbose"
        val process = Runtime.getRuntime().exec(command)

        while(process.isAlive) { }

        println(process.errorStream.readBytes().toString(Charset.defaultCharset()))

        return File("/tmp/bytecoder/classes/${code.name.replace(".java", ".class")}")
    }

    fun generateInstructions(
        clazz : File
    ) : String {
        val classNode = read(clazz.readBytes())
        var instructions = ""

        for(methodNode in classNode.methods) {
            if(methodNode.name == "main") {
                for(instruction in methodNode.instructions) {
                    instructions += "${opcodeName(instruction.opcode)}\n"
                }
            }
        }

        return instructions
    }

    fun javacode2bytecode() {
        val phantoms = generatePhantoms()
        val library = buildLibrary(phantoms)
        val code = buildJavaFile(phantoms)
        val clazz = compileJavaFile(code, library)
        val instructions = generateInstructions(clazz)

        BYTE_CODE.set(instructions)

        //TODO: rewrite it
        println(library.absoluteFile)
        println(code.absoluteFile)
    }

    val gui = object : Application() {
        override fun configure(
            config : Configuration
        ) {
            colorBg.set(0f, 0f, 0f, 0f)

            config.title = "bytecoder"
            config.width = 515
            config.height = 690

            GLFWErrorCallback.createPrint(System.err).set()

            if(!GLFW.glfwInit()) {
                throw IllegalStateException("Unable to initialize GLFW")
            }

            GLFW.glfwWindowHint(GLFW.GLFW_TRANSPARENT_FRAMEBUFFER, 1)
            GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, 0)
        }

        override fun process() {
            ImGui.styleColorsDark()

            val style = ImGui.getStyle()

            style.frameBorderSize = 1f

            val window = style.getColor(ImGuiCol.WindowBg)

            style.setColor(ImGuiCol.WindowBg, window.x, window.y, window.z, 0.6f)

            val frame = style.getColor(ImGuiCol.FrameBg)

            style.setColor(ImGuiCol.FrameBg, frame.x, frame.y, frame.z, frame.w / 2f)

            ImGui.pushStyleColor(ImGuiCol.ChildBg, frame.x, frame.y, frame.z, frame.w / 2f)

            ImGui.begin("bytecoder", ImGuiWindowFlags.NoResize or ImGuiWindowFlags.NoDecoration)

            val w = intArrayOf(1)
            val h = intArrayOf(1)

            GLFW.glfwGetWindowSize(handle, w, h)

            ImGui.setWindowPos(0f, 0f)
            ImGui.setWindowSize(w[0].toFloat(), h[0].toFloat())

            ImGui.text("Java code")
            ImGui.inputTextMultiline("## Java code", JAVA_CODE, 500f, 300f, ImGuiInputTextFlags.AllowTabInput)

            ImGui.text("Byte code")
            ImGui.inputTextMultiline("## Byte code", BYTE_CODE, 500f, 300f, ImGuiInputTextFlags.ReadOnly)

            if(ImGui.button("Convert to byte code")) {
                javacode2bytecode()
            }

//            ImGui.button("Convert to java objectweb2 code")

            ImGui.end()
            ImGui.popStyleColor()
        }
    }

    Application.launch(gui)
}

class Phantom(
    val name : String,
    val type : Types,
    val owner : String? = null,
    val descriptor : String? = null,
    val static : Boolean = false
) {
    override fun equals(
        other : Any?
    ) : Boolean {
        if(this === other) return true
        if(javaClass != other?.javaClass) return false

        other as Phantom

        return !(name != other.name || type != other.type)
    }

    override fun hashCode() = 31 * name.hashCode() + type.hashCode()
}

class Expression(
    val expressionString : String,
    val name : String,
    var valueType : String? = null
)

class LocalField(
    val name : String,
    type : String
) {
    val primitive = type == "int" || type == "short" || type == "byte" || type == "double" || type == "float" || type == "load" || type == "boolean"
}

enum class Types {
    Class,
    Field,
    Method
}