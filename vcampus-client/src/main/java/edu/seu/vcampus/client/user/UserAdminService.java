package edu.seu.vcampus.client.user;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.ProtocolLimit;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.user.dto.BatchResult;
import edu.seu.vcampus.common.user.dto.RegisterRequest;
import edu.seu.vcampus.common.user.dto.UserEnabledRequest;
import edu.seu.vcampus.common.user.dto.UserQuery;
import edu.seu.vcampus.common.user.dto.UserRefRequest;
import edu.seu.vcampus.common.user.dto.UserUpdateRequest;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.User;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户管理「管理轨」客户端 API：查询、编辑、启停、注册、注销、批量（需 {@code USER_MANAGE}）。
 *
 * <p>
 * 与 {@link UserService}（我的轨：登录/登出/改密/会话）分开，对应 ADR-0009 D7 附则的三轨规则：
 * 管理轨要显式指定目标账号，而我的轨一律不带身份参数。两者共用同一份内存会话（由 {@link UserService}
 * 透出 {@link UserService#admin()}），因此 token 只有一个来源。
 *
 * <p>
 * 全部方法<b>同步阻塞</b>，失败抛非受检 {@link edu.seu.vcampus.client.api.ApiException}；界面请用
 * {@code UiTasks.run(...)} 调用。
 */
public class UserAdminService {

    /** 请求工具。 */
    private final UserRequests m_requests;

    /**
     * 构造管理轨 API（由 {@link UserService} 内部创建，界面请用 {@code ClientApis.userAdmin()}）。
     *
     * @param requests 请求工具
     */
    UserAdminService(UserRequests requests) {
        this.m_requests = requests;
    }

    /**
     * 分页查询用户。
     *
     * @param query 查询条件；null 表示全部
     * @return 分页结果
     * @throws edu.seu.vcampus.client.api.ApiException 无权限或本地失败
     */
    public PageResponse<User> listUsers(UserQuery query) {
        return UserRequests.userPage(m_requests.call(Command.USER_LIST, query).getData());
    }

    /**
     * 编辑用户姓名（不改角色）。
     *
     * @param request 编辑请求
     * @throws edu.seu.vcampus.client.api.ApiException 无权限、目标不存在或本地失败
     */
    public void updateUser(UserUpdateRequest request) {
        m_requests.call(Command.USER_UPDATE, request);
    }

    /**
     * 启用/禁用用户。
     *
     * @param userName 目标登录名
     * @param enabled  目标状态
     * @throws edu.seu.vcampus.client.api.ApiException 无权限、目标不存在或本地失败
     */
    public void toggleUserEnabled(String userName, boolean enabled) {
        m_requests.call(Command.USER_TOGGLE_ENABLED, new UserEnabledRequest(userName, enabled));
    }

    /**
     * 新建账户：姓名随注册一并提交，避免多一次往返；服务器会同步建立各模块档案。
     *
     * @param userName    登录名
     * @param displayName 姓名；null 或空表示取登录名
     * @param role        角色
     * @param password    明文密码
     * @throws edu.seu.vcampus.client.api.ApiException 重名、无权限或本地失败
     */
    public void register(String userName, String displayName, Role role, String password) {
        RegisterRequest request = new RegisterRequest();
        request.m_user_name = userName;
        request.m_display_name = displayName;
        request.m_role = role == null ? null : role.getDisplayName();
        request.m_password = password;
        m_requests.call(Command.USER_REGISTER, request);
    }

    /**
     * 注销账户；服务器同时撤销该账号的各模块档案。
     *
     * @param userName 目标登录名
     * @throws edu.seu.vcampus.client.api.ApiException 无权限、目标不存在或本地失败
     */
    public void unregister(String userName) {
        m_requests.call(Command.USER_UNREGISTER, new UserRefRequest(userName));
    }

    /**
     * 批量注册：按协议上限分片发送，逐条记账（重复登录名等失败原因随结果返回）。
     *
     * @param requests 注册请求列表
     * @param listener 进度回调；可为 null
     * @return 聚合结果
     * @throws edu.seu.vcampus.client.api.ApiException 无权限或本地失败
     */
    public BatchResult batchRegister(List<RegisterRequest> requests,
            BatchProgressListener listener) {
        return sendInChunks(Command.USER_BATCH_REGISTER,
                BatchSplitter.split(requests, ProtocolLimit.MAX_BATCH_SIZE), listener);
    }

    /**
     * 批量注销：按协议上限分片发送。
     *
     * @param userNames 登录名列表
     * @param listener  进度回调；可为 null
     * @return 聚合结果
     * @throws edu.seu.vcampus.client.api.ApiException 无权限或本地失败
     */
    public BatchResult batchUnregister(List<String> userNames, BatchProgressListener listener) {
        return sendInChunks(Command.USER_BATCH_UNREGISTER,
                BatchSplitter.split(userNames, ProtocolLimit.MAX_BATCH_SIZE), listener);
    }

    /**
     * 分片发送批量命令并聚合各片结果。
     *
     * @param command  批量命令码
     * @param chunks   已切好的片
     * @param listener 进度回调；可为 null
     * @param <T>      条目类型
     * @return 聚合后的批量结果
     */
    private <T> BatchResult sendInChunks(int command, List<List<T>> chunks,
            BatchProgressListener listener) {
        int total = 0;
        for (List<T> chunk : chunks) {
            total += chunk.size();
        }
        int success = 0;
        int completed = 0;
        List<BatchResult.Failure> failures = new ArrayList<BatchResult.Failure>();
        for (List<T> chunk : chunks) {
            BatchResult partial = UserRequests.batchResult(
                    m_requests.call(command, chunk).getData());
            success += partial.getSuccessCount();
            failures.addAll(partial.getFailures());
            completed += chunk.size();
            if (listener != null) {
                listener.onProgress(completed, total);
            }
        }
        return new BatchResult(success, failures);
    }
}
