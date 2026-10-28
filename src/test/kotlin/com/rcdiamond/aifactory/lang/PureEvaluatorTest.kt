package com.rcdiamond.aifactory.lang

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PureEvaluatorTest {
    @Test
    fun `arithmetic precedence signed literals and integer division`() {
        val result = evaluateTest(
            """
            output precedence : Integer = 1 + 2 * 3;
            output minus : Integer = 1 - -2;
            output division : Integer = 7 / 2;
            output remainder : Integer = 7 % 2;
            """.trimIndent(),
        )
        assertEquals(7, result.integer("precedence"))
        assertEquals(3, result.integer("minus"))
        assertEquals(3, result.integer("division"))
        assertEquals(1, result.integer("remainder"))
    }

    @Test
    fun `boolean conjunction short circuits a division by zero`() {
        val result = evaluateTest(
            """
            input conjunctionFlag : Boolean;
            input disjunctionFlag : Boolean;
            output conjunction : Boolean = conjunctionFlag && (1 / 0 == 0);
            output disjunction : Boolean = disjunctionFlag || (1 / 0 == 0);
            """.trimIndent(),
            mapOf(
                "conjunctionFlag" to PureValue.BooleanValue(false),
                "disjunctionFlag" to PureValue.BooleanValue(true),
            ),
        )
        assertEquals(false, result.boolean("conjunction"))
        assertEquals(true, result.boolean("disjunction"))

        evaluateFailure(
            """
            input flag : Boolean;
            output conjunction : Boolean = flag && (1 / 0 == 0);
            """.trimIndent(),
            mapOf("flag" to PureValue.BooleanValue(true)),
        ).assertEqualsDiagnosticCode(DiagnosticCode.E_EVALUATION)
    }

    @Test
    fun `choice demands only the selected branch`() {
        val result = evaluateTest(
            """
            input flag : Boolean;
            output chosen : Integer = if flag then 1 / 0 else 2;
            """.trimIndent(),
            mapOf("flag" to PureValue.BooleanValue(false)),
        )
        assertEquals(2, result.integer("chosen"))

        evaluateFailure(
            """
            input flag : Boolean;
            output chosen : Integer = if flag then 1 / 0 else 2;
            """.trimIndent(),
            mapOf("flag" to PureValue.BooleanValue(true)),
        ).assertEqualsDiagnosticCode(DiagnosticCode.E_EVALUATION)
    }

    @Test
    fun `ordinary division by zero is an evaluation diagnostic`() {
        val diagnostic = evaluateFailure("output value : Integer = 1 / 0;")
        assertEquals(DiagnosticCode.E_EVALUATION, diagnostic.code)
        assertTrue(diagnostic.message.contains("zero"))
    }

    @Test
    fun `input bindings are required and type checked`() {
        evaluateFailure("input value : Integer;\noutput result : Integer = value;")
            .assertEqualsDiagnosticCode(DiagnosticCode.E_INPUT_BINDING)

        evaluateFailure(
            "input value : Integer;\noutput result : Integer = value;",
            mapOf("value" to PureValue.StringValue("nope")),
        ).assertEqualsDiagnosticCode(DiagnosticCode.E_INPUT_BINDING)

        val unused = evaluateTest(
            """
            input unused : Integer;
            output result : Integer = 1;
            """.trimIndent(),
        )
        assertEquals(1, unused.integer("result"))
    }

    @Test
    fun `strings use decoded escapes and concatenation`() {
        val result = evaluateTest(
            """
            output concatenated : String = "a" ++ "b";
            output escaped : String = "line\n\u0041\uD83D\uDE00";
            """.trimIndent(),
        )
        assertEquals("ab", result.string("concatenated"))
        assertEquals("line\nA\uD83D\uDE00", result.string("escaped"))
    }

    @Test
    fun `finite list primitives evaluate deterministically`() {
        val result = evaluateTest(
            """
            input xs : [Integer];
            output length : Integer = length xs;
            output empty : Boolean = isEmpty xs;
            output first : Integer = get xs 0;
            output fallback : Integer = getOr xs 100 42;
            output appended : [Integer] = append xs 9;
            output sliced : [Integer] = slice xs 1 3;
            """.trimIndent(),
            mapOf("xs" to PureValue.ListValue(listOf(4, 5, 6, 7).map { PureValue.IntegerValue(it) })),
        )
        assertEquals(4, result.integer("length"))
        assertEquals(false, result.boolean("empty"))
        assertEquals(4, result.integer("first"))
        assertEquals(42, result.integer("fallback"))
        assertEquals(listOf(4, 5, 6, 7, 9), result.list("appended").map { (it as PureValue.IntegerValue).value })
        assertEquals(listOf(5, 6), result.list("sliced").map { (it as PureValue.IntegerValue).value })
    }

    @Test
    fun `higher order list primitives preserve finite order`() {
        val result = evaluateTest(
            """
            input xs : [Integer];
            output doubled : [Integer] = map (add 1) xs;
            output atLeastTwo : [Integer] = filter (flip ge 2) xs;
            output total : Integer = reduce add xs 0;
            """.trimIndent(),
            mapOf("xs" to PureValue.ListValue(listOf(1, 2, 3).map { PureValue.IntegerValue(it) })),
        )
        assertEquals(listOf(2, 3, 4), result.list("doubled").map { (it as PureValue.IntegerValue).value })
        assertEquals(listOf(2, 3), result.list("atLeastTwo").map { (it as PureValue.IntegerValue).value })
        assertEquals(6, result.integer("total"))
    }

    @Test
    fun `partial application returns a callable function value`() {
        val result = evaluateTest(
            """
            let inc = add 1;
            output function : Fn(Integer) -> Integer = inc;
            """.trimIndent(),
        )
        val function = result.function("function")
        assertEquals("Fn(Integer) -> Integer", function.signature.display())
        assertEquals(1, function.appliedArguments.size)
        assertEquals(
            3,
            (function.applyTo(PureValue.IntegerValue(2)) as PureValue.IntegerValue).value,
        )
    }

    @Test
    fun `composition applies left to right`() {
        val result = evaluateTest(
            """
            let inc = add 1;
            let twice = mul 2;
            output function : Fn(Integer) -> Integer = inc >> twice;
            """.trimIndent(),
        )
        val function = result.function("function")
        assertEquals(
            8,
            (function.applyTo(PureValue.IntegerValue(3)) as PureValue.IntegerValue).value,
        )
    }

    @Test
    fun `flip swaps the first two arguments at runtime`() {
        val result = evaluateTest(
            """
            output comparison : Boolean = flip lt 64 100;
            output subtraction : Integer = flip sub 10 3;
            """.trimIndent(),
        )
        assertEquals(false, result.boolean("comparison"))
        assertEquals(-7, result.integer("subtraction"))
    }

    @Test
    fun `call0 invokes a genuinely zero parameter primitive`() {
        val environment = StandardScalarEnvironment.environment.builder()
            .addPrimitive(
                "zero",
                TypeScheme(LangTypes.function(result = LangTypes.INTEGER)),
            )
            .build()
        val primitives = StandardScalarRuntime.primitives + (
            "zero" to PurePrimitive { _ -> PureValue.IntegerValue(42) }
            )
        val result = evaluateTest(
            "output answer : Integer = call0 zero;",
            environment = environment,
            primitives = primitives,
        )
        assertEquals(42, result.integer("answer"))
    }

    @Test
    fun `predicate combinators short circuit per input`() {
        val result = evaluateTest(
            """
            let both = allOf (flip ge 2) (flip le 4);
            output yes : Boolean = both 3;
            output no : Boolean = both 5;
            output any : Boolean = anyOf (flip lt 0) (flip gt 10) 5;
            """.trimIndent(),
        )
        assertEquals(true, result.boolean("yes"))
        assertEquals(false, result.boolean("no"))
        assertEquals(false, result.boolean("any"))
    }

    @Test
    fun `integer overflow wraps and double division keeps ieee behavior`() {
        val result = evaluateTest(
            """
            output wrapped : Integer = 2147483647 + 1;
            output minimum : Long = -9223372036854775808L;
            output infinity : Double = 1.0 / 0.0;
            """.trimIndent(),
        )
        assertEquals(Int.MIN_VALUE, result.integer("wrapped"))
        assertEquals(Long.MIN_VALUE, result.long("minimum"))
        val infinity = (result.outputs["infinity"] as PureValue.DoubleValue).value
        assertTrue(infinity.isInfinite())
    }

    @Test
    fun `nominal constant primitives evaluate through the runtime`() {
        val environment = LanguageEnvironment.basic().builder()
            .addNominalType("Item")
            .addPrimitive(
                "emptyItem",
                TypeScheme(LangTypes.nominal("Item")),
            )
            .build()
        val primitives = mapOf(
            "emptyItem" to PurePrimitive { _ ->
                PureValue.ObjectValue(ObjectKind.ITEM, "Item", emptyList())
            },
        )
        val result = evaluateTest(
            "output value : Item = emptyItem;",
            environment = environment,
            primitives = primitives,
        )
        assertTrue(result.outputs["value"] is PureValue.ObjectValue)
    }

    @Test
    fun `get out of range is an evaluation error`() {
        evaluateFailure(
            """
            input xs : [Integer];
            output bad : Integer = get xs 9;
            """.trimIndent(),
            mapOf("xs" to PureValue.ListValue(listOf(PureValue.IntegerValue(1)))),
        ).assertEqualsDiagnosticCode(DiagnosticCode.E_EVALUATION)
    }

    @Test
    fun `missing primitive runtime is a native contract error`() {
        val environment = LanguageEnvironment.basic().builder()
            .addPrimitive(
                "mystery",
                TypeScheme(LangTypes.function(LangTypes.INTEGER, result = LangTypes.INTEGER)),
            )
            .build()
        val module = compileTest(
            "output value : Integer = mystery 1;",
            environment,
        ).assertSuccess()
        val result = PureEvaluator().evaluate(module)
        assertTrue(!result.success)
        assertEquals(DiagnosticCode.E_NATIVE_CONTRACT, result.diagnostics.single().code)
    }
}
