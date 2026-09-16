package edu.seu.vcampus.common.course;

import java.io.Serializable;

/**
 * 学科领域（值对象）：统一表示学院的「研究方向」与「专业」。
 *
 * <p>学院同时拥有研究方向（供教师匹配开课）与专业（供学生匹配选课）。两者在数据
 * 结构上都是「领域标签」，因此统一为同一个 {@link Field} 类型：教师引用它时即为
 * 「研究方向」，学生引用它时即为「专业」——词汇表共享、语义由使用方区分。
 */
public final class Field implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 领域名称，如「人工智能」「软件工程」。 */
    private final String name;

    /**
     * 构造一个领域。
     *
     * @param name 领域名称
     */
    public Field(String name) {
        if (name == null || name.trim().length() == 0) {
            throw new IllegalArgumentException("field name must not be empty");
        }
        this.name = name.trim();
    }

    /** @return 领域名称 */
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Field)) {
            return false;
        }
        Field other = (Field) obj;
        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
