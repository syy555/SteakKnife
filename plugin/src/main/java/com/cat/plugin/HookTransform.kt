package com.cat.plugin

import com.android.build.api.transform.*
import com.google.gson.Gson
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
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


class HookTransform : Transform() {

    override fun getName(): String = "PreHook"

    var isAnnotationParamsModified = false

    override fun getInputTypes(): Set<QualifiedContent.ContentType> = setOf(QualifiedContent.DefaultContentType.CLASSES)

    override fun getReferencedScopes(): MutableSet<in QualifiedContent.Scope>? = hashSetOf(QualifiedContent.Scope.PROJECT,
            QualifiedContent.Scope.SUB_PROJECTS,
            QualifiedContent.Scope.EXTERNAL_LIBRARIES
    )


    override fun getScopes(): MutableSet<in QualifiedContent.Scope>? = mutableSetOf(QualifiedContent.Scope.PROJECT,
            QualifiedContent.Scope.SUB_PROJECTS,
            QualifiedContent.Scope.EXTERNAL_LIBRARIES
    )

    override fun isIncremental(): Boolean = false

    override fun transform(ti: TransformInvocation) {
        println("start pre hook")
        val startTime = System.currentTimeMillis()
        hookItems.clear()
        val executor = ThreadPoolExecutor(8, 10, 1, TimeUnit.SECONDS,
                LinkedBlockingQueue<Runnable>())
        ti.referencedInputs.forEach {
            it.jarInputs.forEach { jarInput ->
                val jarFile = jarInput.file
                var status = jarInput.status
                if (!ti.isIncremental) {
                    status = null
                }
                executor.execute {
                    processPreHookJarFile(jarFile, status)
                }
            }
            it.directoryInputs.forEach { directoryInput ->
                val pathBitLen = directoryInput.file.toString().length + 1
                if (ti.isIncremental) {
                    val changedFiles = directoryInput.changedFiles
                    for ((file, status) in changedFiles.entries) {
                        if (!file.isDirectory) {
                            val classPath = file.toString().substring(pathBitLen)
                            executor.execute {
                                doPreHookTransfer(classPath, file.readBytes(), status)
                            }
                        }
                    }
                } else {
                    directoryInput.file.walk().forEach { file ->
                        if (!file.isDirectory) {
                            val classPath = file.toString().substring(pathBitLen)
                            executor.execute {
                                doPreHookTransfer(classPath, file.readBytes(), null)
                            }
                        }
                    }
                }


            }
        }
        executor.shutdown()
        executor.awaitTermination(Int.MAX_VALUE.toLong(), TimeUnit.SECONDS)
        println("hookItems: ${Gson().toJson(hookItems)}")

        val outputProvider = ti.outputProvider
        val outDir = outputProvider.getContentLocation("hooks", outputTypes, hashSetOf(QualifiedContent.Scope.PROJECT), Format.DIRECTORY)
        val executor1 = ThreadPoolExecutor(8, 10, 1, TimeUnit.SECONDS,
                LinkedBlockingQueue<Runnable>())
        if (ti.isIncremental && !isAnnotationParamsModified) {
            println("doing increase Hook job ")
            ti.inputs.forEach {
                for (jarInput in it.jarInputs) {
                    val jarFile = jarInput.file
                    val status = jarInput.status
                    if (status == Status.NOTCHANGED) {
                        continue
                    }
                    val uniqueName = jarFile.name + "_" + jarFile.absolutePath.hashCode()
                    val jarOutFile = outputProvider.getContentLocation(uniqueName, outputTypes, jarInput.scopes, Format.JAR)
                    if (status == Status.REMOVED) {
                        jarOutFile.delete()
                        continue
                    }
                    executor1.execute {
                        processJarFile(jarFile, jarOutFile)
                    }
                }

                it.directoryInputs.forEach { directoryInput ->
                    val pathBitLen = directoryInput.file.toString().length + 1

                    val changedFiles = directoryInput.changedFiles
                    for ((file, status) in changedFiles.entries) {
                        val classPath = file.toString().substring(pathBitLen)
                        if (status == Status.NOTCHANGED) {
                            continue
                        }
                        val outputFile = File(outDir, classPath)
                        if (status == Status.REMOVED) {
                            outputFile.delete()
                            continue
                        }
                        executor1.execute {
                            processClassFile(classPath, file, outputFile)
                        }
                    }
                }
            }
        } else {
            println("doing normal Hook job ")
            outputProvider.deleteAll()

            outDir.mkdirs()

            ti.inputs.forEach {
                it.jarInputs.forEach { jarInput ->
                    val jarFile = jarInput.file
                    val uniqueName = "${jarFile.name}_${jarFile.absolutePath.hashCode()}"
                    val jarOutDir = outputProvider.getContentLocation(uniqueName, outputTypes, jarInput.scopes, Format.JAR)
                    executor1.execute {
                        processJarFile(jarFile, jarOutDir)
                    }
                }

                it.directoryInputs.forEach { directoryInput ->
                    val pathBitLen = directoryInput.file.toString().length + 1
                    directoryInput.file.walk().forEach { file ->
                        if (!file.isDirectory) {
                            val classPath = file.toString().substring(pathBitLen)
                            val outputFile = File(outDir, classPath)
                            executor1.execute {
                                processClassFile(classPath, file, outputFile)
                            }
                        }
                    }
                }
            }
        }
        executor1.shutdown()
        executor1.awaitTermination(Int.MAX_VALUE.toLong(), TimeUnit.SECONDS)
        println("hook done, cost ${System.currentTimeMillis() - startTime}ms")
    }

    private fun processPreHookJarFile(file: File, status: Status?) {
        val zipFile = ZipFile(file)
        zipFile.entries().toList().forEach {
            val name = it.name
            var bytes = zipFile.getInputStream(it).readBytes()
            if (name.endsWith(".class")) {
                doPreHookTransfer(name, bytes, status)
            }
        }
    }

    private fun doPreHookTransfer(classPath: String, input: ByteArray, status: Status?) {
        val s = String(input)
        if (s.contains("Lfm/qingting/router/annotations/HookParams;")) {
            if(status != Status.NOTCHANGED){
                isAnnotationParamsModified = true
            }
            val reader = ClassReader(input)
            val cv = PreHookVisitor()
            reader.accept(cv, ClassReader.SKIP_FRAMES)
        }
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
            println("file is directory")
            return
        }
        outputFile.parentFile.mkdirs()
        val classFileBuffer = file.readBytes()
        outputFile.writeBytes(doTransfer(classPath, classFileBuffer))
    }

    private fun doTransfer(classPath: String, input: ByteArray): ByteArray {
        val className = classPath.substring(0, classPath.lastIndexOf('.'))
        val s = String(input)
        val filtered = hookItems.filter { it.checkInjection(className, s) }
        if (!filtered.isEmpty()) {
            val reader = ClassReader(input)
            val writer = ClassWriter(reader, ClassWriter.COMPUTE_MAXS)
            var cv: ClassVisitor = writer
            filtered.forEach {
                cv = HookVisitor(cv, it)
            }
            reader.accept(cv, aj.org.objectweb.asm.ClassReader.EXPAND_FRAMES)
            return writer.toByteArray()
        }
        return input
    }


    companion object {
        var hookItems = mutableListOf<HookItem>()
    }
}