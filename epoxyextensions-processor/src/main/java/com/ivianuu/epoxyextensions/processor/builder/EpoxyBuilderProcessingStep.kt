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

import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.common.MoreElements
import com.google.common.collect.SetMultimap
import com.ivianuu.epoxyextensions.processor.javaToKotlinType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

/**
 * @author Manuel Wrage (IVIanuu)
 */
class EpoxyBuilderProcessingStep(private val processingEnv: ProcessingEnvironment) : BasicAnnotationProcessor.ProcessingStep {

    private val generators = mutableSetOf<EpoxyBuilderGenerator>()

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>): MutableSet<Element> {
        elementsByAnnotation[EpoxyModelClass::class.java]
            .filterIsInstance<TypeElement>()
            .map(this::createDescriptor)
            .map(::EpoxyBuilderGenerator)
            .forEach { generators.add(it) }

        return mutableSetOf()
    }

    override fun annotations()
            = mutableSetOf(EpoxyModelClass::class.java)

    fun writeFiles() {
        generators.forEach(this::generateAndWriteFile)
    }

    private fun createDescriptor(element: TypeElement): EpoxyBuilderDescriptor {
        val attrs = collectEpoxyAttrs(element)
        val builderName = ClassName.bestGuess(element.asType().toString() + "Builder_")
        val superclass = findEpoxyModelAnnotatedSuperclass(element)
        val parentBuilder = if (superclass != null) {
            ClassName.bestGuess(superclass.asType().toString() + "Builder_")
        } else {
            null
        }

        return EpoxyBuilderDescriptor(element, element.asClassName(),
            builderName, parentBuilder, attrs)
    }

    private fun findEpoxyModelAnnotatedSuperclass(element: TypeElement): TypeElement? {
        var superclass = element.superclass
        while (superclass != null) {
            val type = processingEnv.elementUtils
                .getTypeElement(superclass.toString()) ?: break

            if (MoreElements.isAnnotationPresent(type, EpoxyModelClass::class.java)) {
                return type
            }

            superclass = type.superclass
        }

        return null
    }

    private fun collectEpoxyAttrs(element: TypeElement): Set<EpoxyAttrDescriptor> {
        val attrsMap =
            mutableMapOf<TypeElement, MutableSet<EpoxyAttrDescriptor>>()

        fun getSetFor(element: TypeElement)
                = attrsMap.getOrPut(element) { mutableSetOf() }

        var superclass = element.asType()

        while (superclass != null) {
            val type = processingEnv.elementUtils
                .getTypeElement(superclass.toString()) ?: break

            type.enclosedElements
                .filterIsInstance<VariableElement>()
                .filter { MoreElements.isAnnotationPresent(it, EpoxyAttribute::class.java) }
                .map { it to it.getAnnotation(EpoxyAttribute::class.java) }
                .filter { (_, annotation) ->
                    !annotation.value.contains(EpoxyAttribute.Option.NoSetter)
                }
                .map(Pair<VariableElement, EpoxyAttribute>::first)
                .map { it.toEpoxyAttrDescriptor() }
                .forEach { getSetFor(type).add(it) }

            superclass = type.superclass
        }

        val attrs = mutableSetOf<EpoxyAttrDescriptor>()

        var lastEpoxyModelIndex = -1

        attrsMap.toList()
            .forEachIndexed { index, (type, typeAttrs) ->
                val isEpoxyModel = MoreElements.isAnnotationPresent(type, EpoxyModelClass::class.java)

                typeAttrs.forEach { attr ->
                    // this our own element so it is not overriden
                    val isOverridenBySuperClass = when {
                        // this our own element so no
                        type == element -> false
                        // this a epoxy model so yes
                        isEpoxyModel -> true
                        // if the index is greater than the last epoxy model class we have to override
                        else -> lastEpoxyModelIndex != -1 && index > lastEpoxyModelIndex
                    }

                    attrs.add(attr.copy(isOverridenBySuperClass = isOverridenBySuperClass))
                }

                // if its a epoxy model super class mark the index
                if (type != element && isEpoxyModel) {
                    lastEpoxyModelIndex = index
                }
            }

        return attrs
    }

    private fun VariableElement.toEpoxyAttrDescriptor(): EpoxyAttrDescriptor {
        return EpoxyAttrDescriptor(
            this, simpleName.toString(),
            javaToKotlinType(),
            false
        )
    }

    private fun generateAndWriteFile(generator: EpoxyBuilderGenerator) {
        val fileUri = processingEnv.filer
            .createSourceFile(generator.descriptor.builderName.packageName()
                    + "." + generator.descriptor.builderName.simpleName()).toUri()

        generator.generate().writeTo(File(fileUri))
    }
}