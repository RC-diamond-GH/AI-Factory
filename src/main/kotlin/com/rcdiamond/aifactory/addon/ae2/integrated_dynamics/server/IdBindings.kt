package com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.server

import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdBinding
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdDiagnosticCode
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdDslWire
import com.rcdiamond.aifactory.addon.ae2.integrated_dynamics.dsl.IdWireException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Parsing of the `id_describe_inputs` `{bindings:{...}}` object.
 *
 * The exact descriptor shapes are the shared [IdDslWire] ones, so describe accepts precisely the
 * binding JSON the compiler can emit in a plan. A binding is a real variable or read-aspect
 * reference, never a caller-supplied type: the server resolves the type itself.
 */
internal object IdServerBindings {
    private val NAME = Regex("[A-Za-z_][A-Za-z0-9_]{0,63}")

    fun parseArgument(arguments: JsonObject): LinkedHashMap<String, IdBinding> {
        val unknown = arguments.keys - setOf("bindings")
        if (unknown.isNotEmpty()) {
            reject("unsupported argument(s) ${unknown.sorted().joinToString(", ")}")
        }
        val bindings = arguments["bindings"] as? JsonObject ?: reject("argument 'bindings' must be an object")
        if (bindings.size > IdNativeCatalog.MAX_INPUTS) {
            reject("at most ${IdNativeCatalog.MAX_INPUTS} input bindings are allowed")
        }
        val parsed = LinkedHashMap<String, IdBinding>()
        for ((name, value) in bindings) {
            if (!NAME.matches(name)) {
                reject("input name '$name' is not an ASCII identifier", name)
            }
            parsed[name] = parseDescriptor(value, name)
        }
        return parsed
    }

    fun resolve(
        bindings: Map<String, IdBinding>,
        partNetwork: org.cyclops.integrateddynamics.api.network.IPartNetwork,
        context: org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext,
    ): Map<String, IdNativeTyped> {
        val facadeTypes = IdFacadeTypes(partNetwork, context)
        val resolved = LinkedHashMap<String, IdNativeTyped>(bindings.size)
        for ((name, binding) in bindings) {
            resolved[name] = when (binding) {
                is IdBinding.Variable -> facadeTypes.variable(binding.id, name)
                is IdBinding.Aspect -> IdAspectContracts.resolve(
                    partNetwork,
                    binding.partId,
                    resource(binding.aspect, name),
                    name,
                )
            }
        }
        return resolved
    }

    fun parseDescriptor(value: JsonElement, input: String?): IdBinding = try {
        IdDslWire.parseBinding(value)
    } catch (invalid: IdWireException) {
        throw IdBackendError.describe(
            invalid.code,
            invalid.message,
            input = input,
            hint = "A binding is either {kind:'variable',id:...} or " +
                "{kind:'aspect',part_id:...,aspect:'namespace:id'}.",
        )
    }

    private fun resource(text: String, input: String?): net.minecraft.resources.ResourceLocation =
        net.minecraft.resources.ResourceLocation.tryParse(text)
            ?: reject("aspect '$text' is not a valid resource id", input)

    private fun reject(message: String, input: String? = null): Nothing =
        throw IdBackendError.describe(IdDiagnosticCode.E_INPUT_BINDING, message, input = input)
}
