package org.embeddedt.embeddium.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.*
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import java.io.File
import java.util.jar.JarFile

const val LWJGL_ABSTRACTION_PACKAGE = "org.taumc.celeritas.lwjgl"

enum class LWJGLVersion(val version: Int) {
    TWO(2),
    THREE(3);
}

abstract class GenerateLWJGLAbstraction : DefaultTask() {

    @get:InputFiles
    @get:Classpath
    abstract val lwjgl2Classpath: ConfigurableFileCollection

    @get:InputFiles
    @get:Classpath
    abstract val lwjgl3Classpath: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    data class GLMethod(
        val returnType: String,
        val name: String,
        val params: List<Pair<String, String>>
    ) {
        fun signature(): String = "$name(${params.joinToString(",") { it.first }})$returnType"
    }

    data class GLConstant(
            val name: String,
            val type: String,
            val value: Any
    )

    class GLClassData {
        val methods = mutableSetOf<GLMethod>();
        val constants = mutableSetOf<GLConstant>();
    }

    @TaskAction
    fun generate() {
        val outputDir = outputDirectory.get().asFile
        if (outputDir.exists()) {
            outputDir.deleteRecursively()
        }
        outputDir.mkdirs()

        // GL version classes to process
        val glVersions = mapOf(
            "11" to "org/lwjgl/opengl/GL11",
            "12" to "org/lwjgl/opengl/GL12",
            "13" to "org/lwjgl/opengl/GL13",
            "14" to "org/lwjgl/opengl/GL14",
            "15" to "org/lwjgl/opengl/GL15",
            "20" to "org/lwjgl/opengl/GL20",
            "21" to "org/lwjgl/opengl/GL21",
            "30" to "org/lwjgl/opengl/GL30",
            "31" to "org/lwjgl/opengl/GL31",
            "32" to "org/lwjgl/opengl/GL32",
            "33" to "org/lwjgl/opengl/GL33",
            "40" to "org/lwjgl/opengl/GL40",
            "41" to "org/lwjgl/opengl/GL41",
            "42" to "org/lwjgl/opengl/GL42",
            "43" to "org/lwjgl/opengl/GL43",
            "44" to "org/lwjgl/opengl/GL44",
            "45" to "org/lwjgl/opengl/GL45",
            "46" to "org/lwjgl/opengl/GL46"
        )

        val allMethods = mutableMapOf<String, GLClassData>()

        glVersions.forEach { (version, classPath) ->
            val methods = extractGLMethods(classPath, lwjgl3Classpath.files)
            allMethods[version] = methods
            logger.info("Extracted ${methods.methods.size} methods from GL$version")
        }

        filterValidGLMethods(allMethods, glVersions, lwjgl2Classpath.files)

        // Generate LWJGLService interface
        //generateLWJGLServiceInterface(outputDir, allMethods)

        // Generate LWJGLService implementations
        //for (version in LWJGLVersion.entries) {
        //    generateLWJGLBackingService(outputDir, version, allMethods, glVersions)
        //}

        // Generate GLXY wrapper classes
        glVersions.keys.forEach { version ->
            val methods = allMethods[version] ?: GLClassData()
            generateGLWrapper(outputDir, version, methods)
        }

        logger.lifecycle("Generated LWJGL abstraction layer in ${outputDir.absolutePath}")
    }

    private fun isGLMethod(method: MethodNode): Boolean {
        val isStatic = (method.access and Opcodes.ACC_STATIC) != 0
        val isPublic = (method.access and Opcodes.ACC_PUBLIC) != 0

        return (isStatic && isPublic && method.name.startsWith("gl"))
    }

    private fun extractGLMethods(classPath: String, classpathFiles: Set<File>): GLClassData {
        val data = GLClassData()

        for (file in classpathFiles) {
            if (!file.exists() || !file.name.endsWith(".jar")) continue

            try {
                JarFile(file).use { jar ->
                    val entry = jar.getJarEntry("$classPath.class") ?: continue

                    jar.getInputStream(entry).use { stream ->
                        val classReader = ClassReader(stream)
                        val classNode = ClassNode()
                        classReader.accept(classNode, 0)

                        classNode.fields.forEach { field ->
                            val isStatic = (field.access and Opcodes.ACC_STATIC) != 0
                            val isFinal = (field.access and Opcodes.ACC_FINAL) != 0
                            val isPublic = (field.access and Opcodes.ACC_PUBLIC) != 0

                            if (isStatic && isFinal && isPublic && field.value != null) {
                                val type = Type.getType(field.desc)
                                val javaType = asmTypeToJavaType(type)

                                data.constants.add(GLConstant(
                                        name = field.name,
                                        type = javaType,
                                        value = field.value
                                ))
                            }
                        }

                        classNode.methods.filter { isGLMethod(it) }.forEach { method ->
                            val methodType = Type.getMethodType(method.desc)
                            val paramTypes = methodType.argumentTypes.map { asmTypeToJavaType(it) }

                            if (paramTypes.any { it.startsWith("org.lwjgl.") }) {
                                // Do not emit overloads that require LWJGL class references
                                return@forEach
                            }

                            val returnType = asmTypeToJavaType(methodType.returnType)

                            // Try to get parameter names from bytecode
                            val paramNames = mutableListOf<String>()

                            // First try MethodParameters attribute (Java 8+ with -parameters flag)
                            if (method.parameters != null && method.parameters.isNotEmpty()) {
                                method.parameters.forEach { param ->
                                    paramNames.add(param.name)
                                }
                            }
                            // Fall back to LocalVariableTable if available
                            else if (method.localVariables != null && method.localVariables.isNotEmpty()) {
                                // For static methods, local variables start at index 0
                                // Sort by index and skip synthetic/internal variables
                                method.localVariables
                                    .filter { it.index < paramTypes.size }
                                    .sortedBy { it.index }
                                    .forEach { localVar ->
                                        if (paramNames.size < paramTypes.size) {
                                            paramNames.add(localVar.name)
                                        }
                                    }
                            }

                            // Ensure we have enough parameter names
                            while (paramNames.size < paramTypes.size) {
                                paramNames.add("param${paramNames.size}")
                            }

                            val params = paramTypes.mapIndexed { idx, type ->
                                type to paramNames[idx]
                            }

                            data.methods.add(GLMethod(
                                returnType = returnType,
                                name = method.name,
                                params = params
                            ))
                        }
                    }

                    if (data.methods.isNotEmpty()) break
                }
            } catch (e: Exception) {
                logger.warn("Could not read class $classPath from ${file.name}: ${e.message}")
            }
        }

        return data
    }

    private fun filterValidGLMethods(allMethods: MutableMap<String, GLClassData>, glVersions: Map<String, String>, classpathFiles: Set<File>) {
        val lwjgl2Signatures = mutableSetOf<String>()

        for (file in classpathFiles) {
            if (!file.exists() || !file.name.endsWith(".jar")) continue

            JarFile(file).use { jar ->
                allMethods.forEach { (version, _) ->
                    val classBinaryName = glVersions[version]
                    val entry = jar.getJarEntry("$classBinaryName.class") ?: return@forEach

                    jar.getInputStream(entry).use { stream ->
                        val classReader = ClassReader(stream)
                        val classNode = ClassNode()
                        classReader.accept(classNode, 0)

                        classNode.methods.filter { isGLMethod(it) }.forEach { method ->
                            val methodType = Type.getMethodType(method.desc)
                            val paramTypes = methodType.argumentTypes.map { asmTypeToJavaType(it) }
                            val paramNames = mutableListOf<String>()
                            while (paramNames.size < paramTypes.size) {
                                paramNames.add("param${paramNames.size}")
                            }
                            val returnType = asmTypeToJavaType(methodType.returnType)

                            val params = paramTypes.mapIndexed { idx, type ->
                                type to paramNames[idx]
                            }

                            lwjgl2Signatures.add(GLMethod(name = method.name, returnType = returnType, params = params).signature())
                        }
                    }
                }
            }
        }



        allMethods.forEach { (version, methodSet) ->
            val oldSize = methodSet.methods.size
            methodSet.methods.removeIf { !lwjgl2Signatures.contains(it.signature()) }
            val newSize = methodSet.methods.size
            if (newSize < oldSize) {
                logger.info("Removed {} methods from GL{} that don't exist in LWJGL2", oldSize - newSize, version)
            }
        }
    }

    private fun asmTypeToJavaType(type: Type): String {
        return when (type.sort) {
            Type.VOID -> "void"
            Type.BOOLEAN -> "boolean"
            Type.CHAR -> "char"
            Type.BYTE -> "byte"
            Type.SHORT -> "short"
            Type.INT -> "int"
            Type.FLOAT -> "float"
            Type.LONG -> "long"
            Type.DOUBLE -> "double"
            Type.ARRAY -> asmTypeToJavaType(type.elementType) + "[]".repeat(type.dimensions)
            Type.OBJECT -> type.className
            else -> type.className
        }
    }

    private fun getOutputDir(outputDir: File, group: String): File {
        return File(outputDir, group + File.separatorChar + LWJGL_ABSTRACTION_PACKAGE.replace('.', File.separatorChar))
    }

    private fun generateLWJGLServiceInterface(outputDir: File, allMethods: Map<String, GLClassData>) {
        val packageDir = getOutputDir(outputDir, "main")
        packageDir.mkdirs()

        val serviceFile = File(packageDir, "LWJGLService.java")
        val allUniqueMethods = allMethods.values.map { it.methods }.flatten().distinctBy { it.signature() }

        serviceFile.writeText("""
package ${LWJGL_ABSTRACTION_PACKAGE};

public interface LWJGLService {
    
    LWJGLService INSTANCE = createInstance();
    
    static LWJGLService constructInstance(String className) {
        try {
            var clz = Class.forName(className);
            return (LWJGLService)clz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
    
    static LWJGLService createInstance() {
        try {
            Class.forName("org.lwjgl.opengl.GL11");
            return constructInstance("${LWJGL_ABSTRACTION_PACKAGE}.LWJGL2Service");
        } catch (ClassNotFoundException e) {
            return constructInstance("${LWJGL_ABSTRACTION_PACKAGE}.LWJGL3Service");
        }
    }
    
${allUniqueMethods.joinToString("\n\n") { method ->
            val params = method.params.joinToString(", ") { "${it.first} ${it.second}" }
            "    ${method.returnType} ${method.name}($params);"
        }}
}
""".trimIndent())
    }

    private fun generateLWJGLBackingService(outputDir: File, version: LWJGLVersion, allMethods: Map<String, GLClassData>, glVersions: Map<String, String>) {
        val packageDir = getOutputDir(outputDir, "lwjgl" + version.version)
        packageDir.mkdirs()

        val serviceFile = File(packageDir, "LWJGL${version.version}Service.java")

        // Create a map of methods to their GL class
        val methodToClass = mutableMapOf<String, String>()
        glVersions.forEach { (version, _) ->
            allMethods[version]?.methods?.forEach { method ->
                if (!methodToClass.containsKey(method.signature())) {
                    methodToClass[method.signature()] = version
                }
            }
        }

        val allUniqueMethods = allMethods.values.map { it.methods }.flatten().distinctBy { it.signature() }

        serviceFile.writeText("""
package ${LWJGL_ABSTRACTION_PACKAGE};

class LWJGL${version.version}Service implements LWJGLService {
    
${allUniqueMethods.joinToString("\n\n") { method ->
            val params = method.params.joinToString(", ") { "${it.first} ${it.second}" }
            val paramNames = method.params.joinToString(", ") { it.second }
            val returnStmt = if (method.returnType == "void") "" else "return "
            val glVersion = methodToClass[method.signature()] ?: "11"
            """    @Override
    public ${method.returnType} ${method.name}($params) {
        ${returnStmt}org.lwjgl.opengl.GL${glVersion}.${method.name}($paramNames);
    }"""
        }}
}
""".trimIndent())
    }

    private val GL_VERSION_CHAIN = listOf(
            "11", "12", "13", "14", "15", "20", "21", "30", "31", "32", "33", "40", "41", "42", "43", "44", "45"
    )

    private fun generateGLWrapper(outputDir: File, version: String, classData: GLClassData) {
        val packageDir = getOutputDir(outputDir, "main")
        packageDir.mkdirs()

        val className = "GL$version"
        val wrapperFile = File(packageDir, "$className.java")

        // Determine previous version in the chain, or null if none
        val previousVersion = GL_VERSION_CHAIN
                .zipWithNext()
                .firstOrNull { it.second == version }
                ?.first

        val extendsClause = previousVersion?.let { " extends GL$it" } ?: ""

        wrapperFile.writeText("""
package ${LWJGL_ABSTRACTION_PACKAGE};

public class $className$extendsClause {
    
${classData.constants.joinToString("\n\n") { constant -> 
    "    public static final ${constant.type} ${constant.name} = ${constant.value};"
        }}
}
""".trimIndent())
    }
}