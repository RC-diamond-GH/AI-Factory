package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.server

import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdDiagnosticCode
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdDslType
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdDslTypes
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import net.minecraft.nbt.TagParser
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.registries.ForgeRegistries
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue
import org.cyclops.integrateddynamics.api.evaluate.variable.IValueType
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeBlock
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeFluidStack
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueObjectTypeItemStack
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeBoolean
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeDouble
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeInteger
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeList
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeLong
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeNbt
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypeString
import java.nio.charset.StandardCharsets

/**
 * Exact conversion of a plan's `value` node JSON into a real Integrated Dynamics value.
 *
 * The caller has already checked the node's declared type; this object independently validates the
 * literal shape, resource registry presence, SNBT and ranges. It creates prototype item/block/fluid
 * values (never player items or arbitrary NBT stacks) and never evaluates an expression.
 */
internal object IdNativeValues {
    fun value(type: IdDslType, json: JsonElement, node: Int): IValue {
        return when (type) {
            is IdDslType.Scalar -> scalar(type, json, node)
            is IdDslType.ListType -> list(type, json, node)
            is IdDslType.FunctionType -> reject(
                node,
                IdDiagnosticCode.E_LITERAL_RESOURCE,
                "a function value is not a value literal; use a native operator node or a call",
            )
        }
    }

    fun isEmptyValue(type: IdDslType, json: JsonElement): Boolean =
        json is JsonNull && type is IdDslType.Scalar && (type.name == "Item" || type.name == "Fluid")

    private fun scalar(type: IdDslType.Scalar, json: JsonElement, node: Int): IValue = when (type.name) {
        "Boolean" -> {
            val value = primitive(json, node)?.takeIf { !it.isString }?.booleanOrNull
                ?: reject(node, IdDiagnosticCode.E_TYPE, "Boolean literal must be true or false")
            ValueTypeBoolean.ValueBoolean.of(value)
        }
        "Integer" -> {
            val value = primitive(json, node)?.takeIf { !it.isString }?.intOrNull
                ?: reject(node, IdDiagnosticCode.E_LITERAL_RANGE, "Integer literal must fit a signed 32-bit integer")
            ValueTypeInteger.ValueInteger.of(value)
        }
        "Long" -> {
            val value = primitive(json, node)?.takeIf { !it.isString }?.longOrNull
                ?: reject(node, IdDiagnosticCode.E_LITERAL_RANGE, "Long literal must fit a signed 64-bit integer")
            ValueTypeLong.ValueLong.of(value)
        }
        "Double" -> {
            val value = primitive(json, node)?.takeIf { !it.isString }?.doubleOrNull
                ?: reject(node, IdDiagnosticCode.E_LITERAL_RANGE, "Double literal must be a finite number")
            if (!value.isFinite()) {
                reject(node, IdDiagnosticCode.E_LITERAL_RANGE, "Double literal must be finite")
            }
            ValueTypeDouble.ValueDouble.of(value)
        }
        "String" -> {
            val text = string(json, node, "String")
            ValueTypeString.ValueString.of(text)
        }
        "NBT" -> {
            val text = string(json, node, "NBT")
            val tag = try {
                TagParser.parseTag(text)
            } catch (_: Exception) {
                reject(node, IdDiagnosticCode.E_LITERAL_NBT, "NBT literal is not valid SNBT")
            }
            ValueTypeNbt.ValueNbt.of(tag)
        }
        "Item" -> {
            if (json is JsonNull) {
                ValueObjectTypeItemStack.ValueItemStack.of(ItemStack.EMPTY)
            } else {
                val resource = resource(json, node)
                val item = ForgeRegistries.ITEMS.getValue(resource)
                    ?: reject(node, IdDiagnosticCode.E_LITERAL_RESOURCE, "item '$resource' is not registered")
                val stack = ItemStack(item)
                if (stack.isEmpty) {
                    reject(
                        node,
                        IdDiagnosticCode.E_LITERAL_RESOURCE,
                        "item '$resource' resolves to an empty stack; use the explicit emptyItem value",
                    )
                }
                ValueObjectTypeItemStack.ValueItemStack.of(stack)
            }
        }
        "Block" -> {
            if (json is JsonNull) {
                reject(node, IdDiagnosticCode.E_LITERAL_RESOURCE, "Block has no empty literal; give a block resource")
            }
            val resource = resource(json, node)
            val block = ForgeRegistries.BLOCKS.getValue(resource)
                ?: reject(node, IdDiagnosticCode.E_LITERAL_RESOURCE, "block '$resource' is not registered")
            ValueObjectTypeBlock.ValueBlock.of(block.defaultBlockState())
        }
        "Fluid" -> {
            if (json is JsonNull) {
                ValueObjectTypeFluidStack.ValueFluidStack.of(FluidStack.EMPTY)
            } else {
                val objectValue = exactObject(json, node, setOf("resource", "amount"), "Fluid")
                val resource = parseResource(objectValue["resource"], node, "Fluid")
                val amount = (objectValue["amount"] as? JsonPrimitive)
                    ?.takeIf { !it.isString }?.intOrNull
                    ?: reject(node, IdDiagnosticCode.E_LITERAL_RANGE, "Fluid amount must be a positive Integer")
                if (amount <= 0) {
                    reject(node, IdDiagnosticCode.E_LITERAL_RANGE, "Fluid amount must be positive")
                }
                val fluid = ForgeRegistries.FLUIDS.getValue(resource)
                    ?: reject(node, IdDiagnosticCode.E_LITERAL_RESOURCE, "fluid '$resource' is not registered")
                ValueObjectTypeFluidStack.ValueFluidStack.of(FluidStack(fluid, amount))
            }
        }
        "Entity" -> reject(
            node,
            IdDiagnosticCode.E_LITERAL_RESOURCE,
            "Entity has no string constructor; bind a typed Entity input instead",
        )
        "Ingredients" -> reject(
            node,
            IdDiagnosticCode.E_LITERAL_RESOURCE,
            "Ingredients has no string constructor; bind a typed Ingredients input instead",
        )
        "Recipe" -> reject(
            node,
            IdDiagnosticCode.E_LITERAL_RESOURCE,
            "Recipe has no string constructor; bind a typed Recipe input instead",
        )
        else -> reject(node, IdDiagnosticCode.E_UNKNOWN_TYPE, "type '${type.name}' has no native literal encoder")
    }

    private fun list(type: IdDslType.ListType, json: JsonElement, node: Int): IValue {
        val array = json as? JsonArray
            ?: reject(node, IdDiagnosticCode.E_TYPE, "List literal must be a JSON array")
        if (array.size > IdNativeCatalog.MAX_LIST_LENGTH) {
            reject(
                node,
                IdDiagnosticCode.E_LIMIT,
                "List literal has ${array.size} elements; the limit is ${IdNativeCatalog.MAX_LIST_LENGTH}",
            )
        }
        val elementType = type.element
        val elementNative = IdNativeCatalog.nativeType(elementType)
        val values = array.map { element -> value(elementType, element, node) }
        @Suppress("UNCHECKED_CAST")
        return ValueTypeList.ValueList.ofList(elementNative as IValueType<IValue>, values)
    }

    private fun resource(json: JsonElement, node: Int): ResourceLocation {
        val objectValue = exactObject(json, node, setOf("resource"), "object literal")
        return parseResource(objectValue["resource"], node, "resource")
    }

    private fun parseResource(value: JsonElement?, node: Int, label: String): ResourceLocation {
        val text = (value as? JsonPrimitive)?.takeIf { it.isString }?.content
            ?: reject(node, IdDiagnosticCode.E_LITERAL_RESOURCE, "$label must be a resource-id string")
        if (text.toByteArray(StandardCharsets.UTF_8).size > IdNativeCatalog.MAX_LITERAL_BYTES) {
            reject(node, IdDiagnosticCode.E_LIMIT, "$label is too long")
        }
        if ('!' in text || ':' !in text) {
            reject(node, IdDiagnosticCode.E_LITERAL_RESOURCE, "$label must be an explicit namespace:path resource id")
        }
        return ResourceLocation.tryParse(text)
            ?: reject(node, IdDiagnosticCode.E_LITERAL_RESOURCE, "'$text' is not a valid resource id")
    }

    private fun string(json: JsonElement, node: Int, label: String): String {
        val text = (json as? JsonPrimitive)?.takeIf { it.isString }?.content
            ?: reject(node, IdDiagnosticCode.E_TYPE, "$label literal must be a string")
        if (text.toByteArray(StandardCharsets.UTF_8).size > IdNativeCatalog.MAX_LITERAL_BYTES) {
            reject(
                node,
                IdDiagnosticCode.E_LIMIT,
                "$label literal is longer than ${IdNativeCatalog.MAX_LITERAL_BYTES} UTF-8 bytes",
            )
        }
        return text
    }

    private fun primitive(json: JsonElement, node: Int): JsonPrimitive? =
        (json as? JsonPrimitive) ?: reject(node, IdDiagnosticCode.E_TYPE, "literal must be a JSON primitive")

    private fun exactObject(json: JsonElement, node: Int, keys: Set<String>, label: String): JsonObject {
        val objectValue = json as? JsonObject
            ?: reject(node, IdDiagnosticCode.E_TYPE, "$label must be a JSON object")
        val unknown = objectValue.keys - keys
        val missing = keys - objectValue.keys
        if (unknown.isNotEmpty() || missing.isNotEmpty()) {
            reject(node, IdDiagnosticCode.E_TYPE, "$label has an unexpected shape")
        }
        return objectValue
    }

    private fun reject(node: Int, code: IdDiagnosticCode, message: String): Nothing =
        throw IdNativeContractError(code, "node $node: $message")
}
