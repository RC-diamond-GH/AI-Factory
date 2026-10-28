package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.server

import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdDiagnosticCode
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdDslType
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdDslTypes
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdFiniteness
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdNativeOperator
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdNativeSignature
import com.rcdiamond.aifactory.lang.LangType
import com.rcdiamond.aifactory.lang.TypeConstraint
import com.rcdiamond.aifactory.lang.TypeScheme
import net.minecraft.resources.ResourceLocation
import org.cyclops.integrateddynamics.api.evaluate.operator.IOperator
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueTypeListProxy
import org.cyclops.integrateddynamics.core.evaluate.operator.Operators
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueHelpers
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeList
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes

/** A type plus the finiteness the server can actually prove for it. */
internal data class IdNativeTyped(val type: IdDslType, val finite: IdFiniteness)

/**
 * The server's independent view of the pinned Integrated Dynamics 1.31.3 value/operator contracts.
 *
 * This class never evaluates a variable. It maps a concrete [IValueType] to the DSL's public type
 * vocabulary and re-derives operator outputs from the real registry objects. Generic operators are
 * handled by the small set of fixed rules below; an operator whose output is a list/operator/`Any`
 * category and whose rule is unknown is rejected instead of guessed. That keeps the dsl package's
 * fixed catalog from being the only guard.
 */
internal object IdNativeCatalog {
    const val MAX_NODES = 256
    const val MAX_INPUTS = 64
    const val MAX_OUTPUTS = 64
    const val MAX_TYPE_DEPTH = 16
    const val MAX_LIST_LENGTH = 256
    const val MAX_LITERAL_BYTES = 8192

    /** The pure dsl validator additionally caps the entire plan and materialized value JSON. */
    const val MAX_PLAN_BYTES = 262_144
    const val MAX_VALUE_JSON_NODES = 4_096

    private val NUMERIC = IdDslTypes.NUMERIC_SCALARS

    /** The concrete native type used to check a DSL type against an [IOperator] signature. */
    fun nativeType(type: IdDslType): IValueType<*> = when (type) {
        is IdDslType.Scalar -> when (type.name) {
            "Boolean" -> ValueTypes.BOOLEAN
            "Integer" -> ValueTypes.INTEGER
            "Long" -> ValueTypes.LONG
            "Double" -> ValueTypes.DOUBLE
            "String" -> ValueTypes.STRING
            "Item" -> ValueTypes.OBJECT_ITEMSTACK
            "Block" -> ValueTypes.OBJECT_BLOCK
            "Entity" -> ValueTypes.OBJECT_ENTITY
            "Fluid" -> ValueTypes.OBJECT_FLUIDSTACK
            "NBT" -> ValueTypes.NBT
            "Ingredients" -> ValueTypes.OBJECT_INGREDIENTS
            "Recipe" -> ValueTypes.OBJECT_RECIPE
            else -> throw IdNativeContractError(
                IdDiagnosticCode.E_UNKNOWN_TYPE,
                "type '${type.name}' is not one of the pinned Integrated Dynamics core types",
            )
        }
        is IdDslType.ListType -> ValueTypes.LIST
        is IdDslType.FunctionType -> ValueTypes.OPERATOR
    }

    /** A scalar DSL type, or null when [valueType] is a category/list/operator without a concrete name. */
    fun scalarOf(valueType: IValueType<*>): IdDslType.Scalar? = when {
        valueType === ValueTypes.BOOLEAN -> IdDslTypes.BOOLEAN
        valueType === ValueTypes.INTEGER -> IdDslTypes.INTEGER
        valueType === ValueTypes.LONG -> IdDslTypes.LONG
        valueType === ValueTypes.DOUBLE -> IdDslTypes.DOUBLE
        valueType === ValueTypes.STRING -> IdDslTypes.STRING
        valueType === ValueTypes.OBJECT_ITEMSTACK -> IdDslTypes.ITEM
        valueType === ValueTypes.OBJECT_BLOCK -> IdDslTypes.BLOCK
        valueType === ValueTypes.OBJECT_ENTITY -> IdDslTypes.ENTITY
        valueType === ValueTypes.OBJECT_FLUIDSTACK -> IdDslTypes.FLUID
        valueType === ValueTypes.NBT -> IdDslTypes.NBT
        valueType === ValueTypes.OBJECT_INGREDIENTS -> IdDslTypes.INGREDIENTS
        valueType === ValueTypes.OBJECT_RECIPE -> IdDslTypes.RECIPE
        else -> null
    }

    /** The element type of a materialized/proxy list, if it is a concrete core scalar. */
    fun listElementOf(proxy: IValueTypeListProxy<*, *>): IdDslType.Scalar? = scalarOf(proxy.valueType)

    /** The element type of a constant list value, if provable without touching its elements. */
    fun listElementOf(value: ValueTypeList.ValueList<*, *>): IdDslType.Scalar? =
        listElementOf(value.rawValue)

    /** The list's finiteness from its proxy metadata; an unreadable proxy is `unknown`, never `finite`. */
    fun finiteOf(proxy: IValueTypeListProxy<*, *>): IdFiniteness = try {
        if (proxy.isInfinite) IdFiniteness.INFINITE else IdFiniteness.FINITE
    } catch (_: Exception) {
        IdFiniteness.UNKNOWN
    }

    fun finiteOf(value: ValueTypeList.ValueList<*, *>): IdFiniteness = finiteOf(value.rawValue)

    /** Resolves a plan/registry operator id. The id must be the exact registered canonical name. */
    fun operatorById(id: String): IOperator {
        val name = ResourceLocation.tryParse(id)
            ?: throw IdNativeContractError(IdDiagnosticCode.E_NATIVE_CONTRACT, "operator '$id' is not a valid resource id")
        if (name.toString() != id) {
            throw IdNativeContractError(
                IdDiagnosticCode.E_NATIVE_CONTRACT,
                "operator '$id' is not canonical; use the registered lower-case resource id",
            )
        }
        return Operators.REGISTRY.getOperator(name)
            ?: throw IdNativeContractError(
                IdDiagnosticCode.E_NATIVE_CONTRACT,
                "operator '$id' is not registered by the pinned Integrated Dynamics version",
            )
    }

    /**
     * A native operator constant's declared Fn signature must unify with the pinned catalog
     * signature and with the live operator's declared category types.
     *
     * `operator_map`/`filter`/`pipe`/... have a fixed signature even though they take and return
     * operator values, so they are valid native constants. Only `apply`/`apply0` are not emitted as
     * native constants by the compiler; a native node for those is rejected. `flip` is checked by
     * its structural swap rule.
     */
    fun checkNativeConstant(
        operator: IOperator,
        declared: IdDslType.FunctionType,
        contract: IdNativeOperator,
    ) {
        when (val signature = contract.signature) {
            is IdNativeSignature.Fixed -> {
                if (!schemeMatches(signature.scheme, declared)) {
                    throw IdNativeContractError(
                        IdDiagnosticCode.E_FN_SIGNATURE,
                        "operator '${operator.uniqueName}' concrete type ${declared.display()} does not match " +
                            "the pinned catalog signature",
                    )
                }
            }
            IdNativeSignature.ApplyOne, IdNativeSignature.ApplyZero -> {
                throw IdNativeContractError(
                    IdDiagnosticCode.E_NATIVE_CONTRACT,
                    "operator '${operator.uniqueName}' is an application operator and must be a call, not a " +
                        "native constant",
                )
            }
            IdNativeSignature.Flip -> checkFlip(declared, operator)
        }
        val inputs = operator.inputTypes
        if (inputs.size != declared.parameters.size) {
            throw IdNativeContractError(
                IdDiagnosticCode.E_FN_SIGNATURE,
                "operator '${operator.uniqueName}' takes ${inputs.size} input(s), but the plan declares " +
                    "${declared.parameters.size}",
            )
        }
        for (index in inputs.indices) {
            val declaredType = nativeType(declared.parameters[index])
            if (!ValueHelpers.correspondsTo(declaredType, inputs[index])) {
                throw IdNativeContractError(
                    IdDiagnosticCode.E_FN_SIGNATURE,
                    "operator '${operator.uniqueName}' input ${index + 1} is incompatible with " +
                        declared.parameters[index].display(),
                )
            }
        }
        val declaredResult = nativeType(declared.result)
        if (!ValueHelpers.correspondsTo(declaredResult, operator.outputType)) {
            throw IdNativeContractError(
                IdDiagnosticCode.E_FN_SIGNATURE,
                "operator '${operator.uniqueName}' output is incompatible with ${declared.result.display()}",
            )
        }
    }

    private fun schemeMatches(scheme: TypeScheme, declared: IdDslType.FunctionType): Boolean {
        val shape = scheme.type as? LangType.FunctionType ?: return false
        if (shape.parameters.size != declared.parameters.size) return false
        val bindings = HashMap<Int, IdDslType>()
        if (!matchType(shape.result, declared.result, bindings)) return false
        for (index in shape.parameters.indices) {
            if (!matchType(shape.parameters[index], declared.parameters[index], bindings)) return false
        }
        for (constraint in scheme.constraints) {
            val bound = bindings[constraint.variableId] ?: return false
            if (!constraintAccepts(constraint.constraint, bound)) return false
        }
        return true
    }

    private fun matchType(
        expected: LangType,
        actual: IdDslType,
        bindings: MutableMap<Int, IdDslType>,
    ): Boolean = when (expected) {
        is LangType.Variable -> {
            val previous = bindings[expected.id]
            if (previous == null) {
                bindings[expected.id] = actual
                true
            } else {
                previous == actual
            }
        }
        is LangType.Nominal -> actual is IdDslType.Scalar && actual.name == expected.name
        is LangType.ListType -> actual is IdDslType.ListType && matchType(expected.element, actual.element, bindings)
        is LangType.FunctionType -> {
            val fn = actual as? IdDslType.FunctionType
            fn != null &&
                fn.parameters.size == expected.parameters.size &&
                expected.parameters.indices.all { index ->
                    matchType(expected.parameters[index], fn.parameters[index], bindings)
                } &&
                matchType(expected.result, fn.result, bindings)
        }
    }

    private fun constraintAccepts(constraint: TypeConstraint, type: IdDslType): Boolean = when (constraint) {
        TypeConstraint.Numeric -> type is IdDslType.Scalar && type.name in NUMERIC
        TypeConstraint.Concat ->
            (type is IdDslType.Scalar && type.name == "String") || type is IdDslType.ListType
        TypeConstraint.Equatable -> when (type) {
            is IdDslType.Scalar -> true
            is IdDslType.ListType -> constraintAccepts(constraint, type.element)
            is IdDslType.FunctionType -> false
        }
        is TypeConstraint.Named -> false
    }

    private fun checkFlip(declared: IdDslType.FunctionType, operator: IOperator) {
        val inner = declared.parameters.singleOrNull() as? IdDslType.FunctionType
            ?: throw IdNativeContractError(
                IdDiagnosticCode.E_FN_SIGNATURE,
                "operator '${operator.uniqueName}' native constant must be Fn(Fn(a,b,...)->r)->Fn(b,a,...)->r",
            )
        if (inner.parameters.size < 2) {
            throw IdNativeContractError(
                IdDiagnosticCode.E_FN_SIGNATURE,
                "operator '${operator.uniqueName}' needs an inner function with at least two parameters",
            )
        }
        val outer = declared.result as? IdDslType.FunctionType
            ?: throw IdNativeContractError(
                IdDiagnosticCode.E_FN_SIGNATURE,
                "operator '${operator.uniqueName}' native constant must return a function",
            )
        val swapped = inner.parameters.toMutableList()
        val first = swapped[0]
        swapped[0] = swapped[1]
        swapped[1] = first
        if (outer.parameters != swapped || outer.result != inner.result) {
            throw IdNativeContractError(
                IdDiagnosticCode.E_FN_SIGNATURE,
                "operator '${operator.uniqueName}' native constant must swap the first two inner parameters",
            )
        }
    }

    /**
     * Re-derives a call's concrete output type from its real operator object and already checked
     * argument types. Throws [IdNativeContractError] when the operator's output is not provable.
     */
    fun inferCall(operator: IOperator, arguments: List<IdNativeTyped>): IdNativeTyped {
        val args = arguments.map { it.type }
        val nativeArguments = arguments.map { nativeType(it.type) }.toTypedArray()
        operator.validateTypes(nativeArguments)?.let { error ->
            throw IdNativeContractError(IdDiagnosticCode.E_TYPE, error.string)
        }

        val id = operator.uniqueName.toString()
        val result = when {
            matches(operator, Operators.GENERAL_CHOICE) -> generalChoice(args)
            matches(operator, Operators.GENERAL_IDENTITY) -> {
                requireArgs(id, args, 1)
                args[0]
            }
            matches(operator, Operators.GENERAL_CONSTANT) -> {
                requireArgs(id, args, 2)
                args[0]
            }
            matches(operator, Operators.OPERATOR_APPLY) -> apply(args)
            matches(operator, Operators.OPERATOR_APPLY_0) -> {
                requireArgs(id, args, 1)
                val fn = function(id, args[0])
                if (fn.parameters.isNotEmpty()) {
                    throw IdNativeContractError(
                        IdDiagnosticCode.E_FN_SIGNATURE,
                        "operator_apply0 needs a zero-argument function, got ${fn.display()}",
                    )
                }
                fn.result
            }
            matches(operator, Operators.OPERATOR_MAP) -> map(args)
            matches(operator, Operators.OPERATOR_FILTER) -> filter(args)
            matches(operator, Operators.OPERATOR_CONJUNCTION) ||
                matches(operator, Operators.OPERATOR_DISJUNCTION) -> conjunction(id, args)
            matches(operator, Operators.OPERATOR_NEGATION) -> negation(id, args)
            matches(operator, Operators.OPERATOR_PIPE) -> pipe(args)
            matches(operator, Operators.OPERATOR_PIPE2) -> pipe2(args)
            matches(operator, Operators.OPERATOR_FLIP) -> flip(args)
            matches(operator, Operators.OPERATOR_REDUCE) -> reduce(args)
            matches(operator, Operators.LIST_LAZYBUILT) -> lazyBuilt(args)
            matches(operator, Operators.LIST_LENGTH) -> {
                requireList(id, args, 0)
                IdDslTypes.INTEGER
            }
            matches(operator, Operators.LIST_EMPTY) || matches(operator, Operators.LIST_NOT_EMPTY) -> {
                requireList(id, args, 0)
                IdDslTypes.BOOLEAN
            }
            matches(operator, Operators.LIST_ELEMENT) -> listElement(id, args)
            matches(operator, Operators.LIST_ELEMENT_DEFAULT) -> listElementDefault(id, args)
            matches(operator, Operators.LIST_APPEND) -> listAppend(id, args)
            matches(operator, Operators.LIST_CONCAT) -> listConcat(id, args)
            matches(operator, Operators.LIST_SLICE) -> listSlice(id, args)
            matches(operator, Operators.OBJECT_ITEMSTACK_LIST_COUNT) -> {
                requireArgs(id, args, 2)
                kind(id, args[0], IdDslType.ListType(IdDslTypes.ITEM))
                kind(id, args[1], IdDslTypes.ITEM)
                IdDslTypes.INTEGER
            }
            matches(operator, Operators.OBJECT_ITEMSTACK_MODNAME) -> {
                requireArgs(id, args, 1)
                kind(id, args[0], IdDslTypes.ITEM)
                IdDslTypes.STRING
            }
            matches(operator, Operators.OBJECT_ITEMSTACK_ISSTACKABLE) ||
                matches(operator, Operators.OBJECT_ITEMSTACK_ISENCHANTABLE) -> {
                requireArgs(id, args, 1)
                kind(id, args[0], IdDslTypes.ITEM)
                IdDslTypes.BOOLEAN
            }
            matches(operator, Operators.RELATIONAL_EQUALS) || matches(operator, Operators.RELATIONAL_NOTEQUALS) -> {
                requireArgs(id, args, 2)
                if (args[0] != args[1]) {
                    throw IdNativeContractError(
                        IdDiagnosticCode.E_TYPE,
                        "operator '$id' requires two equal input types, got ${args[0].display()} and ${args[1].display()}",
                    )
                }
                IdDslTypes.BOOLEAN
            }
            matches(operator, Operators.RELATIONAL_GT) || matches(operator, Operators.RELATIONAL_LT) ||
                matches(operator, Operators.RELATIONAL_GE) || matches(operator, Operators.RELATIONAL_LE) -> {
                requireArgs(id, args, 2)
                sameNumeric(id, args[0], args[1])
                IdDslTypes.BOOLEAN
            }
            matches(operator, Operators.ARITHMETIC_ADDITION) || matches(operator, Operators.ARITHMETIC_SUBTRACTION) ||
                matches(operator, Operators.ARITHMETIC_MULTIPLICATION) || matches(operator, Operators.ARITHMETIC_DIVISION) ||
                matches(operator, Operators.ARITHMETIC_MODULUS) || matches(operator, Operators.ARITHMETIC_MINIMUM) ||
                matches(operator, Operators.ARITHMETIC_MAXIMUM) -> {
                requireArgs(id, args, 2)
                sameNumeric(id, args[0], args[1])
            }
            matches(operator, Operators.ARITHMETIC_INCREMENT) || matches(operator, Operators.ARITHMETIC_DECREMENT) -> {
                requireArgs(id, args, 1)
                numeric(id, args[0])
            }
            matches(operator, Operators.NUMBER_CAST_INTEGER) -> cast(id, args, IdDslTypes.INTEGER)
            matches(operator, Operators.NUMBER_CAST_LONG) -> cast(id, args, IdDslTypes.LONG)
            matches(operator, Operators.NUMBER_CAST_DOUBLE) -> cast(id, args, IdDslTypes.DOUBLE)
            matches(operator, Operators.NUMBER_ROUND) || matches(operator, Operators.NUMBER_CEIL) ||
                matches(operator, Operators.NUMBER_FLOOR) -> {
                requireArgs(id, args, 1)
                numeric(id, args[0])
                IdDslTypes.INTEGER
            }
            matches(operator, Operators.NUMBER_COMPACT) -> {
                requireArgs(id, args, 1)
                numeric(id, args[0])
                IdDslTypes.STRING
            }
            matches(operator, Operators.LOGICAL_AND) || matches(operator, Operators.LOGICAL_OR) -> {
                requireArgs(id, args, 2)
                kind(id, args[0], IdDslTypes.BOOLEAN)
                kind(id, args[1], IdDslTypes.BOOLEAN)
                IdDslTypes.BOOLEAN
            }
            matches(operator, Operators.LOGICAL_NOT) -> {
                requireArgs(id, args, 1)
                kind(id, args[0], IdDslTypes.BOOLEAN)
                IdDslTypes.BOOLEAN
            }
            matches(operator, Operators.STRING_CONCAT) -> {
                requireArgs(id, args, 2)
                kind(id, args[0], IdDslTypes.STRING)
                kind(id, args[1], IdDslTypes.STRING)
                IdDslTypes.STRING
            }
            else -> directOutput(operator)
        }

        return IdNativeTyped(result, finiteOfResult(result, operator, arguments))
    }

    /**
     * Conservative finiteness of a call result. Only operators whose construction proves the
     * list's boundedness are marked finite; an unknown list stays `unknown`.
     */
    fun finiteOfResult(result: IdDslType, operator: IOperator, arguments: List<IdNativeTyped>): IdFiniteness {
        if (result !is IdDslType.ListType) return IdFiniteness.FINITE
        return when {
            matches(operator, Operators.LIST_LAZYBUILT) -> IdFiniteness.INFINITE
            matches(operator, Operators.OPERATOR_MAP) -> arguments.getOrNull(1)?.finite ?: IdFiniteness.UNKNOWN
            matches(operator, Operators.OPERATOR_FILTER) -> arguments.getOrNull(1)?.finite ?: IdFiniteness.UNKNOWN
            matches(operator, Operators.LIST_APPEND) -> arguments.getOrNull(0)?.finite ?: IdFiniteness.UNKNOWN
            matches(operator, Operators.LIST_CONCAT) -> combineFinite(
                arguments.getOrNull(0)?.finite ?: IdFiniteness.UNKNOWN,
                arguments.getOrNull(1)?.finite ?: IdFiniteness.UNKNOWN,
            )
            // The native slice result length is min(list length, to) - from, an int: even an
            // infinite source produces a bounded result.
            matches(operator, Operators.LIST_SLICE) -> IdFiniteness.FINITE
            matches(operator, Operators.GENERAL_CHOICE) -> {
                val yes = arguments.getOrNull(1)?.finite ?: IdFiniteness.UNKNOWN
                val no = arguments.getOrNull(2)?.finite ?: IdFiniteness.UNKNOWN
                if (yes == no) yes else IdFiniteness.UNKNOWN
            }
            else -> IdFiniteness.UNKNOWN
        }
    }

    private fun combineFinite(a: IdFiniteness, b: IdFiniteness): IdFiniteness = when {
        a == IdFiniteness.INFINITE || b == IdFiniteness.INFINITE -> IdFiniteness.INFINITE
        a == IdFiniteness.FINITE && b == IdFiniteness.FINITE -> IdFiniteness.FINITE
        else -> IdFiniteness.UNKNOWN
    }

    private fun matches(operator: IOperator, candidate: IOperator): Boolean = operator === candidate

    private fun requireArgs(id: String, args: List<IdDslType>, count: Int) {
        if (args.size != count) {
            throw IdNativeContractError(
                IdDiagnosticCode.E_TYPE,
                "operator '$id' takes $count argument(s), got ${args.size}",
            )
        }
    }

    private fun function(id: String, type: IdDslType): IdDslType.FunctionType = type as? IdDslType.FunctionType
        ?: throw IdNativeContractError(
            IdDiagnosticCode.E_FN_SIGNATURE,
            "operator '$id' needs a function value, got ${type.display()}",
        )

    private fun kind(id: String, type: IdDslType, expected: IdDslType) {
        if (type != expected) {
            throw IdNativeContractError(
                IdDiagnosticCode.E_TYPE,
                "operator '$id' expects ${expected.display()}, got ${type.display()}",
            )
        }
    }

    private fun numeric(id: String, type: IdDslType): IdDslType {
        val scalar = type as? IdDslType.Scalar
        if (scalar == null || scalar.name !in NUMERIC) {
            throw IdNativeContractError(
                IdDiagnosticCode.E_TYPE,
                "operator '$id' needs a numeric value, got ${type.display()}",
            )
        }
        return scalar
    }

    private fun sameNumeric(id: String, left: IdDslType, right: IdDslType): IdDslType {
        val a = numeric(id, left)
        val b = numeric(id, right)
        if (a != b) {
            throw IdNativeContractError(
                IdDiagnosticCode.E_TYPE,
                "operator '$id' requires the same numeric type on both sides, got ${a.display()} and ${b.display()}",
            )
        }
        return a
    }

    private fun requireList(id: String, args: List<IdDslType>, index: Int): IdDslType.ListType =
        args.getOrNull(index) as? IdDslType.ListType
            ?: throw IdNativeContractError(
                IdDiagnosticCode.E_TYPE,
                "operator '$id' argument ${index + 1} must be a list",
            )

    private fun generalChoice(args: List<IdDslType>): IdDslType {
        requireArgs("general_choice", args, 3)
        kind("general_choice", args[0], IdDslTypes.BOOLEAN)
        if (args[1] != args[2]) {
            throw IdNativeContractError(
                IdDiagnosticCode.E_TYPE,
                "general_choice branches must have the same type, got ${args[1].display()} and ${args[2].display()}",
            )
        }
        return args[1]
    }

    private fun apply(args: List<IdDslType>): IdDslType {
        requireArgs("operator_apply", args, 2)
        val fn = function("operator_apply", args[0])
        if (fn.parameters.isEmpty()) {
            throw IdNativeContractError(
                IdDiagnosticCode.E_FN_SIGNATURE,
                "operator_apply needs a function with at least one argument",
            )
        }
        if (fn.parameters[0] != args[1]) {
            throw IdNativeContractError(
                IdDiagnosticCode.E_FN_SIGNATURE,
                "operator_apply argument ${args[1].display()} does not match ${fn.parameters[0].display()}",
            )
        }
        return if (fn.parameters.size == 1) {
            fn.result
        } else {
            IdDslTypes.fn(fn.parameters.drop(1), result = fn.result)
        }
    }

    private fun map(args: List<IdDslType>): IdDslType {
        requireArgs("operator_map", args, 2)
        val fn = function("operator_map", args[0])
        if (fn.parameters.size != 1) {
            throw IdNativeContractError(
                IdDiagnosticCode.E_FN_SIGNATURE,
                "operator_map requires a one-argument function, got ${fn.display()}",
            )
        }
        val list = requireList("operator_map", args, 1)
        if (list.element != fn.parameters[0]) {
            throw IdNativeContractError(
                IdDiagnosticCode.E_FN_SIGNATURE,
                "operator_map function input ${fn.parameters[0].display()} does not match list element ${list.element.display()}",
            )
        }
        return IdDslType.ListType(fn.result)
    }

    private fun filter(args: List<IdDslType>): IdDslType {
        requireArgs("operator_filter", args, 2)
        val fn = function("operator_filter", args[0])
        if (fn.parameters.size != 1 || fn.result != IdDslTypes.BOOLEAN) {
            throw IdNativeContractError(
                IdDiagnosticCode.E_FN_SIGNATURE,
                "operator_filter requires a predicate Fn(A) -> Boolean, got ${fn.display()}",
            )
        }
        val list = requireList("operator_filter", args, 1)
        if (list.element != fn.parameters[0]) {
            throw IdNativeContractError(
                IdDiagnosticCode.E_FN_SIGNATURE,
                "operator_filter predicate input ${fn.parameters[0].display()} does not match list element ${list.element.display()}",
            )
        }
        return list
    }

    private fun conjunction(id: String, args: List<IdDslType>): IdDslType {
        requireArgs(id, args, 2)
        val left = function(id, args[0])
        val right = function(id, args[1])
        val predicate = IdDslTypes.fn(listOf(left.parameters.singleOrNull()
            ?: throw IdNativeContractError(IdDiagnosticCode.E_FN_SIGNATURE, "operator '$id' requires one-argument predicates")),
            result = IdDslTypes.BOOLEAN)
        if (left != predicate || right != predicate) {
            throw IdNativeContractError(
                IdDiagnosticCode.E_FN_SIGNATURE,
                "operator '$id' requires two identical Fn(A) -> Boolean predicates",
            )
        }
        return predicate
    }

    private fun negation(id: String, args: List<IdDslType>): IdDslType {
        requireArgs(id, args, 1)
        val fn = function(id, args[0])
        if (fn.parameters.size != 1 || fn.result != IdDslTypes.BOOLEAN) {
            throw IdNativeContractError(
                IdDiagnosticCode.E_FN_SIGNATURE,
                "operator '$id' requires a predicate Fn(A) -> Boolean",
            )
        }
        return fn
    }

    private fun pipe(args: List<IdDslType>): IdDslType {
        requireArgs("operator_pipe", args, 2)
        val left = function("operator_pipe", args[0])
        val right = function("operator_pipe", args[1])
        if (left.parameters.size != 1) {
            throw IdNativeContractError(IdDiagnosticCode.E_FN_SIGNATURE, "operator_pipe first function must take one argument")
        }
        if (right.parameters.isEmpty() || right.parameters[0] != left.result) {
            throw IdNativeContractError(
                IdDiagnosticCode.E_FN_SIGNATURE,
                "operator_pipe second function must accept ${left.result.display()}",
            )
        }
        return IdDslTypes.fn(listOf(left.parameters[0]) + right.parameters.drop(1), result = right.result)
    }

    private fun pipe2(args: List<IdDslType>): IdDslType {
        requireArgs("operator_pipe2", args, 3)
        val first = function("operator_pipe2", args[0])
        val second = function("operator_pipe2", args[1])
        val third = function("operator_pipe2", args[2])
        if (first.parameters.size != 1 || second.parameters.size != 1) {
            throw IdNativeContractError(IdDiagnosticCode.E_FN_SIGNATURE, "operator_pipe2 first two functions must take one argument")
        }
        if (first.parameters[0] != second.parameters[0]) {
            throw IdNativeContractError(IdDiagnosticCode.E_FN_SIGNATURE, "operator_pipe2 first two functions must accept the same type")
        }
        if (third.parameters.size < 2 || third.parameters[0] != first.result || third.parameters[1] != second.result) {
            throw IdNativeContractError(
                IdDiagnosticCode.E_FN_SIGNATURE,
                "operator_pipe2 third function must accept ${first.result.display()} and ${second.result.display()}",
            )
        }
        return IdDslTypes.fn(listOf(first.parameters[0]) + third.parameters.drop(2), result = third.result)
    }

    private fun flip(args: List<IdDslType>): IdDslType {
        requireArgs("operator_flip", args, 1)
        val fn = function("operator_flip", args[0])
        if (fn.parameters.size < 2) {
            throw IdNativeContractError(IdDiagnosticCode.E_FN_SIGNATURE, "operator_flip needs a function with at least two inputs")
        }
        val parameters = fn.parameters.toMutableList()
        val first = parameters[0]
        parameters[0] = parameters[1]
        parameters[1] = first
        return IdDslTypes.fn(parameters, result = fn.result)
    }

    private fun reduce(args: List<IdDslType>): IdDslType {
        requireArgs("operator_reduce", args, 3)
        val fn = function("operator_reduce", args[0])
        if (fn.parameters.size != 2 || fn.parameters[0] != fn.result) {
            throw IdNativeContractError(
                IdDiagnosticCode.E_FN_SIGNATURE,
                "operator_reduce requires Fn(A, B) -> A, got ${fn.display()}",
            )
        }
        val list = requireList("operator_reduce", args, 1)
        kind("operator_reduce", list.element, fn.parameters[1])
        kind("operator_reduce", args[2], fn.parameters[0])
        return fn.parameters[0]
    }

    private fun lazyBuilt(args: List<IdDslType>): IdDslType {
        requireArgs("list_lazybuilt", args, 2)
        val fn = function("list_lazybuilt", args[1])
        if (fn.parameters.size != 1 || fn.parameters[0] != args[0] || fn.result != args[0]) {
            throw IdNativeContractError(
                IdDiagnosticCode.E_FN_SIGNATURE,
                "list_lazybuilt requires Fn(A) -> A matching the seed type",
            )
        }
        return IdDslType.ListType(args[0])
    }

    private fun listElement(id: String, args: List<IdDslType>): IdDslType {
        requireArgs(id, args, 2)
        val list = requireList(id, args, 0)
        kind(id, args[1], IdDslTypes.INTEGER)
        return list.element
    }

    private fun listElementDefault(id: String, args: List<IdDslType>): IdDslType {
        requireArgs(id, args, 3)
        val list = requireList(id, args, 0)
        kind(id, args[1], IdDslTypes.INTEGER)
        kind(id, args[2], list.element)
        return list.element
    }

    private fun listAppend(id: String, args: List<IdDslType>): IdDslType {
        requireArgs(id, args, 2)
        val list = requireList(id, args, 0)
        kind(id, args[1], list.element)
        return list
    }

    private fun listConcat(id: String, args: List<IdDslType>): IdDslType {
        requireArgs(id, args, 2)
        val left = requireList(id, args, 0)
        val right = requireList(id, args, 1)
        if (left.element != right.element) {
            throw IdNativeContractError(
                IdDiagnosticCode.E_TYPE,
                "operator '$id' lists have different element types",
            )
        }
        return left
    }

    private fun listSlice(id: String, args: List<IdDslType>): IdDslType {
        requireArgs(id, args, 3)
        val list = requireList(id, args, 0)
        kind(id, args[1], IdDslTypes.INTEGER)
        kind(id, args[2], IdDslTypes.INTEGER)
        return list
    }

    private fun cast(id: String, args: List<IdDslType>, result: IdDslType.Scalar): IdDslType {
        requireArgs(id, args, 1)
        numeric(id, args[0])
        return result
    }

    /**
     * The fallback for operators not covered by a generic rule: the real operator must declare a
     * concrete scalar output. List/operator/category outputs without a rule are never guessed.
     */
    private fun directOutput(operator: IOperator): IdDslType {
        val output = operator.outputType
        if (output === ValueTypes.LIST) {
            throw IdNativeContractError(
                IdDiagnosticCode.E_NATIVE_CONTRACT,
                "operator '${operator.uniqueName}' produces a list whose element type this server cannot prove; " +
                    "use a supported list operator",
            )
        }
        if (output === ValueTypes.OPERATOR || output.isCategory) {
            throw IdNativeContractError(
                IdDiagnosticCode.E_NATIVE_CONTRACT,
                "operator '${operator.uniqueName}' has no concrete output type in the pinned contract",
            )
        }
        return scalarOf(output)
            ?: throw IdNativeContractError(
                IdDiagnosticCode.E_NATIVE_CONTRACT,
                "operator '${operator.uniqueName}' output type '${output.uniqueName}' is not a supported core type",
            )
    }
}
