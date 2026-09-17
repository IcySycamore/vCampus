package edu.seu.vcampus.server.user;

import edu.seu.vcampus.server.util.ServerLog;

import edu.seu.vcampus.common.random.RandomGen;
import edu.seu.vcampus.common.user.entity.SessionEntry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录会话管理
 *
 * <p>
 * 登录成功后签发 token；之后每条请求携带 token，服务器按 token 解析身份。 token 30 分钟滑动过期，每次校验刷新 TTL，登出删除。线程安全。
 *
 * <p>
 * <b>同一账号只保留一个登录会话</b>：同一账号再次登录时，旧 token 立即作废（后登录顶掉先登录）， 避免一个账号在两个客户端上同时在线。校园业务的「一次性复核
 * token」不走这条路径，它不会影响已登录会话。
 */
public class SessionManager {

    /** 过期时长（毫秒）：30 分钟。 */
    private static final long EXPIRY_MILLIS = 30 * 60 * 1000L;

    /** map(token,{uuid,username,role,expiry}) */
    private final Map<String, SessionEntry> sessions;

    /** map(uuid,token)：账户当前有效的登录会话；同账号重复登录时用它找到要顶掉的旧会话。 */
    private final Map<String, String> activeByAccount;

    /** map(token,connectionId)：会话来自哪条连接；同一连接上的再次登录（密码复核）不能顶掉自己的会话。 */
    private final Map<String, String> connectionByToken;

    /** 随机源。 */
    private final RandomGen random = new RandomGen();

    /** 构造会话管理器。 */
    public SessionManager() {
        sessions = new ConcurrentHashMap<String, SessionEntry>();
        activeByAccount = new ConcurrentHashMap<String, String>();
        connectionByToken = new ConcurrentHashMap<String, String>();
    }

    /** 单例实例。 */
    private static SessionManager instance;

    /**
     * 获取全局会话管理器单例。
     *
     * @return 会话管理器
     */
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * 签发会话并返回 token（不采集姓名）。
     *
     * @param uuid     账户全局唯一标识
     * @param username 登录名
     * @param role     真实角色
     * @return 新 token
     */
    public String create(String uuid, String username, String role) {
        return create(uuid, username, null, role);
    }

    /**
     * 签发会话并返回 token。
     *
     * <p>
     * 姓名一并写进会话记录，客户端登录后立刻就有称呼可显示，不必为了一个姓名字段多跑一次 请求；会话失效时姓名随之作废（它属于会话快照，不是权威档案）。
     *
     * @param uuid        账户全局唯一标识
     * @param username    登录名
     * @param displayName 姓名（可为 null）
     * @param role        真实角色
     * @return 新 token
     */
    public String create(String uuid, String username, String displayName, String role) {
        String token = random.randomHex(16);
        sessions.put(token, new SessionEntry(uuid, username, displayName, role,
                System.currentTimeMillis() + EXPIRY_MILLIS));
        return token;
    }

    /**
     * 签发<b>独占</b>登录会话：同一账号已存在的会话立即作废（后登录顶掉先登录）。
     *
     * <p>
     * 与 {@link #create} 分开是为了不动「一次性复核 token」那条路径 —— 它同属一个账号，但按约定不能把 用户已经登录的会话顶掉。
     *
     * @param uuid        账户全局唯一标识
     * @param username    登录名
     * @param displayName 姓名（可为 null）
     * @param role        真实角色
     * @return 新 token
     */
    public String createExclusive(String uuid, String username, String displayName, String role) {
        return createExclusive(uuid, username, displayName, role, null);
    }

    /**
     * 签发独占登录会话（带来源连接）。
     *
     * <p>
     * 只有<b>来自另一条连接</b>的登录才作废旧会话（“另一个客户端抢登录”）；同一条连接上的再次登录是客户端在 为自己的业务做密码复核，它后续仍要用原来那个 token
     * 发命令，顶掉就等于把客户端自己踢下去。
     *
     * <p>
     * 同连接的复核 token 也不接管「活跃登录」的位置：活跃登录仍然是那条连接上真正的那次登录，否则原会话 会失去跟踪，下一次抢登录就顶不掉它。
     *
     * @param uuid         账户全局唯一标识
     * @param username     登录名
     * @param displayName  姓名（可为 null）
     * @param role         真实角色
     * @param connectionId 来源连接编号；null 视为「另一条连接」
     * @return 新 token
     */
    public String createExclusive(String uuid, String username, String displayName, String role,
            String connectionId) {
        String previous = uuid == null ? null : activeByAccount.get(uuid);
        boolean sameConnection = previous != null && connectionId != null
                && connectionId.equals(connectionByToken.get(previous));
        if (previous != null && !sameConnection) {
            sessions.remove(previous);
            connectionByToken.remove(previous);
            ServerLog.info("账号 " + username + " 从另一连接登录，旧会话已作废");
        }
        String token = create(uuid, username, displayName, role);
        if (uuid != null) {
            if (connectionId != null) {
                connectionByToken.put(token, connectionId);
            }
            if (!sameConnection) {
                activeByAccount.put(uuid, token);
            }
        }
        return token;
    }

    /**
     * 账户当前是否有有效登录会话。
     *
     * @param uuid 账户全局唯一标识
     * @return 有有效会话为 true
     */
    public boolean hasActiveSession(String uuid) {
        if (uuid == null) {
            return false;
        }
        String token = activeByAccount.get(uuid);
        return token != null && sessions.containsKey(token);
    }

    /**
     * 校验 token：有效则刷新 TTL 并返回身份，无效返回 null。
     *
     * @param token 会话令牌
     * @return 会话记录；无效/过期返回 null
     */
    public SessionEntry validate(String token) {
        if (token == null) {
            // 未携带令牌：直接视为未登录；ConcurrentHashMap 不接受 null 键
            return null;
        }
        SessionEntry entry = sessions.get(token);
        if (entry == null) {// 没找到token
            return null;
        }
        if (entry.isExpired(System.currentTimeMillis())) {// 移除存在但过期的token
            sessions.remove(token);
            return null;
        }
        entry.renew(System.currentTimeMillis() + EXPIRY_MILLIS);// 更新有效期
        return entry;
    }

    /**
     * 注销：删除 token 会话。
     *
     * @param token 会话令牌
     */
    public void invalidate(String token) {
        if (token == null) {
            return;
        }
        SessionEntry entry = sessions.remove(token);
        connectionByToken.remove(token);
        if (entry != null && entry.getUuid() != null) {
            // 只在下标确实指向本 token 时移除，别把刚顶上来的新会话一起清掉
            activeByAccount.remove(entry.getUuid(), token);
        }
    }
}