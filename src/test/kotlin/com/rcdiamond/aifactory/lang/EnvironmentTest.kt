package com.rcdiamond.aifactory.lang

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EnvironmentTest {
    private fun domainEnvironment(
        catalog: OperatorCatalog? = null,
        withConstructors: Boolean = true,
    ): LanguageEnvironment {
        val builder = LanguageEnvironment.basic().builder()
            .addNominalType("Item")
            .addNominalType("Block")
            .addNominalType("Fluid")
            .addNominalType("NBT")
            .resourceIdValidator { id ->
                Regex("^[a-z0-9_.-]+:[a-z0-9_./-]+$").matches(id)
            }
        if (withConstructors) {
            builder
                .addObjectConstructor(
                    ObjectKind.ITEM,
                    "Item",
                    listOf(ConstructorArgumentKind.RESOURCE_ID),
                )
                .addObjectConstructor(
                    ObjectKind.BLOCK,
                    "Block",
                    listOf(ConstructorArgumentKind.RESOURCE_ID),
                )
                .addObjectConstructor(
                    ObjectKind.FLUID,
                    "Fluid",
                    listOf(ConstructorArgumentKind.RESOURCE_ID, ConstructorArgumentKind.POSITIVE_INTEGER),
                )
                .addObjectConstructor(
                    ObjectKind.NBT,
                    "NBT",
                    listOf(ConstructorArgumentKind.TEXT),
                )
        }
        if (catalog != null) builder.operatorCatalog(catalog)
        return builder.build()
    }

    private fun catalogue(): OperatorCatalog = OperatorCatalog { id ->
        if (id == "test:predicate") {
            OperatorContract(
                id,
                TypeScheme(
                    LangTypes.function(LangTypes.nominal("Item"), result = LangTypes.BOOLEAN),
                ),
            )
        } else {
            null
        }
    }

    @Test
    fun `object constructors use resource or text arguments as declared`() {
        val environment = domainEnvironment()
        val source = """
            output itemValue : Item = item "minecraft:stone";
            output blockValue : Block = block "minecraft:stone";
            output fluidValue : Fluid = fluid "minecraft:water" 1000;
            output nbtValue : NBT = nbt "{Damage:10, display:{Name:\"x y\"}}";
        """.trimIndent()
        val module = compileTest(source, environment).assertSuccess()

        val constructs = module.graph.nodes.mapNotNull { it.operation as? IrOperation.Construct }
        assertEquals(4, constructs.size)
        assertEquals(ObjectKind.ITEM, constructs[0].kind)
        assertEquals(ObjectKind.BLOCK, constructs[1].kind)
        assertEquals(ObjectKind.FLUID, constructs[2].kind)
        assertEquals(
            listOf(IrStaticArgument.Text("minecraft:water"), IrStaticArgument.Integer(1000)),
            constructs[2].arguments,
        )
        assertEquals(ObjectKind.NBT, constructs[3].kind)
        assertEquals(
            listOf(IrStaticArgument.Text("{Damage:10, display:{Name:\"x y\"}}")),
            constructs[3].arguments,
        )

        val evaluation = PureEvaluator().evaluate(module)
        assertTrue(evaluation.success, evaluation.renderDiagnostics(module.source))
        val item = evaluation.result!!.outputs["itemValue"] as PureValue.ObjectValue
        assertEquals(ObjectKind.ITEM, item.kind)
        assertEquals("Item", item.typeName)
    }

    @Test
    fun `resource ids are validated but nbt text is not a resource`() {
        val environment = domainEnvironment()
        compileTest(
            """output bad : Item = item "stone";""",
            environment,
        ).assertFailure(DiagnosticCode.E_LITERAL_RESOURCE)

        compileTest(
            """output bad : Fluid = fluid "minecraft:water" 0;""",
            environment,
        ).assertFailure(DiagnosticCode.E_LITERAL_RANGE)

        compileTest(
            """output bad : Fluid = fluid "minecraft:water" -1;""",
            environment,
        ).assertFailure(DiagnosticCode.E_SYNTAX)
    }

    @Test
    fun `missing object constructors are a native contract error`() {
        val environment = domainEnvironment(withConstructors = false)
        compileTest(
            """output bad : Item = item "minecraft:stone";""",
            environment,
        ).assertFailure(DiagnosticCode.E_NATIVE_CONTRACT)
    }

    @Test
    fun `operator literals require a catalog entry with a type scheme`() {
        val environment = domainEnvironment(catalog = catalogue())
        val module = compileTest(
            """
            output applied : Boolean = op "test:predicate" (item "minecraft:stone");
            output predicate : Fn(Item) -> Boolean = op "test:predicate";
            """.trimIndent(),
            environment,
        ).assertSuccess()
        assertEquals(
            "Fn(Item) -> Boolean",
            module.output("predicate")?.type?.display(),
        )
        assertTrue(module.graph.nodes.any { it.operation is IrOperation.OperatorLiteral })

        compileTest(
            """output predicate : Fn(Item) -> Boolean = op "test:missing";""",
            environment,
        ).assertFailure(DiagnosticCode.E_NATIVE_CONTRACT)

        compileTest(
            """output predicate : Fn(Item) -> Boolean = op "test:predicate";""",
            domainEnvironment(catalog = null),
        ).assertFailure(DiagnosticCode.E_NATIVE_CONTRACT)
    }

    @Test
    fun `custom constraints are enforced at finalization`() {
        val n = LangType.Variable(0)
        val evenScheme = TypeScheme(
            LangTypes.function(n, result = n),
            setOf(0),
            listOf(SchemeConstraint(0, TypeConstraint.Named("Even"))),
        )
        val environment = LanguageEnvironment.basic().builder()
            .addConstraint("Even", TypeConstraintRule { type ->
                type == LangTypes.INTEGER || type == LangTypes.LONG
            })
            .addPrimitive("onlyEven", evenScheme)
            .build()

        compileTest(
            """output bad : String = onlyEven "x";""",
            environment,
        ).assertFailure(DiagnosticCode.E_TYPE)
        compileTest(
            """output good : Integer = onlyEven 2;""",
            environment,
        ).assertSuccess()
    }

    @Test
    fun `dynamic flip rule keeps arbitrary function arity`() {
        val triple = TypeScheme(
            LangTypes.function(
                LangTypes.INTEGER,
                LangTypes.LONG,
                LangTypes.STRING,
                result = LangTypes.BOOLEAN,
            ),
        )
        val environment = StandardScalarEnvironment.environment.builder()
            .addPrimitive("triple", triple)
            .build()
        val module = compileTest(
            """output flipped : Fn(Long, Integer, String) -> Boolean = flip triple;""",
            environment,
        ).assertSuccess()
        assertEquals(
            "Fn(Long, Integer, String) -> Boolean",
            module.output("flipped")?.type?.display(),
        )

        compileTest("output bad = flip identity;", environment)
            .assertFailure(DiagnosticCode.E_FN_SIGNATURE)
        compileTest("output bad : Integer = flip 1;", environment)
            .assertFailure(DiagnosticCode.E_TYPE)
    }

    @Test
    fun `basic environment is isolated from scalar primitives`() {
        val environment = LanguageEnvironment.basic()
        compileTest("output x : Integer = 1;", environment).assertSuccess()
        compileTest("output x : Integer = add 1 2;", environment)
            .assertFailure(DiagnosticCode.E_UNKNOWN_NAME)
    }

    @Test
    fun `contracts cannot introduce unregistered nominal types`() {
        val environment = LanguageEnvironment.basic().builder()
            .addPrimitive(
                "weird",
                TypeScheme(LangTypes.nominal("Missing")),
            )
            .build()
        compileTest("output value = weird;", environment)
            .assertFailure(DiagnosticCode.E_NATIVE_CONTRACT)
    }

    @Test
    fun `duplicate environment registrations are programmer errors`() {
        assertFailsWith<IllegalArgumentException> {
            LanguageEnvironment.basic().builder()
                .addPrimitive(
                    "value",
                    TypeScheme(LangTypes.INTEGER),
                )
                .addPrimitive(
                    "value",
                    TypeScheme(LangTypes.INTEGER),
                )
        }
        assertFailsWith<IllegalArgumentException> {
            LanguageEnvironment.basic().builder()
                .addConstraint("Custom", TypeConstraintRule { true })
                .addConstraint("Custom", TypeConstraintRule { true })
        }
    }
}
