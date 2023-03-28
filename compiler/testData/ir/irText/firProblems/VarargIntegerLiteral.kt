// FIR_IDENTICAL
fun <T: Comparable<T>> arrayData(vararg values: T, toArray: Array<T>.() -> Unit) {}
fun box(): String {
    arrayData(42) { }
    return "OK"
}
