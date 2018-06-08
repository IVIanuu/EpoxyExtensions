/*
 * Copyright 2018 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.epoxyextensions.processor.builder

import com.squareup.kotlinpoet.*

/**
 * @author Manuel Wrage (IVIanuu)
 */
class EpoxyBuilderGenerator(val descriptor: EpoxyBuilderDescriptor) {

    fun generate(): FileSpec {
        val file = FileSpec.builder(
            descriptor.builderName.packageName(), descriptor.builderName.simpleName())

        file.addType(epoxyModelBuilder())

        return file.build()
    }

    private fun epoxyModelBuilder(): TypeSpec {
        val type = TypeSpec.classBuilder(descriptor.builderName)
            .addModifiers(KModifier.OPEN)

        type.addProperty(modelField())
        type.primaryConstructor(constructor())

        if (descriptor.parent != null) {
            type.superclass(descriptor.parent)
            type.addSuperclassConstructorParameter("model")
        }

        descriptor.attrs
            .map { attributeSetter(it) }
            .forEach { type.addFunction(it) }

        return type.build()
    }

    private fun constructor(): FunSpec {
        return FunSpec.constructorBuilder()
            .addParameter("model", descriptor.target)
            .build()
    }

    private fun modelField(): PropertySpec {
        return PropertySpec.builder("model", descriptor.target)
            .initializer("model")
            .apply {
                if (descriptor.parent != null) {
                    addModifiers(KModifier.OVERRIDE)
                } else {
                    addModifiers(KModifier.OPEN)
                }
            }
            .build()
    }

    private fun attributeSetter(attr: EpoxyAttrDescriptor): FunSpec {
        val function = FunSpec.builder(attr.name)

        function.addParameter(attr.name, attr.type)

        if (attr.isOverridenBySuperClass) {
            function.addModifiers(KModifier.OVERRIDE)
        } else {
            function.addModifiers(KModifier.OPEN)
        }

        if (attr.isOverridenBySuperClass) {
            function.addCode(
                CodeBlock.builder()
                    .addStatement("super.${attr.name}(${attr.name})")
                    .addStatement("return this")
                    .build()
            )
        } else {
            function.addCode(
                CodeBlock.builder()
                    .addStatement("model.${attr.name} = ${attr.name}")
                    .addStatement("return this")
                    .build()
            )
        }

        function.returns(descriptor.builderName)

        return function.build()
    }

}