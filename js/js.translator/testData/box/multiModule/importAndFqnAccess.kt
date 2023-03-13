// KJS_WITH_FULL_RUNTIME
// MODULE: lib
// FILE: file1.kt
package foo.bar

fun test1() = "O"

// FILE: file2.kt
package foo.baz

fun test2() = "K"

// MODULE: main(lib)
// FILE: main.kt
import foo.bar.*


fun box(): String {
    return test1() + foo.baz.test2()
}
