package com.boompala.engine.ai

import com.boompala.engine.bazi.BaziGender
import com.boompala.engine.model.YaoPosition
import com.boompala.engine.rules.YongShenCategory
import com.google.gson.annotations.SerializedName

/**
 * 测事分类定义。
 * 每种分类映射对应的六爻主要参考用神。
 */
enum class AiDivinationTopic(
    val id: String,
    val displayName: String,
    val description: String,
) {
    GENERAL("general", "综合求测", "以世爻为主体，观世应生克与整体卦局态势"),
    CAREER("career", "事业官运", "以官鬼为主要用神，参看父母（职位/文书）与世爻"),
    WEALTH("wealth", "求财财运", "以妻财为主要用神，参看子孙（财源）与兄弟（克剥）"),
    RELATIONSHIP("relationship", "婚姻感情", "男测以妻财为用，女测以官鬼为用，兼看世应相生相克"),
    HEALTH("health", "健康平安", "以子孙（福神/医药）为解忧用神，兼看官鬼（病症）与世爻（体质）"),
    STUDY("study", "学业考试", "以父母（文书/成绩）为主要用神，兼看官鬼（功名录取）与世爻");

    fun toPrimaryYongShenCategory(gender: BaziGender? = null): YongShenCategory = when (this) {
        GENERAL -> YongShenCategory.SHI_YAO
        CAREER -> YongShenCategory.OFFICER
        WEALTH -> YongShenCategory.WEALTH
        RELATIONSHIP -> when (gender) {
            BaziGender.FEMALE -> YongShenCategory.OFFICER
            else -> YongShenCategory.WEALTH
        }
        HEALTH -> YongShenCategory.OFFSPRING
        STUDY -> YongShenCategory.PARENTS
    }

    companion object {
        fun fromId(id: String): AiDivinationTopic =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: GENERAL
    }
}

data class AiChatMessage(
    val role: String,
    val content: String,
)

data class AiChatRequest(
    val model: String,
    val messages: List<AiChatMessage>,
    val stream: Boolean = true,
    val temperature: Double? = 0.7,
    @SerializedName("max_tokens")
    val maxTokens: Int? = 1000,
)

sealed interface AiStreamEvent {
    data class TextDelta(val text: String) : AiStreamEvent
    data class Completed(val fullText: String, val finishReason: String? = null) : AiStreamEvent
    data class Error(val error: AiError) : AiStreamEvent
}

sealed class AiError(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause) {
    data class InvalidApiKey(
        override val message: String = "API Key 无效或未授权 (HTTP 401)",
    ) : AiError(message)

    data class RateLimited(
        override val message: String = "请求过于频繁或额度已耗尽 (HTTP 429)",
    ) : AiError(message)

    data class BadRequest(
        val rawMessage: String? = null,
        override val message: String = rawMessage?.let { "请求参数或模型配置有误: $it" } ?: "请求参数或模型配置有误 (HTTP 400)",
    ) : AiError(message)

    data class ServerError(
        val statusCode: Int,
        val rawMessage: String? = null,
        override val message: String = rawMessage?.let { "AI 服务商暂时异常 (HTTP $statusCode): $it" } ?: "AI 服务商暂时异常 (HTTP $statusCode)",
    ) : AiError(message)

    data class NetworkUnavailable(
        override val message: String = "网络连接不可用，请检查网络",
        override val cause: Throwable? = null,
    ) : AiError(message, cause)

    data class NetworkTimeout(
        override val message: String = "网络请求超时，请稍后重试",
        override val cause: Throwable? = null,
    ) : AiError(message, cause)

    data class InvalidResponse(
        override val message: String = "服务商返回数据格式异常",
        override val cause: Throwable? = null,
    ) : AiError(message, cause)

    data class Unknown(
        override val message: String = "未知错误",
        override val cause: Throwable? = null,
    ) : AiError(message, cause)

    override fun toString(): String = "${javaClass.simpleName}: $message"
}

data class AiLineDetail(
    val position: YaoPosition,
    val polarityName: String,
    val isMoving: Boolean,
    val stemBranchName: String,
    val elementName: String,
    val relationName: String,
    val spiritName: String,
    val isShi: Boolean,
    val isYing: Boolean,
    val isVoid: Boolean,
    val statuses: List<String>,
    val fuShenSummary: String?,
    val changedStemBranchName: String?,
    val changedRelationName: String?,
    val changedElementName: String?,
)

data class AiDivinationContext(
    val topic: AiDivinationTopic,
    val userQuestion: String?,
    val userGender: BaziGender?,
    val castDateSolar: String,
    val castDateLunar: String,
    val ganzhiFourPillars: String,
    val monthBranch: String,
    val dayBranch: String,
    val voidBranches: String,
    val originalHexagramName: String,
    val originalPalace: String,
    val originalStatuses: List<String>,
    val changedHexagramName: String?,
    val changedPalace: String?,
    val changedStatuses: List<String>,
    val shiYingDescription: String,
    val yongShenDescription: String,
    val movingLinesSummary: List<String>,
    val lines: List<AiLineDetail>,
)

data class AiPrompt(
    val systemPrompt: String,
    val userPrompt: String,
    val context: AiDivinationContext,
    val metadata: Map<String, String> = emptyMap(),
)
