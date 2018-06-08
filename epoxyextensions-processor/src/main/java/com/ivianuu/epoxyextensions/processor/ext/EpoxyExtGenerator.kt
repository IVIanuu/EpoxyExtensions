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

package com.ivianuu.epoxyextensions.processor.ext

import com.ivianuu.epoxyextensions.processor.EPOXY_CONTROLLER
import com.squareup.kotlinpoet.*

/**
 * @author Manuel Wrage (IVIanuu)
 */
class EpoxyExtGenerator(val descriptor: EpoxyExtDescriptor) {

    fun generate(): FileSpec {
        return FileSpec.builder(
            descriptor.fileName.packageName(), descriptor.fileName.simpleName())
            .addFunction(builderInit())
            .build()
    }

    private fun builderInit(): FunSpec {
        val function = FunSpec.builder(descriptor.initName)
            .receiver(EPOXY_CONTROLLER)

        descriptor.constructorParams
            .forEach { function.addParameter(it.first, it.second) }

        function.addParameter(
            "initializer",
            LambdaTypeName.get(
                receiver = descriptor.builderName,
                returnType = UNIT
            )
        )

        val constructorString = descriptor.constructorParams
            .map(Pair<String, TypeName>::first)
            .joinToString(",")

        function.addCode(
            CodeBlock.builder()
                .addStatement("val model = %T($constructorString)", descriptor.modelName)
                .addStatement("val builder = %T(model)", descriptor.builderName)
                .addStatement("initializer.invoke(builder)")
                .addStatement("model.addTo(this)")
                .build()
        )

        return function.build()
    }
}