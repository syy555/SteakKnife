package com.cat.plugin

import aj.org.objectweb.asm.ClassReader.EXPAND_FRAMES
import com.android.build.api.transform.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream


class HookTransform : Transform() {

    private val projectScopes = hashSetOf(QualifiedContent.Scope.PROJECT)

    var hookItems = mutableListOf<HookItem>()

    lateinit var configFile: File

    override fun getName(): String = "HookJob"

    override fun getInputTypes(): Set<QualifiedContent.ContentType> = setOf(QualifiedContent.DefaultContentType.CLASSES)

    override fun getSecondaryFiles(): Collection<SecondaryFile> {
        val secondaryFile = SecondaryFile(configFile, false)
        return listOf(secondaryFile)
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope>? = mutableSetOf(QualifiedContent.Scope.PROJECT,
            QualifiedContent.Scope.SUB_PROJECTS,
            QualifiedContent.Scope.EXTERNAL_LIBRARIES
    )

    override fun isIncremental(): Boolean = true

    override fun transform(ti: TransformInvocation) {
        val startTime = System.currentTimeMillis()
        hookItems = Gson().fromJson(FileReader(configFile), object : TypeToken<List<HookItem>>() {
        }.type)
        val outputProvider = ti.outputProvider
        val outDir = outputProvider.getContentLocation("hooks", outputTypes, projectScopes, Format.DIRECTORY)

        val executor = ThreadPoolExecutor(8, 10, 1, TimeUnit.SECONDS,
                LinkedBlockingQueue<Runnable>())
        if (ti.isIncremental) {
            println("Hook job increase")
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
                    executor.execute {
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
                        executor.execute {
                            processClassFile(classPath, file, outputFile)
                        }
                    }
                }
            }
        } else {
            println("Hook job normal")
            outputProvider.deleteAll()

            outDir.mkdirs()

            ti.inputs.forEach {
                it.jarInputs.forEach { jarInput ->
                    val jarFile = jarInput.file
                    val uniqueName = "${jarFile.name}_${jarFile.absolutePath.hashCode()}"
                    val jarOutDir = outputProvider.getContentLocation(uniqueName, outputTypes, jarInput.scopes, Format.JAR)
                    executor.execute {
                        processJarFile(jarFile, jarOutDir)
                    }
                }

                it.directoryInputs.forEach { directoryInput ->
                    val pathBitLen = directoryInput.file.toString().length + 1

                    directoryInput.file.walk().forEach { file ->
                        if (!file.isDirectory) {
                            val classPath = file.toString().substring(pathBitLen)
                            val outputFile = File(outDir, classPath)
                            executor.execute {
                                processClassFile(classPath, file, outputFile)
                            }
                        }
                    }
                }
            }
        }
        executor.shutdown()
        executor.awaitTermination(Int.MAX_VALUE.toLong(), TimeUnit.SECONDS)
        println("Hook job done, cost ${System.currentTimeMillis() - startTime}ms")
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
        val filtered =  hookItems.filter { it.checkInjection(className,s) }
            println("Hook job filters: $classPath : $filtered")
        if (!filtered.isEmpty()) {
            val reader = ClassReader(input)
            val writer = ClassWriter(reader, ClassWriter.COMPUTE_MAXS)
            var cv: ClassVisitor = writer
            filtered.forEach {
                cv = HookVisitor(cv,it)
            }
            reader.accept(cv, EXPAND_FRAMES)
            println("Hook job $classPath end")
        }
        return input
    }

}