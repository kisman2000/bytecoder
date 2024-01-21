class LocalField(
    val name : String,
    val type : String
) {
    val primitive = type == "int" || type == "short" || type == "byte" || type == "double" || type == "float" || type == "load" || type == "boolean"
}