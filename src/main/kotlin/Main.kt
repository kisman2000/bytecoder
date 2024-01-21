import imgui.app.Application
import imgui.type.ImString
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.nio.charset.Charset
import java.util.Stack
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.io.path.name
import kotlin.io.path.writeText

//import kotlin.streams.toList
//import kotlin.stre/

val GUI = Gui()

var JAVA_CODE = ImString(1000)
var BYTE_CODE = ImString(1000)

val STRINGS_TO_REPLACE = listOf(
    "if",
    "else",
    "for",
    "while",
    "do",
    "(",
    ")"
)

/**
 * if(text.startsWith("MEOW") {
 *     lightmap = RenderLayers.DISABLE_LIGHTMAP;
 * }
 *
 *
 *
 */


//val PHANTOM_GENERATOR = Phantomg

fun main(
    args : Array<String>
) {
//    println("Hello World!")

    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
//    println("Program arguments: ${args.joinToString()}")

//    JPhantom

    Application.launch(GUI)
}

fun generatePhantoms() : Collection<Phantom> {
//    val javacode = JAVA_CODE.get()
//    val classNode = ClassNode
    val phantoms = hashSetOf<Phantom>()
    val localFields = mutableListOf<LocalField>()
    val fields = mutableMapOf<String, MutableMap<String, String>>()
    val createdTypeClasses = hashSetOf<String>()
    val javacode = JAVA_CODE.get().replace(Regex("\\s"), "").replace("\n", "").replace("{", "{;")
    val lines = javacode.split(";")

    var phantomClassCounter = 0
    var returnTypeCounter = 0
    var fieldTypeCounter = 0
    var subexpressionCounter = 0
    var stringCounter = 0
    var requiredRootType : String? = null
    var thisPhantomClass : String? = null
//    var strings

    for(line in lines) {
//        val line1 = line.removePrefix(Regex("\\s"))
        val line1 = line.replace(Regex("\\s"), "")

        fun parseExpression(
            expression0 : String
        ) {
            var expression = expression0
            //            println("expression $expression")

            //TODO: chars

//            val beginQuoteStack = Stack<Int>()
            var beginQuote = -1
            val quotes = mutableMapOf<Int, Int>()
//            var state = false

            for((index, char) in expression.toCharArray().withIndex()) {
                if(char == '"') {
                    println(char)

                    if(beginQuote == -1) {
                        beginQuote = index
                    } else {
                        quotes[beginQuote] = index
                        beginQuote = -1
                    }
                    /*if(state) {
                        quotes[beginQuoteStack.pop()] = index
                    } else {
                        beginQuoteStack.pop

                        state = true
                    }*/
                }
            }

            val strings = mutableMapOf<String, String>()

            for((start, end) in quotes) {
                val string = expression.substring(start..end)

                stringCounter++
//                println(">>> $string")

//                expression = expression.replace(string, "STRING$stringCounter")

                strings[string] = "STRING$stringCounter"
            }

            for((toReplace, replacement) in strings) {
                expression = expression.replace(toReplace, replacement)
            }


            val circleBracketStack = Stack<Int>()
            val squareBracketStack = Stack<Int>()
            val brackets = mutableMapOf<Int, Int>()

            /*val beginCircleBrackets = Stack<Int>()
            val endCircleBrackets = Stack<Int>()

            val beginSquareBrackets = Stack<Int>()
            val endSquareBrackets = Stack<Int>()*/

            for((index, char) in expression.toCharArray().withIndex()) {
                when(char) {
                    '(' -> circleBracketStack.add(index)
                    ')' -> brackets[circleBracketStack.pop()] = index
                    /*'(' -> beginCircleBrackets.add(index)
                    ')' -> endCircleBrackets.add(index)
                    '[' -> beginSquareBrackets.add(index)
                    ']' -> endSquareBrackets.add(index)*/
                }
            }

//            println("expression $expression")
            val subexpressions = mutableMapOf<String, Expression>()

            brackets[-1] = expression.length

//            val datas = mutableMapOf<String, Expression?>()

//            for(i in 0..<beginCircleBrackets.size) {
            for((index, entry) in brackets.entries.withIndex()) {
//                val beginCircleBracket = beginCircleBrackets.pop() + 1
//                val endCircleBracket = endCircleBrackets.pop()
//                var subexpression = expression.substring(beginCircleBracket, endCircleBracket)
                val start = entry.key + 1
                val end = entry.value
                var substring = expression.substring(start, end)

                fun parseSubexpression(
                    subexpression0 : String
                ) : Expression {
                    var subexpression = subexpression0

//                    println(">> $subexpression")

                    subexpressionCounter++
                    val expressionLiteral = if(index > 0) {
                        val prev = subexpressions.entries.last().value
                        val string = prev.expressionString
                        val name = prev.name


//                    subexpression = subexpression.replace(prev, "SUBEXPRESSION$subexpressionCounter")
                        subexpression = subexpression.replace(string, name)

//                    "SUBEXPRESSION$subexpressionCounter"
                    } else {
//                    "<root>"
                    }

//                    println(subexpression)

                    val valueType = if(subexpression0.startsWith("STRING")) {
                        "Ljava/lang/String;"
                    } else {
                        null
                    }

                    val expression1 = Expression(subexpression, "SUBEXPRESSION$subexpressionCounter", valueType, emptyList())

//                subexpressions.add(subexpression)
                    subexpressions["SUBEXPRESSION$subexpressionCounter"] = expression1

                    return expression1
                }

                if(substring.contains(",")) {
                    val substring2 = if(substring.contains("(")) substring.substring(substring.indexOf("(") + 1, substring.indexOf(")")) else substring
                    val split = substring2.split(",")
                    var substring3 = substring

                    for(subexpression in split) {
                        val parsedExpression = parseSubexpression(subexpression)

                        substring3 = substring3.replace(parsedExpression.expressionString, parsedExpression.name)
                    }

                    parseSubexpression(substring3)
                } else {
                    parseSubexpression(substring)
                }

//                println("$beginCircleBracket $endCircleBracket $subexpression")

//                println(subexpression)


            }

//            val datas = mutableListOf<ExpressionData>()

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
                    /*phantomClassCounter++

                    phantoms.add(Phantom("PhantomClass$phantomClassCounter", Types.Class, descriptor = className))
                    shouldBeStatic = true


                    "PhantomClass$phantomClassCounter"*/

                    phantoms.add(Phantom(className, Types.Class))

                    className
                }

                /*if(!className.contains("this")) {
//                    println(className)
                    phantoms.add(Phantom(className, Types.Class))
                }*/

//                println("< $string")

                /*phantomClassCounter++

                phantoms.add(Phantom("PhantomClass$phantomClassCounter", Types.Class))*/

//                if(phantomClassCounter == 10) {
//                    println("phc10 $string")
//                }

//                var owner = "PhantomClass$phantomClassCounter"//className
                var prevCall = calls[0]

                for((j, call) in calls.toMutableList().also { it.removeFirst() }.withIndex()) {
                    val name : String
                    val type : Types
                    val typeClass : String
                    val valueType = if(j == calls.size - 2 && i == subexpressions.size - 1) requiredRootType else null

                    println("$call $valueType $requiredRootType $j $i ${calls.size - 2} ${subexpressions.size - 1}")

                    if(call.contains("SUBEXPRESSION")) {
                        if(valueType != null) {
                            returnTypeCounter++
                        }

                        val regex = Regex("SUBEXPRESSION[0-9]+")

                        val subexpressions0 = regex.findAll(call).map { it.value }
                        val params = mutableListOf<String>()

                        for(subexpression0 in subexpressions0) {
                            val subexpression1 = subexpressions[subexpression0]!!

//                            println("${subexpression1.name}")

                            params.add(subexpression1.valueType!!)

//                            println(">>>> ${subexpression1.valueType}")
                        }

//                        val subexpressions = call.regionMatches()

                        phantoms.add(Phantom(call.replace(regex, "").replace("(", "").replace(")", "").replace(",", ""), Types.Method, owner, "(${params.joinToString("")})${valueType ?: "LReturnTypeClass$returnTypeCounter;"}", shouldBeStatic))

//                        println("> > $call $owner")

                        name = call.replace(Regex("(SUBEXPRESSION[0-9]+)"), "")
                        type = Types.Method
                        typeClass = "LReturnTypeClass$returnTypeCounter;"
                    } else {
                        if(fields.contains(owner)) {
                            val mappings = fields[owner]!!

                            if(mappings.contains(call)) {
                                typeClass = mappings[call]!!

                                println("creating for $call 3")

//                                typeClass = null
                            } else {
                                if(valueType == null) {
                                    fieldTypeCounter++
                                    typeClass = "LFieldTypeClass$fieldTypeCounter;"
                                    mappings[call] = typeClass
                                } else {
                                    typeClass = valueType
                                }

                                println("creating for $call 1")

                                phantoms.add(Phantom(call, Types.Field, "L$owner;", valueType ?: "LFieldTypeClass$fieldTypeCounter;", shouldBeStatic))
                            }
                        } else {
                            if(valueType == null) {
                                fieldTypeCounter++
                                typeClass = "LFieldTypeClass$fieldTypeCounter;"
                                fields[owner!!] = mutableMapOf(call to typeClass)
                            } else {
                                typeClass = valueType
                            }

                            println("creating for $call 2")


                            phantoms.add(Phantom(call, Types.Field, "L$owner;", valueType ?: "LFieldTypeClass$fieldTypeCounter;", shouldBeStatic))
                        }

                        /*println("miow $valueType $call")
                        if(valueType == null) {
//                            println("miow!!?!?!?>!>!>!>!")

                            fieldTypeCounter++
                        }

                        name = call
                        type = Types.Field
                        typeClass = "LFieldTypeClass$fieldTypeCounter;"

//                        println("field $call $owner")

                        phantoms.add(Phantom(call, Types.Field, "L$owner;", valueType ?: "LFieldTypeClass$fieldTypeCounter;", shouldBeStatic))*/
                    }

                    if(!createdTypeClasses.contains(typeClass) && typeClass != valueType) {
                        phantoms.add(Phantom(typeClass.removePrefix("L").removeSuffix(";"), Types.Class))
                    }

                    owner = valueType ?: typeClass
                    shouldBeStatic = false

                    if(subexpression.valueType == null) {
//                        println(" ${subexpression.name} $owner")

                        subexpression.valueType = owner
                    }
//                    phantoms.add(Phantom(name, type, typeClass))

//                    println("$name $type $typeClass")

                    prevCall = call
                }

//                println(">>>>> ${subexpression.name} $owner")

            }
//            println("subexpressions ${subexpressions.joinToString()}")
        }

        val split = line.split(Regex("\\s")).stream().filter { it.isNotBlank() }.toArray()// as Array<String>//.toList()

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

        if(true/*!line.contains("}")*/) {
            val line2 = if(line1.startsWith("if")/* || line1.startsWith("for") || line1.startsWith("while")*/) {
                requiredRootType = "Z"
                println("meow")
                line1.removePrefix("if(").removeSuffix("){")


    //            println("subexpressions ${subexpressions.joinToString()}")

                /*val split1 = expression.split(Regex("[^a-zA-z123456789_$.]"))

                for(callPath in split1) {
                    val split2 = callPath.split(".")
                    val className = split2[0] // if we are calling fields of this class we should use "this"

                    for(call in calls)
                }*/
            } else {
                line1
            }

            val assignmentExpressions = line2.split(Regex("[+=*/]*="))

            //TODO: rewrite it
            val expressions = line2.split(Regex("[+\\-*/=&|]"))

            if(assignmentExpressions[0] == expressions[0] && expressions.size == 2) {
                parseExpression(expressions[0])

                val lastPhantom = phantoms.last { it.type == Types.Field }

                val valueType = if(lastPhantom.type == Types.Method) {
                    lastPhantom.descriptor!!.split(")")[1]
                } else {
                    lastPhantom.descriptor
                }

                println(">>> $valueType ${expressions[1]} ${lastPhantom.type}")

                requiredRootType = valueType

                println("$requiredRootType")

                parseExpression(expressions[1])
            } else {
                parseExpression(line2)

//                for(expression in expressions) {
//                    if(expression.isNotEmpty()) {
//                        parseExpression(expression)
//                    }
//                }
            }

        }

        requiredRootType = null
    }

//    for(phantom in )

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

            classNode.visitMethod(
                Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
                "<cinit>",
                "()V",
                null,
                emptyArray()
            )

            classNodes["L${className.removePrefix("L").removeSuffix(";")};"] = classNode

//            println(className)

//            classNode.name = className
        }
    }

    for(phantom in phantoms) {
        val name = phantom.name
        val owner = phantom.owner

        println("searching $name $owner")
        if(owner != null) {
            println("searching $name $owner 2")
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
//                            "L${phantom.fieldType!!};",
                            phantom.descriptor!!,
                            null,
                            null
                        )
                    }
                    Types.Method -> {
                        val methodNode = classNode.visitMethod(
                            access,
                            name,
//                            "()${phantom.returnType!!}",
                            phantom.descriptor!!,
//                            "()V",
                            null,
                            emptyArray()
                        )

//                        println(phantom.descriptor)

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
//                                    "B" -> Opcodes.
                                }

                                opcode2 = when(returnType) {
                                    "I", "Z" -> Opcodes.IRETURN
                                    "L" -> Opcodes.LRETURN
                                    "F" -> Opcodes.FRETURN
                                    "D" -> Opcodes.DRETURN
                                    else -> -1
                                }

                                /*if(opcode1 != -1 && opcode2 != -1) {
                                    methodNode.visitInsn(opcode)

                                    println("writing opcode xd")
                                }*/
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

//                println(name)
            }
        }
    }

    val file = kotlin.io.path.createTempFile(prefix = "bytecodertemplib", suffix = ".jar").toFile()
//    val zis = ZipInputStream(file.inputStream())
    val jos = JarOutputStream(file.outputStream())

    for((className, classNode) in classNodes) {
        val bytes = write(classNode)

        val entry = JarEntry("${className.removePrefix("L").removeSuffix(";")}.class")

//        println("writing ${className.removePrefix("L").removeSuffix(";")}.class")

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

    /*var javaCode = code

    for(phantom in phantoms) {
        if(phantom.type == Types.Class) {

        }
    }*/

    code += " {\n\tpublic $className() {\n\t\tsuper();\n\t}\n\n\tpublic static void main(String... args) {\n${JAVA_CODE.get().split("\n").joinToString("\n") { "\t\t${it.replace("this.", "")}" }}\n\t}\n}"

    BYTE_CODE.set(code)

    file.writeText(code)

    return file.toFile()
}

fun compileJavaFile(
    code : File,
    library : File
) : File {
    val command = "javac -cp ${library.absolutePath} -d /tmp/bytecoder/classes/ ${code.absolutePath} -Xdiags:verbose"
    val process = Runtime.getRuntime().exec(command)

    while(process.isAlive) {

    }

    println(process.errorStream.readBytes().toString(Charset.defaultCharset()))

    val classFile = File("/tmp/bytecoder/classes/${code.name.replace(".java", ".class")}")

    return classFile

//    return File("/tmp/bytecoder/classes/")
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

fun funny() {
    val phantoms = generatePhantoms()
    val library = buildLibrary(phantoms)
    val code = buildJavaFile(phantoms)
    val clazz = compileJavaFile(code, library)
    val instructions = generateInstructions(clazz)

    BYTE_CODE.set(instructions)

//    val first = phantoms.find { it.name == "ThisPhantomClass" }

//    println("first ${first?.name}")

    println(library.absoluteFile)
    println(code.absoluteFile)

//    BYTE_CODE.set(code)
}

fun generateNodes(
    phantoms : Collection<Phantom>
) {

}
