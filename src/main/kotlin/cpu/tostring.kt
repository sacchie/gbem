package cpu

fun objToStringHex(obj: Any): String {
    val fields = obj::class.java.declaredFields
    val s = fields.joinToString(", ") {
        // TODO: fieldを破壊的に変更していて怪しい
        it.isAccessible = true
        val v = it.get(obj)
        if (v is Int) {
            "${it.name}=0x${v.toString(16).uppercase()}"
        } else {
            "${it.name}=${v}"
        }

    }
    return "${obj::class.java.simpleName}($s)"
}
