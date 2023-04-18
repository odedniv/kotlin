// ISSUE: KT-57905

// FILE: Base.java
public class Base {
    String value = null;
}

// FILE: Main.kt
class Derived: Base() {
    val value: Int = 42
    val something: String = <!INITIALIZER_TYPE_MISMATCH!>value<!>
}
