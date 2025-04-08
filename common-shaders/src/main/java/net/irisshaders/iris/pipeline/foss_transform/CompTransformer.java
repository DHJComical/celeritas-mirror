package net.irisshaders.iris.pipeline.foss_transform;

import net.irisshaders.iris.gl.shader.ShaderType;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import net.irisshaders.iris.pipeline.transform.parameter.Parameters;
import org.taumc.glsl.Transformer;
import org.taumc.glsl.grammar.GLSLLexer;
import org.taumc.glsl.grammar.GLSLParser;

import java.util.Map;

public class CompTransformer {

    private static final ShaderType[] pipeline = {ShaderType.VERTEX, ShaderType.TESSELATION_CONTROL, ShaderType.TESSELATION_EVAL, ShaderType.GEOMETRY, ShaderType.FRAGMENT};

    public static void transformEach(Transformer root, Parameters parameters) {
        if (parameters.type == PatchShaderType.VERTEX) {
            if ( root.containsCall("fract(worldpos.y + 0.001)")) {
                root.replaceExpression("fract(worldpos.y + 0.001)", "fract(worldpos.y + 0.01)");
            }
        }

		/*
		  Removes const storage qualifier from declarations in functions if they are
		  initialized with const parameters. Const parameters are immutable parameters
		  and can't be used to initialize const declarations because they expect
		  constant, not just immutable, expressions. This varies between drivers and
		  versions. Also removes the const qualifier from declarations that use the
		  identifiers from which the declaration was removed previously.
		  See https://wiki.shaderlabs.org/wiki/Compiler_Behavior_Notes
		 */
        root.removeUnusedFunctions();
        root.removeConstAssignment();

        // rename reserved words within files
        //root.rename("texture", "iris_renamed_texture");
        //root.rename("sampler", "iris_renamed_sampler");

        // transform that moves unsized array specifiers on struct members from the type
        // to the identifier of a type and init declaration. Some drivers appear to not
        // be able to detect the unsized array if it's on the type.
        root.rewriteStructArrays();
    }

    // does transformations that require cross-shader type data
    public static void transformGrouped(Map<PatchShaderType, Transformer> trees, Parameters parameters) {
		/*
		  find attributes that are declared as "in" in geometry or fragment but not
		  declared as "out" in the previous stage. The missing "out" declarations for
		  these attributes are added and initialized.

		  It doesn't bother with array specifiers because they are only legal in
		  geometry shaders, but then also only as an in declaration. The out
		  declaration in the vertex shader is still just a single value. Missing out
		  declarations in the geometry shader are also just normal.

		  TODO:
		  - fix issues where Iris' own declarations are detected and patched like
		  iris_FogFragCoord if there are geometry shaders present
		  - improved geometry shader support? They use funky declarations
		 */

        ShaderType prevType = null;
        for (ShaderType type : pipeline) {
            PatchShaderType[] patchTypes = PatchShaderType.fromGlShaderType(type);

            // check if the patch types have sources and continue if not
            boolean hasAny = false;
            for (PatchShaderType currentType : patchTypes) {
                if (trees.get(currentType) != null) {
                    hasAny = true;
                }
            }
            if (!hasAny) {
                continue;
            }

            // if the current type has sources but the previous one doesn't, set the
            // previous one and continue
            if (prevType == null) {
                prevType = type;
                continue;
            }

            PatchShaderType prevPatchTypes = PatchShaderType.fromGlShaderType(prevType)[0];
            var prevTree = trees.get(prevPatchTypes);

            Map<String, GLSLParser.Single_declarationContext> outDec = prevTree.findQualifiers(GLSLLexer.OUT);
            for (PatchShaderType currentType : patchTypes) {
                var currentTree = trees.get(currentType);

                if (currentTree == null) {
                    continue;
                }

                Map<String, GLSLParser.Single_declarationContext> inDec = currentTree.findQualifiers(GLSLLexer.IN);

                for (String in : inDec.keySet()) {

                    if (in.startsWith("gl_")) {
                        continue;
                    }

                    if (!outDec.containsKey(in)) {

                        if (!currentTree.containsCall(in)) {
                            continue;
                        }

                        prevTree.makeOutDeclaration(inDec.get(in), in);

                        if (!prevTree.hasAssigment(in)) {
                            prevTree.initialize(inDec.get(in), in);
                        }

                    } else {
                        var outType = outDec.get(in).fully_specified_type().type_specifier().type_specifier_nonarray().children.get(0);
                        var inType = inDec.get(in).fully_specified_type().type_specifier().type_specifier_nonarray().children.get(0);

                        if (outDec.get(in).fully_specified_type().type_specifier().array_specifier() != null) {
                            continue;
                        }

                        if (inType.getText().equals(outType.getText())) {
                            if (!prevTree.hasAssigment(in)) {
                                prevTree.initialize(inDec.get(in), in);
                            }
                        }

                    }
                }
            }

            prevType = type;
        }
    }

    private static void makeOutDeclarationtemp(Transformer root, GLSLParser.Single_declarationContext inDeclarationContext, String name) {
        String insert = ShaderTransformer.getFormattedShader(inDeclarationContext.fully_specified_type(), "") + name + ";";
        insert = insert.replaceFirst("in", "out");
        root.injectVariable(insert);
    }

    //  public static void transformFragmentCore(ASTParser t, TranslationUnit tree, Root root, Parameters parameters) {
    // do layout attachment (attaches a location(layout = 4) to the out declaration
    // outColor4 for example)

    // iterate the declarations
//        ArrayList<NewDeclarationData> newDeclarationData = new ArrayList<>();
//        ArrayList<ExternalDeclaration> declarationsToRemove = new ArrayList<>();
//        for (DeclarationExternalDeclaration declaration : root.nodeIndex.get(DeclarationExternalDeclaration.class)) {
//            if (!nonLayoutOutDeclarationMatcher.matchesExtract(declaration)) {
//                continue;
//            }
//
//            // find the matching outColor members
//            List<DeclarationMember> members = nonLayoutOutDeclarationMatcher
//                    .getNodeMatch("name*", DeclarationMember.class)
//                    .getAncestor(TypeAndInitDeclaration.class)
//                    .getMembers();
//            TypeQualifier typeQualifier = nonLayoutOutDeclarationMatcher.getNodeMatch("qualifier", TypeQualifier.class);
//            BuiltinNumericTypeSpecifier typeSpecifier = nonLayoutOutDeclarationMatcher.getNodeMatch("type",
//                    BuiltinNumericTypeSpecifier.class);
//            int addedDeclarations = 0;
//            for (DeclarationMember member : members) {
//                String name = member.getName().getName();
//                if (!name.startsWith(attachTargetPrefix)) {
//                    continue;
//                }
//
//                // get the number suffix after the prefix
//                String numberSuffix = name.substring(attachTargetPrefix.length());
//                if (numberSuffix.isEmpty()) {
//                    continue;
//                }
//
//                // make sure it's a number and is between 0 and 7
//                int number;
//                try {
//                    number = Integer.parseInt(numberSuffix);
//                } catch (NumberFormatException e) {
//                    continue;
//                }
//                if (number < 0 || 7 < number) {
//                    continue;
//                }
//
//                newDeclarationData.add(new NewDeclarationData(typeQualifier, typeSpecifier, member, number));
//                addedDeclarations++;
//            }
//
//            // if the member list is now empty, remove the declaration
//            if (addedDeclarations == members.size()) {
//                declarationsToRemove.add(declaration);
//            }
//        }
//        tree.getChildren().removeAll(declarationsToRemove);
//        for (ExternalDeclaration declaration : declarationsToRemove) {
//            declaration.detachParent();
//        }
//
//        // generate new declarations with layout qualifiers for each outColor member
//        ArrayList<ExternalDeclaration> newDeclarations = new ArrayList<>();
//
//        // Note: since everything is wrapped in a big Root.indexBuildSession, we don't
//        // need to do it manually here
//        for (NewDeclarationData data : newDeclarationData) {
//            DeclarationMember member = data.member;
//            member.detach();
//            TypeQualifier newQualifier = data.qualifier.cloneInto(root);
//            newQualifier.getChildren()
//                    .add(new LayoutQualifier(Stream.of(new NamedLayoutQualifierPart(
//                            new Identifier("location"),
//                            new LiteralExpression(Type.INT32, data.number)))));
//            ExternalDeclaration newDeclaration = layoutedOutDeclarationTemplate.getInstanceFor(root,
//                    newQualifier,
//                    data.type.cloneInto(root),
//                    member);
//            newDeclarations.add(newDeclaration);
//        }
//        tree.injectNodes(ASTInjectionPoint.BEFORE_DECLARATIONS, newDeclarations);
    //  }
}
