// ISSUE: KT-57649

open class A
abstract class B {
    fun test(current: A): A? =
        <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING("K; B?, A?; multiple incompatible classes; : B, A")!>if (<!EQUALITY_NOT_APPLICABLE_WARNING!>current === this<!>) current else null<!>
}
