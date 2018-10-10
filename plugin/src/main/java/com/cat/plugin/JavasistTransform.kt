package com.cat.plugin

import com.android.build.api.transform.*
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream


class JavasistTransform : Transform() {

    private val projectScopes = hashSetOf(QualifiedContent.Scope.PROJECT)

    override fun getName(): String = "InjectTest"

    override fun getInputTypes(): Set<QualifiedContent.ContentType> = setOf(QualifiedContent.DefaultContentType.CLASSES)

    override fun getScopes(): MutableSet<in QualifiedContent.Scope>? = hashSetOf(QualifiedContent.Scope.PROJECT,
            QualifiedContent.Scope.SUB_PROJECTS,
            QualifiedContent.Scope.EXTERNAL_LIBRARIES
    )

    override fun isIncremental(): Boolean = true

    override fun transform(ti: TransformInvocation) {
        val startTime = System.currentTimeMillis()
        val outputProvider = ti.outputProvider
        val outDir = outputProvider.getContentLocation("inject", outputTypes, projectScopes, Format.DIRECTORY)

        val executor = ThreadPoolExecutor(8, 10, 1, TimeUnit.SECONDS,
                LinkedBlockingQueue<Runnable>())
        if (ti.isIncremental) {
            println("QTInject doing incremental build ...")
            ti.inputs.forEach {
                for (jarInput in it.jarInputs) {
                    val jarFile = jarInput.file
                    val status = jarInput.status
                    if (status == Status.NOTCHANGED) {
                        continue
                    }
                    println("Status of file " + jarFile + " is " + jarInput.status)
                    val uniqueName = jarFile.name + "_" + jarFile.absolutePath.hashCode()
                    val jarOutFile = outputProvider.getContentLocation(uniqueName, outputTypes, jarInput.scopes, Format.JAR)
                    if (status == Status.REMOVED) {
                        jarOutFile.delete()
                        continue
                    }
                    executor.execute {
                        processJarFile(jarFile, jarOutFile)
                    }
                }

                it.directoryInputs.forEach {
                    val pathBitLen = it.file.toString().length + 1

                    val changedFiles = it.changedFiles
                    for ((file, status) in changedFiles.entries) {
                        val classPath = file.toString().substring(pathBitLen)
                        if (status == Status.NOTCHANGED) {
                            continue
                        }
                        println("Status of file $file is $status")
                        val outputFile = File(outDir, classPath)
                        if (status == Status.REMOVED) {
                            outputFile.delete()
                            continue
                        }
                        executor.execute {
                            processClassFile(classPath, file, outputFile)
                        }
                    }
                }
            }
        } else {
            println("QTInject doing non-incremental build ...")
            outputProvider.deleteAll()

            outDir.mkdirs()

            ti.inputs.forEach {
                it.jarInputs.forEach {
                    val jarFile = it.file
                    val uniqueName = "${jarFile.name}_${jarFile.absolutePath.hashCode()}"
                    val jarOutDir = outputProvider.getContentLocation(uniqueName, outputTypes, it.scopes, Format.JAR)

                    executor.execute {
                        processJarFile(jarFile, jarOutDir)
                    }
                }

                it.directoryInputs.forEach {
                    val pathBitLen = it.file.toString().length + 1

                    it.file.walk().forEach {
                        if (!it.isDirectory) {
                            val f = it
                            val classPath = f.toString().substring(pathBitLen)
                            val outputFile = File(outDir, classPath)
                            executor.execute {
                                processClassFile(classPath, f, outputFile)
                            }
                        }
                    }
                }
            }
        }
        executor.shutdown()
        executor.awaitTermination(Int.MAX_VALUE.toLong(), TimeUnit.SECONDS)
        println("QTInject done, cost ${System.currentTimeMillis() - startTime}ms")
    }

    private fun processJarFile(file: File, outFile: File) {
        val crc32 = CRC32()
        outFile.delete()
        outFile.parentFile.mkdirs()
        ZipOutputStream(FileOutputStream(outFile)).use { zos ->
            val zipFile = ZipFile(file)
            zipFile.entries().toList().forEach {
                val name = it.name
                var bytes = zipFile.getInputStream(it).readBytes()
                if (name.endsWith(".class")) {
                    bytes = doTransfer(name, bytes)
                }
                crc32.reset()
                crc32.update(bytes)
                val zipEntry = ZipEntry(name).apply {
                    method = ZipEntry.STORED
                    size = bytes.size.toLong()
                    compressedSize = bytes.size.toLong()
                    crc = crc32.value
                }
                zos.putNextEntry(zipEntry)
                zos.write(bytes)
                zos.closeEntry()
            }
            zos.flush()
        }
    }

    private fun processClassFile(classPath: String, file: File, outputFile: File) {
        if (file.isDirectory) {
            return
        }
        outputFile.parentFile.mkdirs()
        val classFileBuffer = file.readBytes()
        outputFile.writeBytes(doTransfer(classPath, classFileBuffer))
    }

    private fun doTransfer(classPath: String, input: ByteArray): ByteArray {
        val className = classPath.substring(0, classPath.lastIndexOf('.'))
        val s = String(input)
        if (s.contains("android/widget/Toast")&& !s.contains("ToastUtils")) {
            println("input: $s ")
            val reader = ClassReader(input)
            val writer = ClassWriter(reader, ClassWriter.COMPUTE_MAXS)
            val cv = TestVisitor(writer)
            reader.accept(cv, 8)
            val result = writer.toByteArray()
            println("output: " + String(result))
            println("inject $classPath end")
            return result
        }
        return input
    }

//    private fun doTransfer(classPath: String, input: ByteArray): ByteArray {
//        val className = classPath.substring(0, classPath.lastIndexOf('.'))
//        val s = String(input)
//        if (s.contains("android.widget.Toast")) {
//            println("inject $classPath start")
//
//            val cc = pool.makeClass(classPath)
//            cc.declaredBehaviors.forEach {
//                it.addCatch()
//            }
//        }
//        println("inject $classPath end")
//        return input
//    }
}