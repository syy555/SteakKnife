package com.cat.plugin;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.ASM5;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

public class TestVisitor extends ClassVisitor {
    private String className;

    private boolean doInject = false;

    public TestVisitor(ClassVisitor cv) {
        super(ASM5, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        className = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            mv = new MethodVisitor(ASM5, mv) {
                String methodName = name;
                String methodDesc = descriptor;

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                    if (owner.equalsIgnoreCase("android/widget/Toast") && name.equalsIgnoreCase("makeText") && desc.equalsIgnoreCase("(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;")) {
                        logHook(methodName, methodDesc);
                        mv.visitMethodInsn(INVOKESTATIC, "com/cat/myapplication/ToastUtils", "makeText", "(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;", false);
                        return;
                    }
                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                }
            };
            return mv;
    }


    private void logHook(String method, String desc) {
        System.out.println("Hooking in " + className + ":" + method + desc);
    }
}
