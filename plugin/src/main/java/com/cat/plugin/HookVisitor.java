package com.cat.plugin;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.ASM5;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

public class HookVisitor extends ClassVisitor {
    private String className;

    private boolean doInject = false;

    private HookItem hookItem;

    public HookVisitor(ClassVisitor cv, HookItem item) {
        super(ASM5, cv);
        this.hookItem = item;
    }

    public static boolean check(HookItem item, String className, String source) {
        boolean needInject = false;
        if (item.checkInjection(className, source)) {
            needInject = true;
        }
        return needInject;
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
                String[] target = hookItem.transformMethod(opcode, owner, name, desc);
                if (target != null) {
                    logHook(methodName, methodDesc);
                    mv.visitMethodInsn(INVOKESTATIC, target[0], target[1], target[2], false);
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
