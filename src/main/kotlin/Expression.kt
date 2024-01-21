class Expression(
    val expressionString : String,
    val name : String,
    var valueType : String? = null,
    val childExpressions : Collection<Expression>
) {
}