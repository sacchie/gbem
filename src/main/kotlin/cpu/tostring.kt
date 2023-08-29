package cpu

fun objToStringHex(obj: Any): String {
    val fields = obj::class.java.declaredFields
    val s = fields.joinToString(", ") {
        // fieldを破壊的に変更していて怪しいけど、Java的には仕方ないらしい
        // getDeclaredFields()で返るインスタンスが毎回別になっている模様（要出典）なので、isAccessibleを書き換えてもこの関数に影響は閉じるはず
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
