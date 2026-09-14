package com.zentide.mapper;
import org.apache.ibatis.annotations.Mapper; import org.apache.ibatis.annotations.Param; import java.math.BigDecimal; import java.util.List; import java.util.Map;
/** 治理控制面数据访问接口，规则、审核结果和管理员反馈统一落主库。 */
@Mapper
public interface ZentideGovernanceMapper {
    /** 查询现场当前启用的治理规则。 */
    List<Map<String,Object>> listRules(@Param("sceneId") Long sceneId);
    List<Map<String,Object>> listAllRules(@Param("sceneId") Long sceneId);
    /** 保存一条现场治理规则。 */
    int insertRule(@Param("sceneId") Long sceneId, @Param("violationType") String type,
                   @Param("description") String description, @Param("keywordsJson") String keywords, @Param("severity") String severity);
    int updateRule(@Param("ruleId") Long ruleId, @Param("violationType") String type,
                   @Param("description") String description, @Param("keywordsJson") String keywords, @Param("severity") String severity,
                   @Param("enabled") Boolean enabled);
    int deleteRule(@Param("ruleId") Long ruleId);
    /** 保存 Agent 审核结果，重复结果 ID 时更新证据快照。 */
    int insertResult(@Param("resultId") String id, @Param("contentId") Long contentId,
                     @Param("sceneId") Long sceneId, @Param("riskLevel") String riskLevel,
                     @Param("suggestedAction") String suggestedAction, @Param("confidence") BigDecimal confidence,
                     @Param("payloadJson") String payload);
    /** 保存管理员最终处理反馈。 */
    int insertFeedback(Map<String,Object> data);
    List<Map<String,Object>> listPendingResults(@Param("limit") int limit);
}
