/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

class MyTypes extends AbstractMarshallable {
    final StringBuilder text = new StringBuilder();
    boolean b;
    short s;
    double d;
    long l;
    int i;

    public void b(boolean b) {
        this.b = b;
    }

    public boolean b() {
        return this.b;
    }

    public void s(short s) {
        this.s = s;
    }

    public short s() {
        return this.s;
    }

    public void d(double d) {
        this.d = d;
    }

    public double d() {
        return this.d;
    }

    public void l(long l) {
        this.l = l;
    }

    public long l() {
        return this.l;
    }

    public void i(int i) {
        this.i = i;
    }

    public int i() {
        return this.i;
    }

    @NotNull
    public CharSequence text() {
        return text;
    }

    public void text(CharSequence value) {
        text.setLength(0);
        text.append(value);
    }

    enum Fields implements WireKey {
        B_FLAG, S_NUM, D_NUM, L_NUM, I_NUM, TEXT;

        @Override
        public int code() {
            return ordinal();
        }
    }
}
