// FIR_IDENTICAL
// SKIP_KLIB_TEST
package test

class A() {
    constructor(x: Int) : this()
    fun b() {}
    fun a() {}
    val b: Int = 1
    val a: Int = 2
    constructor(x: String) : this()
    val Int.b: String get() = "b"
    fun String.b() {}
    val Int.a: String get() = "a"
    fun String.a() {}
    constructor(x: Double) : this()
}
