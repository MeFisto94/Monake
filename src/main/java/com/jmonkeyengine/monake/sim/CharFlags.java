package com.jmonkeyengine.monake.sim;

public class CharFlags {
    byte value = 0;

    public CharFlags() {
    }

    public CharFlags(byte value) {
        this.value = value;
    }

    public void setFlag(CharFlag flag, boolean set) {
        if (set) {
            value |= (1 << flag.ordinal());
        } else {
            value &= ~(1 << flag.ordinal());
        }
    }

    public boolean getFlag(CharFlag flag) {
        return (value & (1 << flag.ordinal())) != 0;
    }

    @Override
    public String toString() {
        String str = "CharFlags{";
        for (CharFlag flag: CharFlag.values()) {
            str += flag.toString() + "=" + getFlag(flag);
        }
        return str + '}';
    }
}
