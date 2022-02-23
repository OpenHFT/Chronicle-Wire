/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

class MyTypes extends SelfDescribingMarshallable {
    final StringBuilder text = new StringBuilder();
    boolean flag;
    byte b;
    short s;
    char ch;
    int i;
    float f;
    double d;
    long l;

    public MyTypes flag(boolean b) {
        this.flag = b;
        return this;
    }

    public boolean flag() {
        return this.flag;
    }

    public byte b() {
        return b;
    }

    public MyTypes b(byte b) {
        this.b = b;
        return this;
    }

    public MyTypes s(short s) {
        this.s = s;
        return this;
    }

    public short s() {
        return this.s;
    }

    public char ch() {
        return ch;
    }

    public MyTypes ch(char ch) {
        this.ch = ch;
        return this;
    }

    public float f() {
        return f;
    }

    public MyTypes f(float f) {
        this.f = f;
        return this;
    }

    public MyTypes d(double d) {
        this.d = d;
        return this;
    }

    public double d() {
        return this.d;
    }

    public MyTypes l(long l) {
        this.l = l;
        return this;
    }

    public long l() {
        return this.l;
    }

    public MyTypes i(int i) {
        this.i = i;
        return this;
    }

    public int i() {
        return this.i;
    }

    @NotNull
    public StringBuilder text() {
        return text;
    }

    public MyTypes text(CharSequence value) {
        text.setLength(0);
        text.append(value);
        return this;
    }
}
