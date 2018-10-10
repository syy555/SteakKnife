package com.cat.plugin
//package fm.qingting.plugin
//
//import com.android.build.api.transform.QualifiedContent
//import com.android.build.api.transform.Transform
//import com.android.build.api.transform.TransformException
//import com.android.build.api.transform.TransformInvocation
//
//class JavasistTransform1 extends Transform {
//
//    private Set<QualifiedContent.Scope> projectScopes =
//            [QualifiedContent.Scope.PROJECT]
//
//    @Override
//    String getName() {
//        return "QingTingInject"
//    }
//
//    @Override
//    Set<QualifiedContent.ContentType> getInputTypes() {
//        return [QualifiedContent.DefaultContentType.CLASSES]
//    }
//
//    @Override
//    Set<QualifiedContent.Scope> getScopes() {
//        return [QualifiedContent.Scope.PROJECT,
//                QualifiedContent.Scope.SUB_PROJECTS,
//                QualifiedContent.Scope.EXTERNAL_LIBRARIES]
//    }
//
//    @Override
//    boolean isIncremental() {
//        return true
//    }
//
//    @Override
//    void transform(TransformInvocation ti) throws TransformException, InterruptedException, IOException {
//        val outDir = outputProvider.getContentLocation("inject", outputTypes, projectScopes, Format.DIRECTORY)
//
//        transformInvocation.inputs.each {
//            it.jar.each {
//                println it.file
//            }
//            it.directoryInputs.each {
//                println it.file
//            }
//        }
//    }
//}