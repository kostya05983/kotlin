/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jsr223

import org.jetbrains.kotlin.cli.common.repl.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.script.ScriptContext
import javax.script.ScriptEngineFactory
import kotlin.reflect.KClass
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvmhost.repl.JvmReplCompiler
import kotlin.script.experimental.jvmhost.repl.JvmReplEvaluator

class KotlinJsr223ScriptEngine(
    factory: ScriptEngineFactory,
    val getScriptArgs: (ScriptContext, Array<out KClass<out Any>>?) -> ScriptArgsWithTypes?,
    val scriptArgsTypes: Array<out KClass<out Any>>?
) : KotlinJsr223JvmScriptEngineBase(factory), KotlinJsr223JvmInvocableScriptEngine {

    val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<KotlinJsr223DefaultScript> {
        jvm {
            dependenciesFromCurrentContext(wholeClasspath = true)
        }
    }

    val evaluationConfiguration = ScriptEvaluationConfiguration()

    override val replCompiler: ReplCompiler by lazy {
        JvmReplCompiler(compilationConfiguration)
    }
    private val localEvaluator by lazy {
        GenericReplCompilingEvaluatorBase(replCompiler, JvmReplEvaluator(evaluationConfiguration))
    }

    override val replEvaluator: ReplFullEvaluator get() = localEvaluator

    override val state: IReplStageState<*> get() = getCurrentState(getContext())

    override fun createState(lock: ReentrantReadWriteLock): IReplStageState<*> = replEvaluator.createState(lock)

    override fun overrideScriptArgs(context: ScriptContext): ScriptArgsWithTypes? = getScriptArgs(context, scriptArgsTypes)
}
