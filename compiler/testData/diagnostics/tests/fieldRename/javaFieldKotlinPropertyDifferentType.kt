// ISSUE: KT-57905

// FILE: Base.java
public class Base {
    String value = null;
}

// FILE: Main.kt
class Derived: Base() {
    val value: Int = 42
    val something: String = <!BASE_CLASS_FIELD_WITH_DIFFERENT_SIGNATURE_THAN_DERIVED_CLASS_PROPERTY!>value<!>
}
