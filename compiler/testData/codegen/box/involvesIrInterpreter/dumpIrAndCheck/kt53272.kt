// !LANGUAGE: +IntrinsicConstEvaluation
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR

// FILE: 1.kt

const val name = E.OK.<!EVALUATED("OK")!>name<!>
fun box(): String = <!EVALUATED("OK")!>name<!>

// FILE: 2.kt

enum class E(val parent: E?) {
    X(null),
    OK(X),
}
