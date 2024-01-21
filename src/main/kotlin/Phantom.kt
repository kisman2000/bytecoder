class Phantom(
    val name : String,
    val type : Types,
    val owner : String? = null,
    val descriptor : String? = null,
    val static : Boolean = false
//    val fieldType : String? = null,
//    val returnType : String? = null
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