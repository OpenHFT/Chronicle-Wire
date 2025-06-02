/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
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

package run.chronicle.wire.demo.mapreuse;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;

public final class Security extends SelfDescribingMarshallable {

    private int id;
    private long averagePrice;
    private long count;

    public Security(int id, long price, long count) {
        this.id = id;
        this.averagePrice = price;
        this.count = count;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getAveragePrice() {
        return averagePrice;
    }

    public void setAveragePrice(long averagePrice) {
        this.averagePrice = averagePrice;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "Security{" +
                "id=" + id +
                ", averagePrice=" + averagePrice +
                ", count=" + count +
                '}';
    }
}
