// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +SealedInlineClasses
// IGNORE_BACKEND: JVM

OPTIONAL_JVM_INLINE_ANNOTATION
sealed value class IC

OPTIONAL_JVM_INLINE_ANNOTATION
value class FieldValue(val value: String): IC()

enum class RequestFields {
    ENUM_ONE
}

class RequestInputParameters(
    private val backingMap: Map<RequestFields, IC>
) : Map<RequestFields, IC> by backingMap

fun box(): String {
    val testMap1 = mapOf(RequestFields.ENUM_ONE to FieldValue("value1"))
    val test1 = testMap1[RequestFields.ENUM_ONE]!!
    if ((test1 as FieldValue).value != "value1") throw AssertionError("test1: $test1")

    val testMap2 = RequestInputParameters(mapOf(RequestFields.ENUM_ONE to FieldValue("value2")))
    val test2 = testMap2[RequestFields.ENUM_ONE]!!
    if ((test2 as FieldValue).value != "value2") throw AssertionError("test2: $test2")

    return "OK"
}