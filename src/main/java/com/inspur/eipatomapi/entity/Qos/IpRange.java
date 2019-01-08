package com.inspur.eipatomapi.entity.Qos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IpRange implements Serializable {
    private String min;

    private String max;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IpRange ipRange = (IpRange) o;
        return Objects.equals(min, ipRange.min) &&
                Objects.equals(max, ipRange.max);
    }

    @Override
    public int hashCode() {
        return Objects.hash(min, max);
    }
}
