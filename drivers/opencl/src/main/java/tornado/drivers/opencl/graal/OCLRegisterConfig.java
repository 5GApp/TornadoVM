package tornado.drivers.opencl.graal;

import jdk.vm.ci.code.CallingConvention.Type;
import jdk.vm.ci.code.*;
import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.PlatformKind;

import static tornado.common.exceptions.TornadoInternalError.unimplemented;

public class OCLRegisterConfig implements RegisterConfig {

    @Override
    public RegisterArray getCalleeSaveRegisters() {
        unimplemented();
        return null;
    }

    @Override
    public CallingConvention getCallingConvention(Type type, JavaType jt, JavaType[] jts, ValueKindFactory<?> vkf) {
        unimplemented();
        return null;
    }

    @Override
    public Register getReturnRegister(JavaKind kind) {
        unimplemented();
        return null;
    }

    @Override
    public Register getFrameRegister() {
        unimplemented();
        return null;
    }

    @Override
    public RegisterArray getCallingConventionRegisters(Type type, JavaKind kind) {
        unimplemented();
        return null;
    }

    @Override
    public RegisterArray getAllocatableRegisters() {
        unimplemented();
        return null;
    }

    @Override
    public RegisterArray filterAllocatableRegisters(PlatformKind kind, RegisterArray registers) {
        unimplemented();
        return null;
    }

    @Override
    public RegisterArray getCallerSaveRegisters() {
        unimplemented();
        return null;
    }

    @Override
    public RegisterAttributes[] getAttributesMap() {
        unimplemented();
        return null;
    }

    @Override
    public boolean areAllAllocatableRegistersCallerSaved() {
        unimplemented();
        return false;
    }

}
