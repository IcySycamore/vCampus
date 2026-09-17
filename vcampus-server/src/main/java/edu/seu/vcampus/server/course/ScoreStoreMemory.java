package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.course.Score;

import java.util.Collections;
import java.util.List;

/**
 * 不持久化的成绩后端，保持改造前的纯内存行为。
 *
 * <p>
 * 缺省装配用它，因此没接数据库的机器上（含绝大多数单元测试）行为与改造前逐字一致：读回空、 写入都成功，真正的状态仍只在 {@link ScoreDao} 的内存表里。{@link #save}
 * 原样返回实体已有的 记录号，让调用方按内存规则自行分配。
 */
public final class ScoreStoreMemory implements ScoreStore {

    /** @return 空列表，内存版没有可恢复的历史 */
    @Override
    public List<Score> loadAll() {
        return Collections.emptyList();
    }

    /** @return 实体已有的记录号；为 null 表示交由 {@link ScoreDao} 自行分配 */
    @Override
    public Long save(Score score) {
        return score == null ? null : score.getId();
    }

    /** @return 恒为 true，内存版无需写入 */
    @Override
    public boolean delete(String studentUuid, String courseCode) {
        return true;
    }
}
