/*
 * 逐次核验凭据、Redis 会话与账号状态，并只在有效业务操作后续期。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.filter;

// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入账号当前登录身份（AccountPrincipal），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.AccountPrincipal;
// 引入账号Security提供器（AccountSecurityProvider），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.AccountSecurityProvider;
// 引入登录会话数据（LoginSessionData），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.LoginSessionData;
// 引入Redis缓存会话Store（RedisSessionStore），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.RedisSessionStore;
// 引入Security账号数据（SecurityAccountData），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.SecurityAccountData;
// 引入Security错误Handler（SecurityErrorHandler），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.SecurityErrorHandler;
// 引入Jwt身份凭据Utils（JwtTokenUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.JwtTokenUtils;

// 引入筛选Chain（FilterChain），下面可以直接使用这个类型或静态成员。
import jakarta.servlet.FilterChain;
// 引入ServletException，下面可以直接使用这个类型或静态成员。
import jakarta.servlet.ServletException;
// 引入Java HTTP 请求对象（HttpServletRequest），下面可以直接使用这个类型或静态成员。
import jakarta.servlet.http.HttpServletRequest;
// 引入Java HTTP 响应对象（HttpServletResponse），下面可以直接使用这个类型或静态成员。
import jakarta.servlet.http.HttpServletResponse;

// 引入日志记录器（Logger），下面可以直接使用这个类型或静态成员。
import org.slf4j.Logger;
// 引入日志记录器创建器（LoggerFactory），下面可以直接使用这个类型或静态成员。
import org.slf4j.LoggerFactory;
// 引入数据访问权限Exception（DataAccessException），下面可以直接使用这个类型或静态成员。
import org.springframework.dao.DataAccessException;
// 引入用户名密码身份认证身份凭据（UsernamePasswordAuthenticationToken），下面可以直接使用这个类型或静态成员。
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// 引入SimpleGranted权限（SimpleGrantedAuthority），下面可以直接使用这个类型或静态成员。
import org.springframework.security.core.authority.SimpleGrantedAuthority;
// 引入Security上下文Holder（SecurityContextHolder），下面可以直接使用这个类型或静态成员。
import org.springframework.security.core.context.SecurityContextHolder;
// 引入JwtException，下面可以直接使用这个类型或静态成员。
import org.springframework.security.oauth2.jwt.JwtException;
// 引入OncePer请求筛选（OncePerRequestFilter），下面可以直接使用这个类型或静态成员。
import org.springframework.web.filter.OncePerRequestFilter;
// 引入内容Caching响应Wrapper（ContentCachingResponseWrapper），下面可以直接使用这个类型或静态成员。
import org.springframework.web.util.ContentCachingResponseWrapper;

// 引入IOException，下面可以直接使用这个类型或静态成员。
import java.io.IOException;

/* 请求认证与七天滑动续签；仅成功业务请求续期，设备列表轮询和退出不会保活。 */
// 定义类 JwtAuthenticationFilter；逐次核验凭据、Redis 会话与账号状态，并只在有效业务操作后续期。
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // 准备日志记录器（LOGGER），类型为 Logger；初始内容是读取 日志记录器（Logger） 的值。
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    // 凭据处理器（tokens），使用 JwtTokenUtils 类型保存。
    private final JwtTokenUtils tokens;
    // 会话列表（sessions），使用 RedisSessionStore 类型保存。
    private final RedisSessionStore sessions;
    // accounts，使用 AccountSecurityProvider 类型保存。
    private final AccountSecurityProvider accounts;
    // 错误处理器（errors），使用 SecurityErrorHandler 类型保存。
    private final SecurityErrorHandler errors;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    public JwtAuthenticationFilter(
            // 凭据处理器（tokens），使用 JwtTokenUtils 类型保存。
            JwtTokenUtils tokens,
            // 会话列表（sessions），使用 RedisSessionStore 类型保存。
            RedisSessionStore sessions,
            // accounts，使用 AccountSecurityProvider 类型保存。
            AccountSecurityProvider accounts,
            // 错误处理器（errors），使用 SecurityErrorHandler 类型保存。
            SecurityErrorHandler errors) {
        // 把凭据处理器（tokens）保存到 this.tokens。
        this.tokens = tokens;
        // 把会话列表（sessions）保存到 this.sessions。
        this.sessions = sessions;
        // 把accounts保存到 this.accounts。
        this.accounts = accounts;
        // 把错误处理器（errors）保存到 this.errors。
        this.errors = errors;
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // shouldNot筛选（shouldNotFilter）方法：调用 shouldNotFilter 完成shouldNot筛选处理；参数负责传入本次处理所需的数据。
    @Override
    // 请求（request），使用 HttpServletRequest 类型保存。
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 准备路径（path），类型为 String；初始内容是读取 Servlet路径（ServletPath） 的值。
        String path = request.getServletPath();
        // 把优先使用前面的有效值，没有时采用后面的备用值：!path.startsWith("/asterway/") || "OPTIONS".equals(request.getMethod()) || path.startsWith("/asterway/public/"…交还给调用方，本方法到此结束。
        return !path.startsWith("/asterway/")
                // 继续补充同一个判断或备用结果：条件 || "OPTIONS".equals(request.getMethod()) 是否成立。
                || "OPTIONS".equals(request.getMethod())
                // 继续补充同一个判断或备用结果：条件 || path.startsWith("/asterway/public/") 是否成立。
                || path.startsWith("/asterway/public/")
                // 继续补充同一个判断或备用结果：条件 || path.equals("/asterway/time-zones") 是否成立。
                || path.equals("/asterway/time-zones")
                // 继续补充同一个判断或备用结果：条件 || path.equals("/asterway/captchas") 是否成立。
                || path.equals("/asterway/captchas")
                // 继续补充同一个判断或备用结果：条件 || path.equals("/asterway/captcha-verifications") 是否成立。
                || path.equals("/asterway/captcha-verifications")
                // 继续补充同一个判断或备用结果：条件 || path.equals("/asterway/registrations") 是否成立。
                || path.equals("/asterway/registrations")
                // 继续补充同一个判断或备用结果：条件 || path.equals("/asterway/sessions") && "POST".equals(request.getMethod()); 是否成立。
                || path.equals("/asterway/sessions") && "POST".equals(request.getMethod());
    }

    // 说明此方法实现或改写了父类、接口中已经声明的方法。
    // do筛选内部（doFilterInternal）方法：调用 doFilterInternal 完成do筛选内部处理；参数负责传入本次处理所需的数据。
    @Override
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    protected void doFilterInternal(
            // 请求（request），使用 HttpServletRequest 类型保存。
            // 响应（response），使用 HttpServletResponse 类型保存。
            // chain，使用 FilterChain 类型保存。
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            // 声明本方法可能向上抛出的异常，调用方需要按规则处理。
            throws ServletException, IOException {
        // 当前会话（session），使用 LoginSessionData 类型保存。
        LoginSessionData session;
        // 账号（account），使用 SecurityAccountData 类型保存。
        SecurityAccountData account;
        // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
        try {
            // 准备请求头（header），类型为 String；初始内容是读取 请求头（Header） 的值。
            String header = request.getHeader("Authorization");
            // 检查header == null || !header.startsWith("Bearer ") || header.length() <= 7，即对应内容是否不存在；成立时执行下面的分支。
            if (header == null || !header.startsWith("Bearer ") || header.length() <= 7) {
                // 中止正常处理并抛出异常：调用 BusinessException.unauthorized 完成unauthorized处理；由上层按失败处理。
                throw BusinessException.unauthorized();
            }
            // 准备jwt，类型为 null；初始内容是调用 tokens.parse 完成解析处理。
            var jwt = tokens.parse(header.substring(7));
            // 把按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值保存到 session。
            session = sessions.get(jwt.getClaimAsString("sid"));
            // 检查记录或凭据是否已经到期：session == null || session.expiresAt() <= System.currentTimeMillis() || !session.userId().equals(jwt.getSubject())；成立时执行下面的分支。
            if (session == null
                    // 继续补充同一个判断或备用结果：记录或凭据是否已经到期：|| session.expiresAt() <= System.currentTimeMillis()。
                    || session.expiresAt() <= System.currentTimeMillis()
                    // 继续补充同一个判断或备用结果：条件 || !session.userId().equals(jwt.getSubject())) { 是否成立。
                    || !session.userId().equals(jwt.getSubject())) {
                // 中止正常处理并抛出异常：调用 BusinessException.unauthorized 完成unauthorized处理；由上层按失败处理。
                throw BusinessException.unauthorized();
            }
            // 把加载 Security账号（SecurityAccount）保存到 account。
            account = accounts.loadSecurityAccount(Long.parseLong(session.userId()));
            // 检查当前记录版本是否符合请求要求：account == null || !account.enabled() || account.authVersion() != session.authVersion()；成立时执行下面的分支。
            if (account == null
                    // 继续补充同一个判断或备用结果：条件 || !account.enabled() 是否成立。
                    || !account.enabled()
                    // 继续补充同一个判断或备用结果：当前记录版本是否符合请求要求：|| account.authVersion() != session.authVersion()) {。
                    || account.authVersion() != session.authVersion()) {
                // 中止正常处理并抛出异常：调用 BusinessException.unauthorized 完成unauthorized处理；由上层按失败处理。
                throw BusinessException.unauthorized();
            }
            // 检查条件 account.mustChangePassword() && !passwordChangeAllowed(request) 是否成立；成立时执行下面的分支。
            if (account.mustChangePassword() && !passwordChangeAllowed(request)) {
                // 把内容写入当前输出或响应。
                errors.write(response, 403, "PASSWORD_CHANGE_REQUIRED", "请先修改初始密码");
                // 把返回（return）交还给调用方，本方法到此结束。
                return;
            }
        // 错误（error），使用 BusinessException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (BusinessException error) {
            // 把内容写入当前输出或响应。
            errors.write(response, error.getStatus(), error.getCode(), error.getMessage());
            // 把返回（return）交还给调用方，本方法到此结束。
            return;
        // 错误（error），使用 JwtException | IllegalArgumentException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (JwtException | IllegalArgumentException error) {
            // 把内容写入当前输出或响应。
            errors.write(response, 401, "UNAUTHORIZED", "登录已失效，请重新登录");
            // 把返回（return）交还给调用方，本方法到此结束。
            return;
        // 错误（error），使用 DataAccessException | IllegalStateException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (DataAccessException | IllegalStateException error) {
            // 把内容写入当前输出或响应。
            errors.write(response, 503, "SERVICE_UNAVAILABLE", "认证服务暂不可用，请稍后重试");
            // 把返回（return）交还给调用方，本方法到此结束。
            return;
        }
        // 准备当前登录身份（principal），类型为 AccountPrincipal；初始内容是新建 账号当前登录身份（AccountPrincipal） 实例。
        AccountPrincipal principal =
                // 继续传入或组合新建 账号当前登录身份（AccountPrincipal） 实例，完成上面尚未结束的调用或声明。
                new AccountPrincipal(
                        // 调用 account.userId 完成用户编号处理。
                        account.userId(),
                        // 调用 account.username 完成用户名处理。
                        account.username(),
                        // 调用 account.mustChangePassword 完成首次登录是否必须改密码处理。
                        account.mustChangePassword(),
                        // 调用 account.authVersion 完成使旧登录失效的认证版本号处理。
                        account.authVersion(),
                        // 调用 account.roles 完成角色列表处理。
                        account.roles(),
                        // 调用 account.permissions 完成权限列表处理。
                        account.permissions(),
                        // 调用 session.id 完成编号处理。
                        session.id());
        // 准备身份认证（authentication），类型为 null；初始内容是新建 用户名密码身份认证身份凭据（UsernamePasswordAuthenticationToken） 实例。
        var authentication =
                // 继续传入或组合新建 用户名密码身份认证身份凭据（UsernamePasswordAuthenticationToken） 实例，完成上面尚未结束的调用或声明。
                new UsernamePasswordAuthenticationToken(
                        // 继续传入或组合当前登录身份（principal），完成上面尚未结束的调用或声明。
                        principal,
                        // 继续传入或组合空值，表示当前还没有对应内容，完成上面尚未结束的调用或声明。
                        null,
                        // 把前面筛选、转换后的结果汇成列表。
                        account.permissions().stream().map(SimpleGrantedAuthority::new).toList());
        // 准备上下文（context），类型为 null；初始内容是创建仅供本次请求使用的空安全上下文。
        var context = SecurityContextHolder.createEmptyContext();
        // 把本次验证通过的身份放入安全上下文。
        context.setAuthentication(authentication);
        // 设置 上下文（Context） 的值。
        SecurityContextHolder.setContext(context);
        // 检查条件 request.getMethod().equals("POST") && (request.getServletPath().equals("/asterway/agent/chat/stream") || isAgentConversationStream(request.getSer… 是否成立；成立时执行下面的分支。
        if (request.getMethod().equals("POST")
                // 继续补充同一个判断或备用结果：条件 && (request.getServletPath().equals("/asterway/agent/chat/stream") 是否成立。
                && (request.getServletPath().equals("/asterway/agent/chat/stream")
                        // 继续补充同一个判断或备用结果：条件 || isAgentConversationStream(request.getServletPath()))) { 是否成立。
                        || isAgentConversationStream(request.getServletPath()))) {
            // SSE 必须直接写出；JSON 响应缓存会吞掉分片并妨碍取消。
            // 不按流式心跳续签，Python 仅在请求开始和完成时重新验证登录。
            // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
            try {
                // 把请求交给过滤链中的下一步处理。
                chain.doFilter(request, response);
            // 无论成功还是失败，都执行下面的资源与状态清理。
            } finally {
                // 清除当前线程的登录身份，避免被后续请求复用。
                SecurityContextHolder.clearContext();
            }
            // 把返回（return）交还给调用方，本方法到此结束。
            return;
        }
        // 缓存本模块的 JSON 响应，业务完成后、写出前才能安全增加续签响应头。
        // 准备缓存结果（cached），类型为 ContentCachingResponseWrapper；初始内容是新建 内容Caching响应Wrapper（ContentCachingResponseWrapper） 实例。
        ContentCachingResponseWrapper cached = new ContentCachingResponseWrapper(response);
        // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
        try {
            // 把请求交给过滤链中的下一步处理。
            chain.doFilter(request, cached);
            // 检查条件 cached.getStatus() < 400 && renewable(request) 是否成立；成立时执行下面的分支。
            if (cached.getStatus() < 400 && renewable(request)) {
                // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
                try {
                    // 准备renewed，类型为 LoginSessionData；初始内容是调用 sessions.touch 完成touch处理。
                    LoginSessionData renewed = sessions.touch(session);
                    // 检查条件 renewed != null 是否成立；成立时执行下面的分支。
                    if (renewed != null) {
                        // 设置 请求头（Header） 的值。
                        cached.setHeader("X-Session-Token", tokens.create(renewed));
                        // 设置 请求头（Header） 的值。
                        cached.setHeader(
                                // 继续传入或组合调用 "X-Session-Expires-At", Long.toString 完成to文字处理，完成上面尚未结束的调用或声明。
                                "X-Session-Expires-At", Long.toString(renewed.expiresAt()));
                    }
                // 错误（error），使用 RuntimeException 类型保存。
                // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
                } catch (RuntimeException error) {
                    // 此时业务可能已经提交，保持真实业务结果，不把保存成功伪装成未保存。
                    // 记录warn级别的运行信息，供定位处理过程和失败原因。
                    LOGGER.warn("业务响应完成后续签不可用，类型：{}", error.getClass().getSimpleName());
                }
            }
        // 无论成功还是失败，都执行下面的资源与状态清理。
        } finally {
            // 调用 cached.copyBodyToResponse 完成copy请求正文To响应处理。
            cached.copyBodyToResponse();
            // 清除当前线程的登录身份，避免被后续请求复用。
            SecurityContextHolder.clearContext();
        }
    }

    /* 会话内 SSE 与单轮 SSE 一样必须绕过 JSON 响应缓存。 */
    // 是否成长助手对话分段回复流（isAgentConversationStream）方法：判断是否为 成长助手对话分段回复流（AgentConversationStream）；参数负责传入本次处理所需的数据。
    // 路径（path），使用 String 类型保存；可为 null，表示尚未提供。
    private boolean isAgentConversationStream(String path) {
        // 把判断文字是否具有指定前缀交还给调用方，本方法到此结束。
        return path.startsWith("/asterway/agent/conversations/") && path.endsWith("/messages/stream");
    }

    // 密码修改Allowed（passwordChangeAllowed）方法：调用 passwordChangeAllowed 完成密码修改Allowed处理；参数负责传入本次处理所需的数据。
    // 请求（request），使用 HttpServletRequest 类型保存。
    // 准备路径（path），类型为 String；初始内容是读取 Servlet路径（ServletPath） 的值。
    private boolean passwordChangeAllowed(HttpServletRequest request) {        String path = request.getServletPath();
        // 把优先使用前面的有效值，没有时采用后面的备用值：path.equals("/asterway/me") && request.getMethod().equals("GET") || path.equals("/asterway/me/password") && re…交还给调用方，本方法到此结束。
        return path.equals("/asterway/me") && request.getMethod().equals("GET")
                // 继续补充同一个判断或备用结果：条件 || path.equals("/asterway/me/password") && request.getMethod().equals("PUT") 是否成立。
                || path.equals("/asterway/me/password") && request.getMethod().equals("PUT")
                // 继续补充同一个判断或备用结果：条件 || path.startsWith("/asterway/me/sessions") && request.getMethod().equals("DELETE"); 是否成立。
                || path.startsWith("/asterway/me/sessions") && request.getMethod().equals("DELETE");
    }

    // renewable方法：调用 renewable 完成renewable处理；参数负责传入本次处理所需的数据。
    // 请求（request），使用 HttpServletRequest 类型保存。
    private boolean renewable(HttpServletRequest request) {
        // 准备路径（path），类型为 String；初始内容是读取 Servlet路径（ServletPath） 的值。
        String path = request.getServletPath();
        // 把判断文字是否具有指定前缀交还给调用方，本方法到此结束。
        return !path.startsWith("/asterway/me/sessions")
                // 继续补充同一个判断或备用结果：条件 && !path.equals("/asterway/sessions") 是否成立。
                && !path.equals("/asterway/sessions")
                // 继续补充同一个判断或备用结果：条件 && !(path.startsWith("/asterway/sessions/") && request.getMethod().equals("DELETE")) 是否成立。
                && !(path.startsWith("/asterway/sessions/") && request.getMethod().equals("DELETE"))
                // 继续补充同一个判断或备用结果：条件 && !(path.equals("/asterway/executions/current") 是否成立。
                && !(path.equals("/asterway/executions/current")
                        // 继续补充同一个判断或备用结果：条件 && request.getMethod().equals("GET")) 是否成立。
                        && request.getMethod().equals("GET"))
                // 继续补充同一个判断或备用结果：条件 && !(request.getMethod().equals("GET") 是否成立。
                && !(request.getMethod().equals("GET")
                        // 继续补充同一个判断或备用结果：条件 && (path.equals("/asterway/dashboard/overview") 是否成立。
                        && (path.equals("/asterway/dashboard/overview")
                                // 继续补充同一个判断或备用结果：条件 || path.equals("/asterway/notifications/unread-count"))) 是否成立。
                                || path.equals("/asterway/notifications/unread-count")))
                // 继续补充同一个判断或备用结果：条件 && !(path.startsWith("/asterway/executions/") 是否成立。
                && !(path.startsWith("/asterway/executions/")
                        // 继续补充同一个判断或备用结果：条件 && path.endsWith("/heartbeat") 是否成立。
                        && path.endsWith("/heartbeat")
                        // 继续补充同一个判断或备用结果：条件 && request.getMethod().equals("POST")) 是否成立。
                        && request.getMethod().equals("POST"))
                // 继续补充同一个判断或备用结果：条件 && !path.equals("/asterway/me/password"); 是否成立。
                && !path.equals("/asterway/me/password");
    }
}
