package io.github.supermonster003.autojs6.plugin.three.prism.camera

import java.io.DataInputStream

/**
 * Reads the `ConstantValue` of a static field straight from a class file, so a JVM test can check constants of a class
 * it must not initialize (an AIDL stub extends `android.os.Binder`, a stub in the mockable `android.jar`).
 * zh-CN: 直接从类文件读取静态字段的 `ConstantValue`, 使 JVM 测试可以检查不能初始化的类的常量 (AIDL Stub 继承自可模拟 `android.jar` 中的
 * `android.os.Binder` 桩).
 */
internal object ClassConstants {

    fun int(type: Class<*>, field: String): Int {
        val resource = type.name.replace('.', '/') + ".class"
        val stream = type.classLoader?.getResourceAsStream(resource) ?: error("$resource is not on the class path")
        DataInputStream(stream.buffered()).use { input ->
            check(input.readInt() == 0xCAFEBABE.toInt()) { "$resource is not a class file" }
            input.readUnsignedShort()
            input.readUnsignedShort()
            val count = input.readUnsignedShort()
            val pool = arrayOfNulls<Any>(count)
            var index = 1
            while (index < count) {
                when (val tag = input.readUnsignedByte()) {
                    1 -> pool[index] = input.readUTF()
                    3 -> pool[index] = input.readInt()
                    4 -> input.readInt()
                    5, 6 -> {
                        input.readLong()
                        index++
                    }
                    7, 8, 16, 19, 20 -> input.readUnsignedShort()
                    9, 10, 11, 12, 17, 18 -> input.readInt()
                    15 -> {
                        input.readUnsignedByte()
                        input.readUnsignedShort()
                    }
                    else -> error("$resource: unknown constant pool tag $tag")
                }
                index++
            }
            input.readUnsignedShort()
            input.readUnsignedShort()
            input.readUnsignedShort()
            repeat(input.readUnsignedShort()) { input.readUnsignedShort() }
            repeat(input.readUnsignedShort()) {
                input.readUnsignedShort()
                val name = pool[input.readUnsignedShort()]
                input.readUnsignedShort()
                repeat(input.readUnsignedShort()) {
                    val attribute = pool[input.readUnsignedShort()]
                    val length = input.readInt()
                    if (name == field && attribute == "ConstantValue") {
                        return pool[input.readUnsignedShort()] as? Int ?: error("$resource: $field is not an int constant")
                    }
                    input.skipNBytes(length.toLong())
                }
            }
            error("$resource: no constant $field")
        }
    }

}
