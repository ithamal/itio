package io.github.ithaml.itio.client;

import lombok.Getter;
import lombok.Setter;

/**
 * @author: ken.lin
 * @since: 2023-10-20 10:43
 */
@Getter
@Setter
public class PoolSetting {

    /**
     * 最小值
     */
    private int minimumSize = 0;

    /**
     * 最大值
     */
    private int maximumSize = 10;

    /**
     * 保持时间，秒 ， <= 0 不生效
     */
    private int keepAliveTime = 0;

    public PoolSetting minimumSize(int minimumSize) {
        this.minimumSize = minimumSize;
        return this;
    }

    public PoolSetting maximumSize(int maximumSize) {
        this.maximumSize = maximumSize;
        return this;
    }

    public PoolSetting keepAliveTime(int keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
        return this;
    }
}
