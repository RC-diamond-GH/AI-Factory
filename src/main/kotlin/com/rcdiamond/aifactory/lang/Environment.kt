package com.rcdiamond.aifactory.lang

/** Raised by a [PrimitiveTypeRule] to report a deterministic typing failure. */
class PrimitiveTypeException(val code: DiagnosticCode, message: String) : RuntimeException(message)

/**
 * Inference services exposed to a [PrimitiveTypeRule]. Implementations are supplied by the type
 * checker; rules can create fresh variables, constrain them and unify types while typing an
 * overloaded primitive.
 */
interface PrimitiveTypeContext {
    fun resolve(type: LangType): LangType

    fun freshVariable(): LangType.Variable

    fun constrain(type: LangType, constraint: TypeConstraint)

    fun unify(expected: LangType, actual: LangType)
}

/**
 * A context-dependent primitive type rule. It is invoked with the types of the arguments consumed
 * so far and either returns the type of the primitive after those arguments, or null when at least
 * one more argument is needed.
 *
 * This exists for genuinely higher-order overloads such as `flip`, whose result keeps the argument
 * count of its function argument. Ordinary primitives should use a [TypeScheme] instead. A rule
 * cannot create a value without arguments: using the bare primitive as a value is an unresolved
 * type, not `Any`.
 */
fun interface PrimitiveTypeRule {
    fun typeAfter(arguments: List<LangType>, context: PrimitiveTypeContext): LangType?
}

/**
 * A primitive value contract: either a fixed [scheme] instantiated at each use, or a context
 * dependent [rule] for overloaded higher-order primitives.
 */
data class PrimitiveContract(
    val name: String,
    val scheme: TypeScheme? = null,
    val description: String = "",
    val rule: PrimitiveTypeRule? = null,
) {
    init {
        require((scheme == null) != (rule == null)) {
            "primitive '${name}' must have exactly one of a type scheme or a type rule"
        }
    }

    companion object {
        fun fixed(name: String, scheme: TypeScheme, description: String = ""): PrimitiveContract =
            PrimitiveContract(name, scheme, description, null)

        fun withRule(name: String, rule: PrimitiveTypeRule, description: String = ""): PrimitiveContract =
            PrimitiveContract(name, null, description, rule)
    }
}

data class NominalType(val name: String, val description: String = "")

enum class ConstructorArgumentKind { RESOURCE_ID, POSITIVE_INTEGER, TEXT }

/**
 * Syntax and typing of one static object constructor. The grammar fixes the keyword, while the
 * environment decides whether the constructor exists and which nominal type it produces.
 */
data class ObjectConstructorSpec(
    val kind: ObjectKind,
    val resultType: LangType.Nominal,
    val arguments: List<ConstructorArgumentKind>,
) {
    init {
        when (kind) {
            ObjectKind.FLUID -> require(
                arguments == listOf(
                    ConstructorArgumentKind.RESOURCE_ID,
                    ConstructorArgumentKind.POSITIVE_INTEGER,
                ),
            ) { "fluid takes a resource id and a positive Integer amount" }

            ObjectKind.ITEM,
            ObjectKind.BLOCK -> require(arguments == listOf(ConstructorArgumentKind.RESOURCE_ID)) {
                "${kind.name.lowercase()} takes exactly one resource id"
            }

            ObjectKind.NBT -> require(arguments == listOf(ConstructorArgumentKind.TEXT)) {
                "nbt takes exactly one SNBT text literal"
            }
        }
    }
}

data class OperatorContract(
    val id: String,
    val scheme: TypeScheme,
    val description: String = "",
)

/** Resolves `op "namespace:registered_id"` references. Missing entries are E_NATIVE_CONTRACT. */
fun interface OperatorCatalog {
    fun resolve(id: String): OperatorContract?
}

/** Optional syntactic check for resource strings in object literals. */
fun interface ResourceIdValidator {
    fun isValid(id: String): Boolean
}

fun interface TypeConstraintRule {
    fun accepts(type: LangType): Boolean
}

/**
 * Immutable injection surface for a language host.
 *
 * The environment owns names and contracts, not game objects or native evaluators. A target backend
 * reads the [TypedModule] types and primitive names and maps them to its own native operations.
 * Objects such as Minecraft items never appear in this package.
 */
class LanguageEnvironment internal constructor(
    val nominalTypes: Map<String, NominalType>,
    val primitives: Map<String, PrimitiveContract>,
    val objectConstructors: Map<ObjectKind, ObjectConstructorSpec>,
    val operatorCatalog: OperatorCatalog?,
    val numericTypes: Set<String>,
    val namedConstraints: Map<String, TypeConstraintRule>,
    val resourceIdValidator: ResourceIdValidator?,
) {
    fun hasType(name: String): Boolean = nominalTypes.containsKey(name)

    fun primitive(name: String): PrimitiveContract? = primitives[name]

    fun objectConstructor(kind: ObjectKind): ObjectConstructorSpec? = objectConstructors[kind]

    /**
     * Whether a resolved type satisfies a constraint. Unresolved variables are accepted here; the
     * finalizer reports them as E_UNRESOLVED_TYPE before constraints can be trusted.
     */
    fun accepts(type: LangType, constraint: TypeConstraint): Boolean = when (constraint) {
        TypeConstraint.Numeric -> when (val resolved = type) {
            is LangType.Nominal -> resolved.name in numericTypes
            is LangType.Variable -> true
            else -> false
        }

        TypeConstraint.Concat -> when (val resolved = type) {
            is LangType.Nominal -> resolved.name == "String"
            is LangType.ListType -> true
            is LangType.Variable -> true
            else -> false
        }

        TypeConstraint.Equatable -> when (val resolved = type) {
            is LangType.Variable -> true
            is LangType.Nominal -> true
            is LangType.ListType -> accepts(resolved.element, TypeConstraint.Equatable)
            is LangType.FunctionType -> false
        }

        is TypeConstraint.Named -> {
            if (type is LangType.Variable) true
            else namedConstraints[constraint.name]?.accepts(type) ?: false
        }
    }

    fun builder(): EnvironmentBuilder = EnvironmentBuilder(this)

    /** Built-in scalar types only; no primitive values and no domain objects. */
    fun toBuilder(): EnvironmentBuilder = EnvironmentBuilder(this)

    companion object {
        val KEYWORDS: Set<String> = setOf(
            "input", "let", "output", "if", "then", "else", "true", "false",
            "op", "item", "block", "fluid", "nbt", "Fn",
        )

        private val TYPE_NAME = Regex("[A-Za-z_][A-Za-z0-9_]*")

        val BUILTIN_TYPES: Set<String> = setOf("Boolean", "Integer", "Long", "Double", "String")

        /** Scalar-only environment. Hosts add their own types, primitives and constructors. */
        fun basic(): LanguageEnvironment {
            val types = linkedMapOf(
                "Boolean" to NominalType("Boolean", "logical true/false"),
                "Integer" to NominalType("Integer", "signed 32-bit integer"),
                "Long" to NominalType("Long", "signed 64-bit integer"),
                "Double" to NominalType("Double", "IEEE-754 double"),
                "String" to NominalType("String", "Unicode string"),
            )
            return LanguageEnvironment(
                nominalTypes = types,
                primitives = emptyMap(),
                objectConstructors = emptyMap(),
                operatorCatalog = null,
                numericTypes = setOf("Integer", "Long", "Double"),
                namedConstraints = emptyMap(),
                resourceIdValidator = null,
            )
        }

        internal fun requireIdentifier(what: String, name: String) {
            require(TYPE_NAME.matches(name)) { "$what '$name' is not an ASCII identifier" }
            require(name !in KEYWORDS) { "$what '$name' is a reserved keyword" }
        }
    }
}

/**
 * Builder for [LanguageEnvironment]. It starts from an existing environment and copies its maps, so
 * a host can keep a base environment and derive specialized ones without mutation.
 */
class EnvironmentBuilder internal constructor(base: LanguageEnvironment) {
    private val nominalTypes = LinkedHashMap(base.nominalTypes)
    private val primitives = LinkedHashMap(base.primitives)
    private val objectConstructors = LinkedHashMap(base.objectConstructors)
    private var operatorCatalog = base.operatorCatalog
    private var numericTypes = LinkedHashSet(base.numericTypes)
    private val namedConstraints = LinkedHashMap(base.namedConstraints)
    private var resourceIdValidator = base.resourceIdValidator
    private var built = false

    private fun checkMutable() {
        check(!built) { "environment builder has already been built" }
    }

    fun addNominalType(name: String, description: String = ""): EnvironmentBuilder {
        checkMutable()
        LanguageEnvironment.requireIdentifier("type name", name)
        require(name !in nominalTypes) { "type '$name' is already registered" }
        nominalTypes[name] = NominalType(name, description)
        return this
    }

    fun addPrimitive(contract: PrimitiveContract): EnvironmentBuilder {
        checkMutable()
        LanguageEnvironment.requireIdentifier("primitive name", contract.name)
        require(contract.name !in primitives) { "primitive '${contract.name}' is already registered" }
        primitives[contract.name] = contract
        return this
    }

    fun addPrimitive(
        name: String,
        scheme: TypeScheme,
        description: String = "",
    ): EnvironmentBuilder = addPrimitive(PrimitiveContract.fixed(name, scheme, description))

    fun addPrimitive(
        name: String,
        rule: PrimitiveTypeRule,
        description: String = "",
    ): EnvironmentBuilder = addPrimitive(PrimitiveContract.withRule(name, rule, description))

    fun addObjectConstructor(
        kind: ObjectKind,
        resultTypeName: String,
        arguments: List<ConstructorArgumentKind>,
    ): EnvironmentBuilder {
        checkMutable()
        require(resultTypeName in nominalTypes) { "constructor result type '$resultTypeName' is not registered" }
        require(kind !in objectConstructors) { "constructor '${kind.name.lowercase()}' is already registered" }
        objectConstructors[kind] = ObjectConstructorSpec(kind, LangType.Nominal(resultTypeName), arguments)
        return this
    }

    fun operatorCatalog(catalog: OperatorCatalog): EnvironmentBuilder {
        checkMutable()
        operatorCatalog = catalog
        return this
    }

    fun numericTypes(names: Collection<String>): EnvironmentBuilder {
        checkMutable()
        for (name in names) {
            require(name in nominalTypes) { "numeric type '$name' is not registered" }
        }
        numericTypes = LinkedHashSet(names)
        return this
    }

    fun addNumericType(name: String): EnvironmentBuilder {
        checkMutable()
        require(name in nominalTypes) { "numeric type '$name' is not registered" }
        numericTypes.add(name)
        return this
    }

    fun addConstraint(name: String, rule: TypeConstraintRule): EnvironmentBuilder {
        checkMutable()
        LanguageEnvironment.requireIdentifier("constraint name", name)
        require(name !in BUILTIN_CONSTRAINT_NAMES) { "constraint '$name' is built in" }
        require(name !in namedConstraints) { "constraint '$name' is already registered" }
        namedConstraints[name] = rule
        return this
    }

    fun resourceIdValidator(validator: ResourceIdValidator?): EnvironmentBuilder {
        checkMutable()
        resourceIdValidator = validator
        return this
    }

    fun build(): LanguageEnvironment {
        checkMutable()
        built = true
        require(nominalTypes.keys.containsAll(LanguageEnvironment.BUILTIN_TYPES)) {
            "built-in scalar types cannot be removed"
        }
        require(numericTypes.all { it in nominalTypes }) { "numeric types must be registered" }
        for (spec in objectConstructors.values) {
            require(spec.resultType.name in nominalTypes) {
                "constructor result type '${spec.resultType.name}' is not registered"
            }
        }
        return LanguageEnvironment(
            nominalTypes = nominalTypes.toMap(),
            primitives = primitives.toMap(),
            objectConstructors = objectConstructors.toMap(),
            operatorCatalog = operatorCatalog,
            numericTypes = numericTypes.toSet(),
            namedConstraints = namedConstraints.toMap(),
            resourceIdValidator = resourceIdValidator,
        )
    }

    private companion object {
        val BUILTIN_CONSTRAINT_NAMES: Set<String> = setOf("Numeric", "Concat", "Equatable")
    }
}
