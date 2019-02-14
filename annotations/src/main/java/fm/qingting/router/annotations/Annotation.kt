package fm.qingting.router.annotations

import kotlin.reflect.KClass


/**
 *  Created by lee on 2018/3/12.
 */

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class HookParams(val targetClass: Array<KClass<*>>, val policy: Policy = Policy.INCLUDE, val scope: Array<KClass<*>> = [], val impactPackage: Array<String> = [], val isStatic: Boolean = true)

enum class Policy(i: kotlin.Int) {
    INCLUDE(1), EXCLUDE(2)
}
